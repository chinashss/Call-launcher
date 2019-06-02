package com.holoview.hololauncher;

import android.animation.Animator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import com.holoview.hololauncher.activitys.ScanLoginActivity;
import com.holoview.hololauncher.bean.Constants;
import com.holoview.hololauncher.bean.ImEvent;
import com.hv.imlib.DB.sp.SystemConfigSp;
import com.hv.imlib.HoloMessage;
import com.hv.imlib.ImLib;
import com.hv.imlib.protocol.http.NaviRes;
import com.tencent.mmkv.MMKV;
import com.trios.voicecmd.AudioOrderMessage;
import com.trios.voicecmd.VoiceCmdEngine;

import org.evilbinary.tv.widget.BorderEffect;
import org.evilbinary.tv.widget.BorderView;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Mr.kk on 2019/3/20.
 * This Project is android-glass-launcher
 */
public class LauncherActivity extends BaseActivity {
    @BindView(R.id.tv_last_call_time)
    TextView tvLastCallTime;

    @BindView(R.id.tv_last_call_time_hour)
    TextView tvLastCallTimeHour;

    @BindView(R.id.tv_last_call_info)
    TextView tvLastCallInfo;
    NetworkConnectChangedReceiver networkConnectChangedReceiver;
    @BindView(R.id.iv_wifi_status)
    ImageView ivWifiStatus;

    @BindView(R.id.iv_hintImg)
    ImageView ivHintWifiStatus;

    @BindView(R.id.tv_hintMsg)
    TextView tvHintWifiStatus;


