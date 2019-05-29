package com.holoview.hololauncher.activitys;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.gson.Gson;
import com.holoview.hololauncher.BaseActivity;
import com.holoview.hololauncher.HoloLauncherApp;
import com.holoview.hololauncher.R;
import com.holoview.hololauncher.bean.Constants;
import com.hv.imlib.DB.sp.SystemConfigSp;
import com.hv.imlib.ImLib;
import com.hv.imlib.imservice.manager.IMNaviManager;
import com.hv.imlib.protocol.ProtoConstant;
import com.hv.imlib.protocol.http.NaviRes;
import com.realview.commonlibrary.server.http.ErrorCode;
import com.realview.commonlibrary.server.manager.AssetsManager;
import com.realview.commonlibrary.server.manager.CommLib;
import com.realview.commonlibrary.server.manager.QRCodeManager;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.szx.zbarscanner.zbar.Result;
import cn.szx.zbarscanner.zbar.ViewFinderView;
import cn.szx.zbarscanner.zbar.WifiViewFinderView;
import cn.szx.zbarscanner.zbar.ZBarScannerView;

import static com.hv.imlib.ImLib.ConnectionStatusListener.ConnectionStatus.CONNECTED;

/**
 * Created by Mr.kk on 2019/3/31.
 * This Project is android-glass-launcher
 */
public class ScanLoginActivity extends BaseActivity implements ZBarScannerView.ResultHandler {
    @BindView(R.id.fl_scan_wifi_contont)
    FrameLayout flScanWifiContont;
    private static final int REQUEST_CAMERA_PERMISSION = 0;
    private SharedPreferences sp;
    private ZBarScannerView autoScannerView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_login);
        ButterKnife.bind(this);
        sp = getSharedPreferences("config", MODE_PRIVATE);
        autoScannerView = new ZBarScannerView(this, new ViewFinderView(this), this);
        flScanWifiContont.addView(autoScannerView);
    }

    @Override
    public void handleResult(Result rawResult) {
        String qrcode = rawResult.getContents();
        if (qrcode.isEmpty()) {
            Toast.makeText(getBaseContext(), "扫码失败，重新扫码", Toast.LENGTH_SHORT).show();
            reScan();
            return;
        }
        final String[] arryText = qrcode.split("\\;");
        if (arryText.length >= 2) {
            String appid = arryText[1];
            ProtoConstant.APP_ID = appid.isEmpty() ? "000001" : appid;
            IMNaviManager.instance().navi(new IMNaviManager.OnNaviListener() {
                @Override
                public void onSuccess() {
                    doLogin(arryText[0]);
                }

                @Override
                public void onFailure() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getBaseContext(), "二维码错误，检查二维码", Toast.LENGTH_SHORT).show();
                            reScan();
                        }
                    });

                }
            });
        } else {
            doLogin(qrcode);
        }
    }


    /**
     * 重新开始扫描，因为扫描成功以后是不会再扫描
     */
    private void reScan() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                autoScannerView.getOneMoreFrame();//再获取一帧图像数据进行识别
            }
        }, 1);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            autoScannerView.startCamera();//打开系统相机，并进行基本的初始化
            //autoScannerView.();
        } else {//没有相机权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    private Handler handler = new Handler();

    @Override
    protected void onPause() {
        super.onPause();

    }

    private void doLogin(String qrid) {
        String result = SystemConfigSp.instance().getStrConfig(SystemConfigSp.SysCfgDimension.NAVIINFO);
        NaviRes naviRes = new Gson().fromJson(result, NaviRes.class);
        CommLib.instance().setNaviRes(naviRes);
        QRCodeManager.init(this);
        QRCodeManager.instance().getQROper(qrid, new QRCodeManager.ResultCallback<String>() {
            @Override
            public void onSuccess(final String s) {
                final String[] arryText = s.split("\\;");
                final String token = arryText[0]; //"5/4W3IOJLBkwVxGeZQyIx43DFx6nMFVOcjMQA3fUGNraaH0duT96EL0Fptb6YkP2sokbShdXitb+qXdFDsuNhJ0Yh0HM78YtbSs5tWG+ZGAU81BFPxPxUAWJq+gkCKp0RD8uuA75Ti0PPmmK6NMQysTdfzYuOMUEl2oZydOOCcMlRuo2X/x5NtEF0d68XYpI";//arryText[0];
                final String roomId = arryText[1];

                HoloLauncherApp.token = token;
                HoloLauncherApp.roomId = Long.parseLong(roomId);
                HoloLauncherApp.converstaiontype = Integer.parseInt(arryText[2]);

                HoloLauncherApp.call_list.clear();
                for (int i = 3; i < arryText.length; i++) {
                    HoloLauncherApp.call_list.add(Long.parseLong(arryText[i]));
                }
                setResult(Activity.RESULT_OK, getIntent());
                finish();
            }

            @Override
            public void onError(String errString) {
                final String str = errString;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getBaseContext(), str, Toast.LENGTH_SHORT).show();
                        reScan();
                    }
                });

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        autoScannerView.stopCamera();//释放相机资源等各种资源
    }
}
