package com.holoview.hololauncher.http;

import android.util.Log;

import okhttp3.logging.HttpLoggingInterceptor;

public class HttpLoggerInterceptor implements HttpLoggingInterceptor.Logger {
    private static final String TAG = "OkHttp";

    @Override
    public void log(String message) {
        Log.i(TAG, message);
    }
}
