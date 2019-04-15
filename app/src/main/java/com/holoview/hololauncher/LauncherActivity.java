package com.holoview.hololauncher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import com.holo.tvwidget.MetroViewBorderImpl;
import com.holoview.hololauncher.activitys.ScanLoginActivity;
import com.holoview.hololauncher.activitys.ScanWifiActivity;
import com.holoview.hololauncher.adapter.AppItemAdapter;
import com.holoview.hololauncher.adapter.OptionItemAdapter;
import com.holoview.hololauncher.adapter.SystemItemAdapter;
import com.holoview.hololauncher.basic.ActivityCollector;
import com.holoview.hololauncher.bean.Constants;
import com.holoview.hololauncher.bean.LauncherApp;
import com.holoview.hololauncher.bean.PackageBean;
import com.hv.imlib.DB.sp.SystemConfigSp;
import com.hv.imlib.HoloMessage;
import com.hv.imlib.ImLib;
import com.hv.imlib.protocol.http.NaviRes;
import com.trios.voicecmd.AudioOrderMessage;
import com.trios.voicecmd.VoiceCmdEngine;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Mr.kk on 2019/3/20.
 * This Project is android-glass-launcher
 */
public class LauncherActivity extends BaseActivity implements AppItemAdapter.OnOptionItemClickLister, SystemItemAdapter.OnSystemItemClickLister, OptionItemAdapter.OnOptionItemClickLister {
    @BindView(R.id.rv_launcher_app_list)
    RecyclerView rvLauncherAppList;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.progress_tips)
    TextView progressTips;
    @BindView(R.id.ll_dialog_content)
    LinearLayout llDialogContent;
    @BindView(R.id.tv_wifi_conn_tips)
    TextView tvWifiConnTips;
    @BindView(R.id.tv_wifi_conn_success_tips)
    TextView tvWifiConnSuccessTips;
    @BindView(R.id.ll_dialog_error_content)
    RelativeLayout llDialogErrorContent;
    @BindView(R.id.rv_system_list)
    RecyclerView rvSystemAppsList;
    @BindView(R.id.ll_wifi_switch_tips)
    LinearLayout llWifiSwitchTips;
    @BindView(R.id.ll_wifi_conn_tips)
    LinearLayout llWifiConnTips;
    @BindView(R.id.ll_wifi_conn_success_tips)
    LinearLayout llWifiConnSuccessTips;
    @BindView(R.id.iv_network_error_logo)
    ImageView ivNetworkErrorLogo;
    @BindView(R.id.tv_network_error_tips)
    TextView tvNetworkErrorTips;
    @BindView(R.id.iv_network_state_success)
    ImageView ivNetworkStateSuccess;
    @BindView(R.id.ll_view_network_state_content)
    LinearLayout llViewNetworkStateContent;
    @BindView(R.id.rv_option_list)
    RecyclerView rvOptionList;


    private int status = 0;

    private List<PackageBean> packageNames = new ArrayList<>();
    private List<LauncherApp> launcherApps = new ArrayList<>();
    private List<LauncherApp> SystemApps = new ArrayList<>();
    NetworkConnectChangedReceiver networkConnectChangedReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        ButterKnife.bind(this);
        initIMService();
        EventBus.getDefault().register(this);
        initExtraAppList();
        checkWifi();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        networkConnectChangedReceiver = new NetworkConnectChangedReceiver();
        registerReceiver(networkConnectChangedReceiver, filter);

    }


    private void checkLogin(int position) {
        if (TextUtils.isEmpty(HoloLauncherApp.token)) {
            Intent intent = new Intent(this, ScanLoginActivity.class);
            intent.putExtra(Constants.ACTION_SCAN_TYPE, Constants.ACTION_REQUEST_LOGIN);
            intent.putExtra(Constants.ACTION_FOR_RESULT, position);
            startActivityForResult(intent, Constants.ACTION_REQUEST_LOGIN);
        }
    }

    private void checkWifi() {
        connection = WifiConnection.getInstance(this);
        if (connection.isWifiConnected()) {
            ivNetworkStateSuccess.setVisibility(View.VISIBLE);
            llViewNetworkStateContent.setVisibility(View.VISIBLE);
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            llViewNetworkStateContent.setVisibility(View.GONE);
                        }
                    });
                }
            };
            Timer timer = new Timer();
            timer.schedule(task, 2000);//3秒后执行TimeTask的run方
            return;
        }
        llDialogContent.setVisibility(View.VISIBLE);
        next();
    }

    public void next() {
        handler.sendEmptyMessageDelayed(0, 2000);
    }

    WifiConnection connection;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (status == 0) {
                status++;
                boolean isOpen = connection.isWifiOpened();
                if (isOpen) {
                    Log.i("TAG", "WIFI is Open");
                } else {
                    connection.openWifi();
                }
                next();
            } else if (status == 1) {
                tvWifiConnTips.setTextColor(Color.WHITE);
                status++;
                boolean isConn = connection.isWifiConnected();
                if (!isConn) {
                    if (HoloLauncherApp.isActivityTop(ScanWifiActivity.class, LauncherActivity.this)) {
                        return;
                    }
                    Intent intent = new Intent(LauncherActivity.this, ScanWifiActivity.class);
                    intent.putExtra(Constants.ACTION_SCAN_TYPE, Constants.ACTION_REQUEST_WIFI);
                    startActivityForResult(intent, Constants.ACTION_REQUEST_WIFI);
                } else {
                    next();
                }
            } else if (status == 2) {
                tvWifiConnSuccessTips.setTextColor(Color.WHITE);
                llDialogContent.setVisibility(View.GONE);
            } else if (status == -1) {
                llDialogContent.setVisibility(View.GONE);
                llDialogErrorContent.setVisibility(View.VISIBLE);
            }
        }
    };


    private void initExtraAppList() {
        List<String> list = new ArrayList<>();
        list.add("切换工单");
        LinearLayoutManager itemLayoutManager = new LinearLayoutManager(this);
        itemLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rvOptionList.setLayoutManager(itemLayoutManager);
        rvOptionList.setFocusable(false);
        OptionItemAdapter optionItemAdapter = new OptionItemAdapter(this, list);
        rvOptionList.setAdapter(optionItemAdapter);
        optionItemAdapter.setOnOptionItemClickLister(this);
        rvOptionList.scrollToPosition(0);


        LinearLayoutManager gridlayoutManager = new LinearLayoutManager(this);
        gridlayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        rvLauncherAppList.setLayoutManager(gridlayoutManager);
        rvLauncherAppList.setFocusable(false);
        loadDataFromSystem();
        AppItemAdapter adapter = new AppItemAdapter(this, launcherApps);
        rvLauncherAppList.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        adapter.setOnOptionItemClickLister(this);
        rvLauncherAppList.scrollToPosition(0);


        LinearLayoutManager systemLayoutManager = new LinearLayoutManager(this);
        systemLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rvSystemAppsList.setLayoutManager(systemLayoutManager);
        rvSystemAppsList.setFocusable(false);
        loadSystemData();
        SystemItemAdapter systemAdapter = new SystemItemAdapter(this, SystemApps);
        rvSystemAppsList.setAdapter(systemAdapter);
        systemAdapter.setOnSystemItemClickLister(this);
        rvSystemAppsList.scrollToPosition(0);


    }

    private void loadSystemData() {
        PackageManager pm = getPackageManager();
        try {
            ApplicationInfo applicationInfo = pm.getApplicationInfo("com.android.settings", 0);
            LauncherApp app = new LauncherApp();
            app.setApplicationInfo(applicationInfo);
            app.setNeedLogin(false);
            app.setNeedNetWork(false);
            app.setPackageName("com.android.settings");
            SystemApps.add(app);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }


    private void focusView() {
        MetroViewBorderImpl mMetroViewBorderImpl = new MetroViewBorderImpl(this);
        mMetroViewBorderImpl.setBackgroundResource(R.drawable.border_color);
        mMetroViewBorderImpl.attachTo(rvSystemAppsList);
        mMetroViewBorderImpl.attachTo(rvOptionList);
        mMetroViewBorderImpl.attachTo(rvLauncherAppList);


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
                onItemClick(0);
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


    private void loadDataFromSystem() {
        packageNames.add(new PackageBean("com.realview.holo.call", true, true));
        packageNames.add(new PackageBean("com.holo.live", true, true));
        for (PackageBean mPackage : packageNames) {
            PackageManager pm = getPackageManager();
            try {
                ApplicationInfo applicationInfo = pm.getApplicationInfo(mPackage.getPackageName(), 0);
                LauncherApp app = new LauncherApp();
                app.setApplicationInfo(applicationInfo);
                app.setNeedLogin(mPackage.isNeedLogin());
                app.setNeedNetWork(mPackage.isNeedNetWork());
                app.setPackageName(mPackage.getPackageName());
                launcherApps.add(app);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

    }


    @Override
    public void onItemClick(int position) {
        if (launcherApps.get(position).isNeedLogin()) {
            if (TextUtils.isEmpty(HoloLauncherApp.token)) {
                Toast.makeText(LauncherActivity.this, "请登录完再操作", Toast.LENGTH_LONG).show();
                checkLogin(position);
            } else {
                startApp(launcherApps.get(position));
            }
        } else {
            startApp(launcherApps.get(position));
        }
    }

    public void startApp(LauncherApp app) {
        try {
            String result = SystemConfigSp.instance().getStrConfig(SystemConfigSp.SysCfgDimension.NAVIINFO);
            NaviRes naviRes = new Gson().fromJson(result, NaviRes.class);
            /**知道要跳转应用的包命与目标Activity*/
            SharedPreferences sp = getSharedPreferences("config", MODE_PRIVATE);
            Intent intent = getPackageManager().getLaunchIntentForPackage(app.getPackageName());
            if (app.isNeedLogin()) {
                intent.putExtra(Constants.CALL_LIST, JSON.toJSONString(HoloLauncherApp.call_list));
                intent.putExtra("userSelfId", HoloLauncherApp.userSelfId);
                intent.putExtra("converstaionType", HoloLauncherApp.converstaiontype);
                intent.putExtra("roomId", HoloLauncherApp.roomId);
                intent.putExtra("navi", result);
                intent.putExtra("wss", naviRes.getResult().getSslmcusvr().getProto() + "://" + naviRes.getResult().getSslmcusvr().getUrl() + "/groupcall");
            }
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.ACTION_REQUEST_LOGIN) {
            if (resultCode == Activity.RESULT_OK) {
                initIMService();
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        startApp(launcherApps.get(data.getIntExtra(Constants.ACTION_FOR_RESULT, 0)));
                    }
                };
                Timer timer = new Timer();
                timer.schedule(task, 1500);//3秒后执行TimeTask的run方法
            }
            return;
        }
        if (requestCode == Constants.ACTION_REQUEST_WIFI) {
            if (resultCode == Activity.RESULT_OK) {
                doWifiConnection(data.getStringExtra(Constants.AP_CONFIG));
            } else {
                if (status == 2) {
                    return;
                }
                status = -1;
                handler.sendEmptyMessageDelayed(0, 1000);
            }
        }
    }


    /**
     * 去连接wifi
     *
     * @param qrcode
     */
    private void doWifiConnection(String qrcode) {
        String[] arryText = qrcode.split(";");
        String ssid = arryText[1];
        String password = arryText[2];
        int mode = Integer.parseInt(arryText[3]);
        boolean connectStatus = connection.connectWifi(ssid, password, WifiConnection.SecurityMode.values()[mode]);
        if (connectStatus) {
            connSuccss();
        } else {

        }
    }


    public void connSuccss() {
        if (HoloLauncherApp.isActivityTop(ScanWifiActivity.class, LauncherActivity.this)) {
            ActivityCollector.closeActivity(ScanWifiActivity.class);
        }
        next();


        ivNetworkStateSuccess.setVisibility(View.VISIBLE);
        llViewNetworkStateContent.setVisibility(View.VISIBLE);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        llViewNetworkStateContent.setVisibility(View.GONE);
                    }
                });
            }
        };
        Timer timer = new Timer();
        timer.schedule(task, 2000);//3秒后执行TimeTask的run方法

        focusView();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (llDialogErrorContent.getVisibility() == View.VISIBLE) {
                reCheck();
                return false;
            } else {
                return super.dispatchKeyEvent(event);
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void reCheck() {
        llDialogErrorContent.setVisibility(View.GONE);
        status = 0;
        checkWifi();
    }


    @Override
    public void onSystemItemClick(int position) {
        startApp(SystemApps.get(position));
    }

    @Override
    public void onOptionItemClick(int position) {
        if (position == 0) {
            HoloLauncherApp.call_list.clear();
            ImLib.instance().logout();
            HoloLauncherApp.token = "";
            HoloLauncherApp.roomId = 0L;
            HoloLauncherApp.converstaiontype = 0;
            Toast.makeText(this, "退出成功", Toast.LENGTH_LONG).show();
            checkLogin(0);
        }
    }


    class NetworkConnectChangedReceiver extends BroadcastReceiver {
        private String getConnectionType(int type) {
            String connType = "";
            if (type == ConnectivityManager.TYPE_MOBILE) {
                connType = "3G网络数据";
            } else if (type == ConnectivityManager.TYPE_WIFI) {
                connType = "WIFI网络";
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
                    Log.e("TAG", "isConnected:" + isConnected);
                    if (isConnected) {
                        status = 2;
                        connSuccss();
                    } else {
                        reCheck();
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
                            Log.i("TAG", getConnectionType(info.getType()) + "连上");
                        }
                    } else {
                        Log.i("TAG", getConnectionType(info.getType()) + "断开");
                    }
                }
            }
        }
    }
}


