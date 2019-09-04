package com.holoview.hololauncher.basic;

import android.util.Log;

/**
 * Created by Mr.kk on 2018/7/21.
 * This Project was duckchat-android
 */
public class DuckLog {
    public static final boolean showSync = false;
    public static final boolean showLife = false;
    public static final boolean showHttp = true;
    public static final boolean jsWeb = true;
    public static final boolean api = true;
    public static final boolean push = true;


    public static void life(String message) {
        if (showLife)
            Log.i("DuckLife", message);
    }

    public static void e(String message) {
        Log.e("DuckConn", message);
    }

    public static void e(String from, Exception e) {
        Log.e("DuckException", "from:" + from, e);
    }

    public static void d(String message) {
        Log.d("DuckConn", message);
    }

    public static void i(String message) {
        Log.i("DuckConn", message);
    }


    public static void pushLog(String message) {
        if (push)
            Log.i("pushLog", message);
        //umeng_push_notification_default_sound.mp3
    }


    public static void httplog(String body) {
        if (showHttp)
            Log.i("DuckHttpBody", body);
    }

    public static void jsLog(String method, String body) {
        if (jsWeb)
            Log.i("JS-WEB", "JsNative Method :" + method + "  body:{" + body + "}");
    }

    public static void imLog(String body) {
        Log.i("IM-Duck", body);
    }

    public static void webLog(String body) {
        Log.i("Web-Duck", body);
    }

    public static void intentLog(String body) {
        Log.i("Intent-Duck", body);
    }

    public static void netLog(String body) {
        Log.i("Net-Duck", body);
    }

    public static void netLog(Throwable t) {
        Log.e("Net-Duck", "error", t);
    }

    public static void proxyLog(String body) {
        Log.i("Proxy-Duck", body);
    }

    public static void testLog(String body) {
        Log.i("Test-Duck", body);
    }

    public static void syncLog(String body) {
        if (showSync)
            Log.i("Sync-Duck", body);
    }

}
