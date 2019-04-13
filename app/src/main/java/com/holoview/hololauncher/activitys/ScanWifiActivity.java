package com.holoview.hololauncher.activitys;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.holoview.hololauncher.BaseActivity;
import com.holoview.hololauncher.R;
import com.holoview.hololauncher.bean.Constants;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.szx.zbarscanner.zbar.Result;
import cn.szx.zbarscanner.zbar.ViewFinderView;
import cn.szx.zbarscanner.zbar.WifiViewFinderView;
import cn.szx.zbarscanner.zbar.ZBarScannerView;

/**
 * Created by Mr.kk on 2019/3/31.
 * This Project is android-glass-launcher
 */
public class ScanWifiActivity extends BaseActivity implements ZBarScannerView.ResultHandler {
    @BindView(R.id.fl_scan_wifi_contont)
    FrameLayout flScanWifiContont;
    private static final int REQUEST_CAMERA_PERMISSION = 0;

    private ZBarScannerView autoScannerView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_scan);
        ButterKnife.bind(this);
        autoScannerView = new ZBarScannerView(this, new WifiViewFinderView(this), this);
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
        if (qrcode.startsWith("wf")) {
            Intent intent = new Intent();
            intent.putExtra(Constants.AP_CONFIG, qrcode);
            setResult(Activity.RESULT_OK, intent);
            finish();
            return;
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
        handler.removeCallbacksAndMessages(null);
        autoScannerView.stopCamera();//释放相机资源等各种资源
    }

}
