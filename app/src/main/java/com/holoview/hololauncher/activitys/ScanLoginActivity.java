package com.holoview.hololauncher.activitys;

import android.Manifest;
import android.animation.Animator;
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
import android.view.View;
import android.view.ViewGroup;
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

import org.evilbinary.tv.widget.BorderEffect;
import org.evilbinary.tv.widget.BorderView;
import org.evilbinary.tv.widget.RoundedFrameLayout;

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
    private ZBarScannerView autoScannerView;

    private RoundedFrameLayout back_layout;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_login);
        ButterKnife.bind(this);

        back_layout = (RoundedFrameLayout) findViewById(R.id.view_back);
        back_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        focusView();
        autoScannerView = new ZBarScannerView(this, new WifiViewFinderView(this), this);
        flScanWifiContont.addView(autoScannerView);
    }

    private void focusView() {
        FrameLayout roundedFrameLayout = new FrameLayout(this);
        roundedFrameLayout.setClipChildren(false);

        final BorderView borderView = new BorderView(roundedFrameLayout);
        borderView.setBackgroundResource(R.drawable.border_color);

        ViewGroup list = (ViewGroup) findViewById(R.id.list);
        borderView.attachTo(list);


        borderView.getEffect().addOnFocusChanged(new BorderEffect.FocusListener() {
            @Override
            public void onFocusChanged(View oldFocus, final View newFocus) {
                borderView.getView().setTag(newFocus);

            }
        });
        borderView.getEffect().addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                View t = borderView.getView().findViewWithTag("top");
                if (t != null) {
                    ((ViewGroup) t.getParent()).removeView(t);
                    View of = (View) borderView.getView().getTag(borderView.getView().getId());
                    ((ViewGroup) of).addView(t);

                }

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                View nf = (View) borderView.getView().getTag();
                if (nf != null) {
                    View top = nf.findViewWithTag("top");
                    if (top != null) {
                        ((ViewGroup) top.getParent()).removeView(top);
                        ((ViewGroup) borderView.getView()).addView(top);
                        borderView.getView().setTag(borderView.getView().getId(), nf);

                    }
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });


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
                final String token = arryText[0];
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
