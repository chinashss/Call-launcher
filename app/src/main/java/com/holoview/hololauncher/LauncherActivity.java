package com.holoview.hololauncher;

import android.animation.Animator;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import com.holoview.hololauncher.activitys.ScanLoginActivity;
import com.holoview.hololauncher.bean.Constants;
import com.hv.imlib.DB.sp.SystemConfigSp;
import com.hv.imlib.HoloMessage;
import com.hv.imlib.ImLib;
import com.hv.imlib.protocol.http.NaviRes;
import com.trios.voicecmd.AudioOrderMessage;
import com.trios.voicecmd.VoiceCmdEngine;

import org.evilbinary.tv.widget.BorderEffect;
import org.evilbinary.tv.widget.BorderView;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Mr.kk on 2019/3/20.
 * This Project is android-glass-launcher
 */
public class LauncherActivity extends BaseActivity {
//    @BindView(R.id.rv_launcher_app_list)
//    RecyclerView rvLauncherAppList;
//
//    RelativeLayout llDialogErrorContent;
//    @BindView(R.id.rv_system_list)
//    RecyclerView rvSystemAppsList;
//
//
//    @BindView(R.id.rv_option_list)
//    RecyclerView rvOptionList;
//    @BindView(R.id.tv_wifi_status)
//    TextView tvWifiStatus;


//    private List<PackageBean> packageNames = new ArrayList<>();
//    private List<LauncherApp> launcherApps = new ArrayList<>();
//    private List<LauncherApp> SystemApps = new ArrayList<>();
//    NetworkConnectChangedReceiver networkConnectChangedReceiver;

//    AppItemAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher_2);
        ButterKnife.bind(this);
        initIMService();
        EventBus.getDefault().register(this);
        focusView();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

//        networkConnectChangedReceiver = new NetworkConnectChangedReceiver();
//        registerReceiver(networkConnectChangedReceiver, filter);

    }


    @Override
    public void onBackPressed() {
        return;
    }

    @Override
    protected void onResume() {
        super.onResume();
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
//        unregisterReceiver(networkConnectChangedReceiver);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioOrderMessage(AudioOrderMessage message) {
        switch (message.getType()) {
            case VoiceCmdEngine.VoiceCmd_CALL:
                Toast.makeText(getBaseContext(), "触发呼叫命令", Toast.LENGTH_SHORT).show();
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


    @OnClick(R.id.rf_wifi_conn)
    public void onWifiConn() {
        Intent intent = getPackageManager().getLaunchIntentForPackage("com.holoview.wificonnecter");
        startActivity(intent);
    }


    @OnClick(R.id.rf_recall_communication)
    public void reCommunication() {
        initIMService();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                callMajor();
            }
        };
        Timer timer = new Timer();
        timer.schedule(task, 1500);//3秒后执行TimeTask的run方法
    }

    @OnClick(R.id.rf_clear_history)
    public void clearHistory() {
        HoloLauncherApp.call_list.clear();
        ImLib.instance().logout();
        HoloLauncherApp.token = "";
        HoloLauncherApp.roomId = 0L;
        HoloLauncherApp.converstaiontype = 0;
        Toast.makeText(this, "退出成功", Toast.LENGTH_LONG).show();
    }

    @OnClick(R.id.rf_setting)
    public void openSetting() {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.android.settings");
            startActivity(intent);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    @OnClick(R.id.rf_call_major)
    public void callMajor() {
        if (TextUtils.isEmpty(HoloLauncherApp.token)) {
            Intent intent = new Intent(this, ScanLoginActivity.class);
            startActivityForResult(intent, Constants.ACTION_START_MAJOR);
            return;
        }
        try {
            String result = SystemConfigSp.instance().getStrConfig(SystemConfigSp.SysCfgDimension.NAVIINFO);
            NaviRes naviRes = new Gson().fromJson(result, NaviRes.class);
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.realview.holo.call");
            intent.putExtra(Constants.CALL_LIST, JSON.toJSONString(HoloLauncherApp.call_list));
            intent.putExtra("userSelfId", HoloLauncherApp.userSelfId);
            intent.putExtra("converstaionType", HoloLauncherApp.converstaiontype);
            intent.putExtra("roomId", HoloLauncherApp.roomId);
            intent.putExtra("navi", result);
            intent.putExtra("wss", naviRes.getResult().getSslmcusvr().getProto() + "://" + naviRes.getResult().getSslmcusvr().getUrl() + "/groupcall");
            startActivity(intent);
        }catch (Exception e){
            e.printStackTrace();
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Activity.RESULT_OK != resultCode) {
            return;
        }
        if (requestCode == Constants.ACTION_START_MAJOR) {
            callMajor();
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

}

//
//    class NetworkConnectChangedReceiver extends BroadcastReceiver {
//
//        private String getConnectionType(int type) {
//            String connType = "";
//            if (type == ConnectivityManager.TYPE_MOBILE) {
//                connType = "数据";
//            } else if (type == ConnectivityManager.TYPE_WIFI) {
//                connType = "WIFI";
//            }
//            return connType;
//        }
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {// 监听wifi的打开与关闭，与wifi的连接无关
//                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
//                Log.e("TAG", "wifiState:" + wifiState);
//                switch (wifiState) {
//                    case WifiManager.WIFI_STATE_DISABLED:
//                        break;
//                    case WifiManager.WIFI_STATE_DISABLING:
//                        break;
//                }
//            }
//            // 监听wifi的连接状态即是否连上了一个有效无线路由
//            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
//                Parcelable parcelableExtra = intent
//                        .getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
//                if (null != parcelableExtra) {
//                    // 获取联网状态的NetWorkInfo对象
//                    NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
//                    //获取的State对象则代表着连接成功与否等状态
//                    NetworkInfo.State state = networkInfo.getState();
//                    //判断网络是否已经连接
//                    boolean isConnected = state == NetworkInfo.State.CONNECTED;
//                    if (isConnected) {
//                        tvWifiStatus.setText("Wifi已连接");
//                    } else {
//                        tvWifiStatus.setText("Wifi已断开");
//                    }
//                }
//            }
//            // 监听网络连接，包括wifi和移动数据的打开和关闭,以及连接上可用的连接都会接到监听
//            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
//                //获取联网状态的NetworkInfo对象
//                NetworkInfo info = intent
//                        .getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
//                if (info != null) {
//                    //如果当前的网络连接成功并且网络连接可用
//                    if (NetworkInfo.State.CONNECTED == info.getState() && info.isAvailable()) {
//                        if (info.getType() == ConnectivityManager.TYPE_WIFI
//                                || info.getType() == ConnectivityManager.TYPE_MOBILE) {
//                            tvWifiStatus.setText(getConnectionType(info.getType()) + "已连接");
//                        }
//                    } else {
//                        tvWifiStatus.setText(getConnectionType(info.getType()) + "已断开");
//                        Log.i("TAG", getConnectionType(info.getType()) + "断开");
//                    }
//                }
//            }
//        }
//    }



