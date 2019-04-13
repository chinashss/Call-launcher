package com.holoview.hololauncher.bean;

import android.content.pm.ApplicationInfo;

/**
 * Created by Mr.kk on 2019/3/20.
 * This Project is android-glass-launcher
 */
public class LauncherApp {
    private String packageName;
    private ApplicationInfo applicationInfo;
    private boolean needNetWork;
    private boolean needLogin;

    public boolean isNeedNetWork() {
        return needNetWork;
    }

    public void setNeedNetWork(boolean needNetWork) {
        this.needNetWork = needNetWork;
    }

    public boolean isNeedLogin() {
        return needLogin;
    }

    public void setNeedLogin(boolean needLogin) {
        this.needLogin = needLogin;
    }

    public ApplicationInfo getApplicationInfo() {
        return applicationInfo;
    }

    public void setApplicationInfo(ApplicationInfo applicationInfo) {
        this.applicationInfo = applicationInfo;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
}