    private WifiManager mWifiManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher_2);
        ButterKnife.bind(this);
        initIMService();
        EventBus.getDefault().register(this);
        focusView();
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        networkConnectChangedReceiver = new NetworkConnectChangedReceiver();
        getApplicationContext().registerReceiver(networkConnectChangedReceiver, filter);

    }


    @Override
    public void onBackPressed() {
        return;
    }

    void initTask() {
        long lastCallTime = MMKV.defaultMMKV().decodeLong("last_call_time", 0L);
        if (lastCallTime == 0L) {
            tvLastCallTime.setVisibility(View.GONE);
            tvLastCallTimeHour.setVisibility(View.GONE);
        } else {
            tvLastCallTime.setVisibility(View.VISIBLE);
            tvLastCallTimeHour.setVisibility(View.VISIBLE);

            DateFormat df2 = new SimpleDateFormat("MM月dd日");
            String time = df2.format(new Date(lastCallTime));
            DateFormat df3 = new SimpleDateFormat("HH:mm:ss");
            String time1 = df3.format(new Date(lastCallTime));

            tvLastCallTime.setText(time);
            tvLastCallTimeHour.setText(time1);

        }
        tvLastCallInfo.setText("工单：");
        if (HoloLauncherApp.roomId != 0L) {
            tvLastCallInfo.setText("工单：" + HoloLauncherApp.roomId);
        } else {
            tvLastCallInfo.setText("工单：空");
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        initTask();
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

    private void initIMService() {
        Intent intent = new Intent(this, BackgroundService.class);
        startService(intent);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        unregisterReceiver(networkConnectChangedReceiver);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioOrderMessage(AudioOrderMessage message) {
        switch (message.getType()) {
            case VoiceCmdEngine.VoiceCmd_CALL:
                Toast.makeText(getBaseContext(), "触发呼叫命令", Toast.LENGTH_SHORT).show();
                callMajor();
//                onItemClick(0);
                break;
            case VoiceCmdEngine.VoiceCmd_Setting:
                Toast.makeText(getBaseContext(), "触发系统设置命令", Toast.LENGTH_SHORT).show();
                break;
            default:
                HoloMessage holoMessage = new HoloMessage();
                holoMessage.setAction("api.voice.order");
                holoMessage.setExtraMsg(String.valueOf(message.getType()));
                EventBus.getDefault().postSticky(holoMessage);
                break;
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onImEvent(ImEvent imEvent) {
        if (imEvent.getAction() == 3051) {
            long userId = imEvent.getData().getLong("userId");
            callMajor();
        }
    }


    @OnClick(R.id.rf_wifi_conn)
    public void onWifiConn() {
        Intent intent = getPackageManager().getLaunchIntentForPackage("com.holoview.wificonnecter");
        startActivity(intent);
    }


    @OnClick(R.id.rf_recall_communication)
    public void reCommunication() {
        if (!TextUtils.isEmpty(HoloLauncherApp.token)) {
            callMajor();
        }
    }

    @OnClick(R.id.rf_clear_history)
    public void clearHistory() {

        MMKV.defaultMMKV().putLong("last_call_time",0);
        HoloLauncherApp.call_list.clear();
        ImLib.instance().logout();
        HoloLauncherApp.token = "";
        HoloLauncherApp.roomId = 0L;
        HoloLauncherApp.converstaiontype = 0;
        initTask();
//        Toast.makeText(this, "退出成功", Toast.LENGTH_LONG).show();
    }

    @OnClick(R.id.rf_setting)
    public void openSetting() {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.android.settings");
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public void callMajor() {
        try {
            String result = SystemConfigSp.instance().getStrConfig(SystemConfigSp.SysCfgDimension.NAVIINFO);
            NaviRes naviRes = new Gson().fromJson(result, NaviRes.class);
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.realview.holo.call");
            String target = JSON.toJSONString(HoloLauncherApp.call_list);
            intent.putExtra(Constants.CALL_LIST, target);
            intent.putExtra("userSelfId", HoloLauncherApp.userSelfId);
            intent.putExtra("converstaionType", HoloLauncherApp.converstaiontype);
            intent.putExtra("roomId", HoloLauncherApp.roomId);
            intent.putExtra("navi", result);
            intent.putExtra("wss", naviRes.getResult().getSslmcusvr().getProto() + "://" + naviRes.getResult().getSslmcusvr().getUrl() + "/groupcall");
            startActivity(intent);
            MMKV.defaultMMKV().encode("last_call_time", System.currentTimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.rf_call_major_new)
    public void callMajorNew() {
        clearHistory();
        Intent intent = new Intent(this, ScanLoginActivity.class);
        startActivityForResult(intent, Constants.ACTION_START_MAJOR);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Activity.RESULT_OK != resultCode) {
            return;
        }
        if (requestCode == Constants.ACTION_START_MAJOR) {
            initIMService();
        }
    }
//
//
//    @Override
//    public boolean dispatchKeyEvent(KeyEvent event) {
//        if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
//            if (llDialogErrorContent.getVisibility() == View.VISIBLE) {
//                return false;
//            } else {
//                return super.dispatchKeyEvent(event);
//            }
//        }
//        return super.dispatchKeyEvent(event);
//    }
//


    class NetworkConnectChangedReceiver extends BroadcastReceiver {

        private String getConnectionType(int type) {
            String connType = "";
            if (type == ConnectivityManager.TYPE_MOBILE) {
                connType = "数据";
            } else if (type == ConnectivityManager.TYPE_WIFI) {
                connType = "WIFI";
            }
            return connType;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {// 监听wifi的打开与关闭，与wifi的连接无关
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
                Log.e("TAG", "wifiState:" + wifiState);
                switch (wifiState) {
                    case WifiManager.WIFI_STATE_DISABLED:
                        break;
                    case WifiManager.WIFI_STATE_DISABLING:
                        break;
                }
            }
            // 监听wifi的连接状态即是否连上了一个有效无线路由
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                Parcelable parcelableExtra = intent
                        .getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (null != parcelableExtra) {
                    // 获取联网状态的NetWorkInfo对象
                    NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
                    //获取的State对象则代表着连接成功与否等状态
                    NetworkInfo.State state = networkInfo.getState();
                    //判断网络是否已经连接
                    boolean isConnected = state == NetworkInfo.State.CONNECTED;
                    if (isConnected) {
                        ivWifiStatus.setImageResource(R.mipmap.ic_tab_wifi_conn);
                    } else {
                        ivWifiStatus.setImageResource(R.mipmap.ic_tab_wifi_disconn);
                    }
                }
            }
            // 监听网络连接，包括wifi和移动数据的打开和关闭,以及连接上可用的连接都会接到监听
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                //获取联网状态的NetworkInfo对象
                NetworkInfo info = intent
                        .getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                if (info != null) {
                    //如果当前的网络连接成功并且网络连接可用
                    if (NetworkInfo.State.CONNECTED == info.getState() && info.isAvailable()) {
                        if (info.getType() == ConnectivityManager.TYPE_WIFI
                                || info.getType() == ConnectivityManager.TYPE_MOBILE) {
                            ivWifiStatus.setImageResource(R.mipmap.ic_tab_wifi_conn);
                        }
                    } else {
                        ivWifiStatus.setImageResource(R.mipmap.ic_tab_wifi_disconn);
                    }
                }
            }


            NetworkInfo mInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            final WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();

            String hintMsg = getString(R.string.wifi_unconnect_try_add);
            ivHintWifiStatus.setBackgroundResource(R.mipmap.wifi_gray);

            if (mInfo != null && mWifiInfo != null && mWifiInfo.getSSID() != null && mInfo.isConnectedOrConnecting()){
                Log.i("LauncherAcitivity", "onNetStateChange ssid:" + mWifiInfo.getSSID() + " state:" + mInfo.getState());

                if (mWifiInfo.getSSID().equals("<unknown ssid>")){
                }else {

                    String ssid = mWifiInfo.getSSID();
                    ssid = ssid.substring(1, ssid.length() - 1);

                    if (mInfo.isConnected()){
                        hintMsg = getString(R.string.wifi_connected_can_switch, ssid);
                        ivHintWifiStatus.setBackgroundResource(R.mipmap.wifi);
                    }else if (mInfo.isConnectedOrConnecting()){
                        hintMsg = getString(R.string.wifi_connecting_please_wait, ssid);
                        ivHintWifiStatus.setBackgroundResource(R.mipmap.wifi_white);
                    }

                }


            }else {
                hintMsg = getString(R.string.wifi_unconnect_try_add);
            }

            tvHintWifiStatus.setText(Html.fromHtml(hintMsg));

        }
    }

}






