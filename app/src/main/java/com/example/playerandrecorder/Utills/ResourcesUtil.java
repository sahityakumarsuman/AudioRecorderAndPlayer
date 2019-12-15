package com.example.playerandrecorder.Utills;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.example.playerandrecorder.Application.PlayerRecorderApplication;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;



public class ResourcesUtil {
    private static Context context = PlayerRecorderApplication.getApplicationContextFromApp().getApplicationContext();
    private static Resources.Theme theme = PlayerRecorderApplication.getApplicationContextFromApp().getApplicationContext().getTheme();

    public static Drawable getDrawableById(int resId) {
        return SDK_INT >= LOLLIPOP ? context.getResources().getDrawable(resId, theme) :
                context.getResources().getDrawable(resId);
    }

    public static String getString(int resId) {
        return SDK_INT >= LOLLIPOP ? context.getResources().getString(resId) :
                context.getResources().getString(resId);
    }

    public static int getColor(int resId) {
        return SDK_INT >= LOLLIPOP ? context.getResources().getColor(resId) :
                context.getResources().getColor(resId);
    }


    public static ColorStateList getColorStateList(int resId) {
        return SDK_INT >= LOLLIPOP ? context.getResources().getColorStateList(resId) :
                context.getResources().getColorStateList(resId);
    }


    public static float getDimen(int resId) {
        return SDK_INT >= LOLLIPOP ? context.getResources().getDimension(resId) :
                context.getResources().getDimension(resId);
    }

    public static int dpToPx(Context c, float dipValue) {
        DisplayMetrics metrics = c.getResources().getDisplayMetrics();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
    }

    public static int spToPx(Context context, float spValue) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spValue, metrics);
    }

    public static int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }
}
