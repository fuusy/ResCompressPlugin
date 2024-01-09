package com.fuusy.reflectiondemo;

import android.app.Application;
import android.content.Context;



/**
 * Created on 2023/10/19.
 *
 * @author shiyao.fu
 * @email shiyao.fu@ximalaya.com
 */
public class MainApp extends Application {


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        //Reflection.unseal(base);

    }
}
