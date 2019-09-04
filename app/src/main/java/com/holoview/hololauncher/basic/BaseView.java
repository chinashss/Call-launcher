package com.holoview.hololauncher.basic;

import android.content.Context;

import com.afollestad.materialdialogs.MaterialDialog;


public interface BaseView {
    Context getContext();

    void showToast(String msg);

    void showDialog(String content, String positiveText, String negativeText, MaterialDialog.SingleButtonCallback callback);

    void showDialog(String content, String positiveText, MaterialDialog.SingleButtonCallback callback);

    MaterialDialog showProgressDialog(String title, String content);
}
