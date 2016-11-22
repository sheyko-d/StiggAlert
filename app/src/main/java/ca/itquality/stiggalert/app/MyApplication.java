package ca.itquality.stiggalert.app;

import android.app.Application;
import android.content.Context;

public class MyApplication extends Application {

    private static MyApplication sContext;

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = this;
    }

    public static Context getContext() {
        return sContext;
    }
}