package com.keyeswest.roboroller;

import android.app.Application;

import timber.log.Timber;

public class RoboRoller extends Application {

    @Override
    public void onCreate(){
        super.onCreate();

        Timber.plant(new Timber.DebugTree(){
            // include line numbers
            @Override
            protected String createStackElementTag(StackTraceElement element){
                return super.createStackElementTag(element) + ':' + element.getLineNumber();
            }
        });

    }
}
