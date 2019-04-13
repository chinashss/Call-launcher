package com.holoview.hololauncher.bean;

/**
 * Created by Mr.kk on 2019/3/27.
 * This Project is android-glass-launcher
 */
public class PackageBean {
    private String packageName;

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

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

    private boolean needNetWork;
    private boolean needLogin;

    public PackageBean(String packageName) {
        this.packageName = packageName;
        this.needNetWork = false;
        this.needLogin = false;
    }

    public PackageBean(String packageName, boolean needNetWork, boolean needLogin) {
        this.packageName = packageName;
        this.needNetWork = needNetWork;
        this.needLogin = needLogin;
    }
}
