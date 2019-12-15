package com.example.playerandrecorder.Application;

import android.app.Application;
import android.os.Handler;

import com.example.playerandrecorder.AppConstants;
import com.example.playerandrecorder.BuildConfig;
import com.example.playerandrecorder.Libs.Injector;
import com.example.playerandrecorder.Utills.AndroidUtils;

import timber.log.Timber;

public class PlayerRecorderApplication extends Application {



    private static String PACKAGE_NAME ;
    public static volatile Handler applicationHandler;

    private static float screenWidthDp = 0;

    public static Injector injector;

    private static boolean isRecording = false;

    public static Injector getInjector() {
        return injector;
    }

    public static String appPackage() {
        return PACKAGE_NAME;
    }

    public static float getDpPerSecond(float durationSec) {
        if (durationSec > AppConstants.LONG_RECORD_THRESHOLD_SECONDS) {
            return AppConstants.WAVEFORM_WIDTH * screenWidthDp / durationSec;
        } else {
            return AppConstants.SHORT_RECORD_DP_PER_SECOND;
        }
    }

    public static int getLongWaveformSampleCount() {
        return (int)(AppConstants.WAVEFORM_WIDTH * screenWidthDp);
    }


    private static PlayerRecorderApplication _mApplicationContext;

    public static  PlayerRecorderApplication getApplicationContextFromApp(){
        if(_mApplicationContext!=null)
            return _mApplicationContext;
        return new PlayerRecorderApplication();
    }



    @Override
    public void onCreate() {
        super.onCreate();
        _mApplicationContext = this;

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree() {
                @Override
                protected String createStackElementTag(StackTraceElement element) {
                    return "AR-AR " + super.createStackElementTag(element) + ":" + element.getLineNumber();
                }
            });
        }

        super.onCreate();

        PACKAGE_NAME = getApplicationContext().getPackageName();
        applicationHandler = new Handler(getApplicationContext().getMainLooper());
        screenWidthDp = AndroidUtils.pxToDp(AndroidUtils.getScreenWidth(getApplicationContext()));
        injector = new Injector(getApplicationContext());
    }


    public static boolean isRecording() {
        return isRecording;
    }

    public static void setRecording(boolean recording) {
        PlayerRecorderApplication.isRecording = recording;
    }
}
