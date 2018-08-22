package com.zoromatic.flashlight;

import android.content.Context;
import android.content.SharedPreferences;

class Preferences {
    static final String PREF_NAME = "com.zoromatic.flashlight.Preferences";
    public static final String PREF_SETTINGS = "settings_";
    static final String PREF_THEME = "theme_";
    static final String PREF_COLOR_SCHEME = "colorscheme_";
    static final String PREF_ORIENTATION = "orientation_";
    static final String PREF_KEEPACTIVE = "keepactive_";
    static final String PREF_TURNONONOPEN = "turnononopen_";
    static final String PREF_KEEPSTROBEFREQ = "keepstrobefreq_";
    private static final String PREF_STROBEFREQ = "strobefreq_";
    static final String PREF_LANGUAGE_OPTIONS = "languageoptions_";

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREF_NAME, 0);
    }

    public static String getTheme(Context context) {
        return getPreferences(context).getString(
                PREF_THEME + 0, "light"); // default: light
    }

    static int getColorScheme(Context context) {
        return getPreferences(context).getInt(
                PREF_COLOR_SCHEME + 0, 9); // default: cyan
    }

    static String getOrientation(Context context) {
        return getPreferences(context).getString(
                PREF_ORIENTATION + 0, "sens"); // default: sensor
    }

    static boolean getTurnOnOnOpen(Context context) {
        return getPreferences(context)
                .getBoolean(PREF_TURNONONOPEN + 0, false); // default: do not turn on
    }

    static boolean getKeepStrobeFrequency(Context context) {
        return getPreferences(context)
                .getBoolean(PREF_KEEPSTROBEFREQ + 0, false); // default: do not keep strobe frequency
    }

    static int getStrobeFrequency(Context context) {
        return getPreferences(context)
                .getInt(PREF_STROBEFREQ + 0, 0); // default: 0
    }

    static boolean getKeepActive(Context context) {
        return getPreferences(context)
                .getBoolean(PREF_KEEPACTIVE + 0, true); // default: keep active
    }

    static String getLanguageOptions(Context context) {
        return getPreferences(context)
                .getString(PREF_LANGUAGE_OPTIONS + 0, "");
    }

    static void setTheme(Context context, String value) {
        getPreferences(context).edit()
                .putString(PREF_THEME + 0, value).apply();
    }

    static void setColorScheme(Context context, int value) {
        getPreferences(context).edit()
                .putInt(PREF_COLOR_SCHEME + 0, value).apply();
    }

    static void setOrientation(Context context, String value) {
        getPreferences(context).edit()
                .putString(PREF_ORIENTATION + 0, value).apply();
    }

    static void setTurnOnOnOpen(Context context, boolean value) {
        getPreferences(context).edit()
                .putBoolean(PREF_TURNONONOPEN + 0, value).apply();
    }

    static void setKeepStrobeFrequency(Context context, boolean value) {
        getPreferences(context).edit()
                .putBoolean(PREF_KEEPSTROBEFREQ + 0, value).apply();
    }

    static void setStrobeFrequency(Context context, int value) {
        getPreferences(context).edit()
                .putInt(PREF_STROBEFREQ + 0, value).apply();
    }

    static void setKeepActive(Context context, boolean value) {
        getPreferences(context).edit()
                .putBoolean(PREF_KEEPACTIVE + 0, value).apply();
    }

    static void setLanguageOptions(Context context, String value) {
        getPreferences(context).edit()
                .putString(PREF_LANGUAGE_OPTIONS + 0, value).apply();
    }
}
