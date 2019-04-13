package com.holoview.hololauncher;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

import com.hv.imlib.DB.sp.SystemConfigSp;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.tencent.bugly.Bugly;
import com.tencent.bugly.beta.Beta;
import com.trios.voicecmd.VoiceCmdEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * Created by admin on 2019/1/21.
 */

public class HoloLauncherApp extends MultiDexApplication {
    private VoiceCmdEngine cmdEngine;
    private static HoloLauncherApp app;

    public static long userSelfId;
    public static String token;
    public static long roomId;
    public static int converstaiontype;

    public static List<Long> call_list = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        SystemConfigSp.instance().init(this);
        initVoice();
        initBugly();
        initImageLoader(this);
    }

    private void initBugly() {
        Bugly.init(getApplicationContext(), "1abb274371", false);
        Bugly.setAppChannel(getApplicationContext(), "Holo");
        Beta.autoInit = true;
        Beta.autoCheckUpgrade = true;
    }

    public static HoloLauncherApp getApp() {
        return app;
    }

    public VoiceCmdEngine getCmdEngine() {
        return cmdEngine;
    }

    public void initVoice() {
        cmdEngine = VoiceCmdEngine.getInstance();
        if (this.cmdEngine != null) {
            this.cmdEngine.InitVoice(getApplicationContext());
        }
    }

    public void registerVoiceCmd(Handler handler, int cmdId) {
        if (this.cmdEngine != null) {
            this.cmdEngine.RegisterVoiceCmd(handler, cmdId);
        }
    }

    public void unRegisterVoiceCmd(Handler handler, int cmdID) {
        if (this.cmdEngine != null) {
            this.cmdEngine.UnRegisterVoiceCmd(handler, cmdID);
        }
    }

    public void startVoice() {
//        if (this.cmdEngine != null) {
//            this.cmdEngine.StartListener();
//        }
    }

    private void initImageLoader(Context context) {
        // This configuration tuning is custom. You can tune every option, you may tune some of them,
        // or you can create default configuration by
        //  ImageLoaderConfiguration.createDefault(this);
        // method.
        ImageLoaderConfiguration.Builder config = new ImageLoaderConfiguration.Builder(context);
        config.threadPriority(Thread.NORM_PRIORITY - 2);
        config.denyCacheImageMultipleSizesInMemory();
        config.diskCacheFileNameGenerator(new Md5FileNameGenerator());
        config.diskCacheSize(50 * 1024 * 1024); // 50 MiB
        config.tasksProcessingOrder(QueueProcessingType.LIFO);
        config.writeDebugLogs(); // Remove for release app

        // Initialize ImageLoader with configuration.
        ImageLoader.getInstance().init(config.build());
    }


    /**
     * 当应用彻底退出，注销进程通讯
     */
    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    public static String getEpsonLoginToken() {
        String m_szDevIDshort = "35" +
                Build.BOARD.length() % 10 +
                Build.BRAND.length() % 10 +
                Build.CPU_ABI.length() % 10 +
                Build.DEVICE.length() % 10 +
                Build.DISPLAY.length() % 10 +
                Build.HOST.length() % 10 +
                Build.ID.length() % 10 +
                Build.MANUFACTURER.length() % 10 +
                Build.MODEL.length() % 10 +
                Build.PRODUCT.length() % 10 +
                Build.TAGS.length() % 10 +
                Build.TYPE.length() % 10 +
                Build.USER.length() % 10;
        String serial = Build.SERIAL;
        //String uuid = new UUID(m_szDevIDshort.hashCode(),serial.hashCode()).toString();
        return new UUID(m_szDevIDshort.hashCode(), serial.hashCode()).toString();
    }


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public static boolean isActivityTop(Class cls, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        String name = manager.getRunningTasks(1).get(0).topActivity.getClassName();
        return name.equals(cls.getName());
    }

}
