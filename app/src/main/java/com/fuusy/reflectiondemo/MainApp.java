package com.fuusy.reflectiondemo;

import android.app.Application;
import android.content.Context;



public class MainApp extends Application {


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        //Reflection.unseal(base);

    }
}
