package com.holoview.hololauncher.basic;


public interface BasePresenter<V extends BaseView>{
    void attachView(V view);

    void detachView();
}
