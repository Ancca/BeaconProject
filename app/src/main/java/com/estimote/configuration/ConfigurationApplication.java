package com.estimote.configuration;

import android.app.Application;
import android.util.Log;

import com.estimote.sdk.EstimoteSDK;

public class ConfigurationApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        EstimoteSDK.initialize(getApplicationContext(), "t5juva00-students-oamk-fi--7p2", "4284e1214e0ac62d411e459b8728dd5b");
        EstimoteSDK.enableDebugLogging(true);
    }
}
