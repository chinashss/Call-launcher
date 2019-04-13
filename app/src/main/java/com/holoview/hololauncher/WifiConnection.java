package com.holoview.hololauncher;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.IOException;
import java.util.List;


/**
 * Created by liuhongyu on 2018/10/30.
 */

public class WifiConnection {
    private static final String TAG = "WifiConnection";

    private static final String SSID = "yinyueke";
    private static final String PASSWORD = "Lishuiqiao1804";
    public static final String WIFI_IS_NOT_OPEN = "wifi is closed.";

    public WifiManager mWifiManager = null;
    private Context mContext = null;
    private int mNetworkID = -1;

    private static WifiConnection mWifiConfig = null;

    public static WifiConnection getInstance(Context ctx) {
        if (mWifiConfig == null) {
            mWifiConfig = new WifiConnection(ctx);
        }

        return mWifiConfig;
    }

    WifiConnection(Context context) {
        this.mContext = context;
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }



    /**
     * 网络是否连接
     */
    public boolean isWifiConnected() {
        if (mContext != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) mContext
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null && mNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                return mNetworkInfo.isConnected();
            }
        }
        return false;
    }

    /**
     * Show wifi list
     */
    public String getWifiList() {
        if (WifiManager.WIFI_STATE_ENABLED != mWifiManager.getWifiState()) {
            return WIFI_IS_NOT_OPEN;
        }
        String text = "";
        List<ScanResult> results = mWifiManager.getScanResults();
        for (ScanResult result : results) {
            String SSID = result.SSID;
            if (SSID.startsWith("Sun")) {
                int db = result.level;
                text += SSID + ":" + db;
                return text;
            }
        }
        return "";
    }

    /**
     * 打开wifi开关
     */
    public void openWifi() {
        mWifiManager.setWifiEnabled(true);
    }

    public void closeWifi() {
        mWifiManager.setWifiEnabled(false);
    }

    /**
     * 是否打开开关
     */
    public boolean isWifiOpened() {
        return WifiManager.WIFI_STATE_ENABLED == mWifiManager.getWifiState();
    }

    public boolean isWifiEnable() {
        return mWifiManager.isWifiEnabled();
    }

    /**
     * 关闭开关
     */
    public void shutdownWiFi() {
        mWifiManager.setWifiEnabled(false);
        mNetworkID = -1;
    }

    /**
     * 连接指定Wifi
     */
    public boolean connectWifi(final String ssid, final String password,
                               final SecurityMode mode) {
        WifiInfo info = getWifiInfo();

        if (isWifiConnected() && info != null
                && info.getSSID().equals("\"" + ssid + "\"")) {
            Log.d(TAG, "connectWifi---->" + info.toString() + " ");
            return true;
        }
        Log.d(TAG, "connectWifi---->" + isWifiConnected() + " ");
        List<WifiConfiguration> existingConfigs = mWifiManager
                .getConfiguredNetworks();
        if (existingConfigs == null) {
            return false;
        }
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + ssid + "\"")) {
                mNetworkID = existingConfig.networkId;
                break;
            }
        }
        if (mNetworkID <= -1) {
            WifiConfiguration config = new WifiConfiguration();
            config.SSID = "\"" + ssid + "\"";
            if (mode == SecurityMode.OPEN) {
                config.wepKeys[0] = "";
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.wepTxKeyIndex = 0;
            } else if (password != null && !"".equals(password)) {
                if (mode == SecurityMode.WEP) {
                    config.preSharedKey = "\"" + password + "\"";
                    config.allowedAuthAlgorithms
                            .set(WifiConfiguration.AuthAlgorithm.SHARED);
                    config.allowedGroupCiphers
                            .set(WifiConfiguration.GroupCipher.CCMP);
                    config.allowedGroupCiphers
                            .set(WifiConfiguration.GroupCipher.TKIP);
                    config.allowedGroupCiphers
                            .set(WifiConfiguration.GroupCipher.WEP40);
                    config.allowedGroupCiphers
                            .set(WifiConfiguration.GroupCipher.WEP104);
                    config.allowedKeyManagement
                            .set(WifiConfiguration.KeyMgmt.NONE);
                    config.wepTxKeyIndex = 0;
                } else {
                    config.preSharedKey = "\"" + password + "\"";
                    config.allowedAuthAlgorithms
                            .set(WifiConfiguration.AuthAlgorithm.OPEN);
                    config.allowedGroupCiphers
                            .set(WifiConfiguration.GroupCipher.TKIP);
                    config.allowedKeyManagement
                            .set(WifiConfiguration.KeyMgmt.WPA_PSK);
                    config.allowedPairwiseCiphers
                            .set(WifiConfiguration.PairwiseCipher.TKIP);
                    config.allowedGroupCiphers
                            .set(WifiConfiguration.GroupCipher.CCMP);
                    config.allowedPairwiseCiphers
                            .set(WifiConfiguration.PairwiseCipher.CCMP);
                    config.status = WifiConfiguration.Status.ENABLED;
                }
            }
            mNetworkID = mWifiManager.addNetwork(config);
        }
        // 连接该网络
        return mWifiManager.enableNetwork(mNetworkID, true);

    }

    public WifiInfo getWifiInfo() {
        WifiInfo info = null;
        try {
            if (isWifiConnected()) {
                info = mWifiManager.getConnectionInfo();
            }
        } catch (Exception e) {
            Log.e(TAG, "getWifiInfo", e);
        }
        return info;
    }

    /**
     * 网络加密模式
     */
    public enum SecurityMode {
        OPEN, WEP, WPA2
    }

    /**
     * 初始化WiFi连接
     */
    public boolean initWifiConnect() {
        Log.d("vivi", "初始化Wifi连接---->" + " 开始连接");
        return connectWifi(SSID, PASSWORD, SecurityMode.WPA2);
    }

    public boolean isNetworkOnline() {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("ping -c 3 www.baidu.com");
            int exitValue = ipProcess.waitFor();
            Log.i("Avalible", "Process:" + exitValue);
            return (exitValue == 0);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }
}
