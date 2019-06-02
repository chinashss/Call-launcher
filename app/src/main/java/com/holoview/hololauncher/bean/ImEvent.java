package com.holoview.hololauncher.bean;

import android.os.Bundle;

public class ImEvent {
    private int action;
    private Bundle data;

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public Bundle getData() {
        return data;
    }

    public void setData(Bundle data) {
        this.data = data;
    }
}
