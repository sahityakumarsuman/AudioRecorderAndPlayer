package com.example.playerandrecorder;


import com.example.playerandrecorder.DatabaseSharePrefUtils.Prefs;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ColorMap {

    private static ColorMap singleton;

    public static ColorMap getInstance(Prefs prefs) {
        if (singleton == null) {
            singleton = new ColorMap(prefs);
        }
        return singleton;
    }

    private static final int THEME_BLUE = 3;
    private static final int THEME_GRAY = 9;

    private int appThemeResource = 0;
    private int primaryColorRes = R.color.md_blue_700;
    private int playbackPanelBackground = R.drawable.panel_amber;
    private int selected;
    private List<OnThemeColorChangeListener> onThemeColorChangeListeners;
    private Prefs prefs;

    private ColorMap(Prefs prefs) {

        onThemeColorChangeListeners = new ArrayList<>();
        this.prefs = prefs;
        if (prefs.isFirstRun()) {
            selected = THEME_GRAY;
        } else {
            selected = prefs.getThemeColor();
        }
        init(selected);
    }

    private void init(int color) {
        if (color < 1 || color > 9) {
            color = new Random().nextInt(9);
        }
        switch (color) {
            case THEME_BLUE:
                primaryColorRes = R.color.md_blue_700;
                appThemeResource = R.style.AppTheme;
                playbackPanelBackground = R.drawable.panel_amber;
                break;
            case THEME_GRAY:
            default:
                appThemeResource = R.style.AppTheme_Gray;
                primaryColorRes = R.color.md_blue_gray_700;
                playbackPanelBackground = R.drawable.panel_red;
        }
    }


    public int getAppThemeResource() {
        return appThemeResource;
    }

    public int getPrimaryColorRes() {
        return primaryColorRes;
    }

    public int getPlaybackPanelBackground() {
        return playbackPanelBackground;
    }


    public void addOnThemeColorChangeListener(OnThemeColorChangeListener onThemeColorChangeListener) {
        this.onThemeColorChangeListeners.add(onThemeColorChangeListener);
    }

    public void removeOnThemeColorChangeListener(OnThemeColorChangeListener onThemeColorChangeListener) {
        this.onThemeColorChangeListeners.remove(onThemeColorChangeListener);
    }

    public void onThemeColorChange(int pos) {
        for (int i = 0; i < onThemeColorChangeListeners.size(); i++) {
            onThemeColorChangeListeners.get(i).onThemeColorChange(pos);
        }
    }

    public interface OnThemeColorChangeListener {
        void onThemeColorChange(int pos);
    }
}
