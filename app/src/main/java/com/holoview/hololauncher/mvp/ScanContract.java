package com.holoview.hololauncher.mvp;


import com.holoview.hololauncher.basic.BasePresenter;
import com.holoview.hololauncher.basic.BaseView;

/**
 * Created by Mr.kk on 2018/8/15.
 * This Project was duckchat-android
 */
public class ScanContract {
    public interface View extends BaseView {
        void onNaviForUrlSuccess(String qrId);

        void loginSuccess();
    }

    public interface Presenter extends BasePresenter<View> {
        void naviForUrl(String QRResult);

        void login(String qrId);
    }
}
