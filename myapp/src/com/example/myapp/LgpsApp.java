package com.example.myapp;

import android.app.Application;
import android.os.Handler;

/**
 * Created by cyandr on 2016/6/12 0012.
 */
public class LgpsApp extends Application {

    private Handler myHandler = null;
    private int NOW_MODE;
    private String NETIP=null;

    public String getNETIP() {
        return NETIP;
    }

    public void setNETIP(String NETIP) {
        this.NETIP = NETIP;
    }

    public Handler getHandler() {
        return myHandler;
    }

    public void setHandler(Handler handler) {
        this.myHandler = handler;
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    public void setNOW_MODE(int modes) {
        NOW_MODE = modes;
    }

    public int getNOW_MODE() {
        return NOW_MODE;
    }

    public class LgpsMode {
        final static byte LOCATING_MODE = 90;
        final static byte NAVAGATING_MODE = 91;
        final static byte SENTRY_MODE = 92;
        final static byte SECURE_MODE = 93;
        final static byte INPUT_MODE = 94;
        final static byte QUIT_MODE = 120;
    }
}
