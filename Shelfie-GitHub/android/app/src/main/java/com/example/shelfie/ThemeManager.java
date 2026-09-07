package com.example.shelfie;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

//persists dark mode, journal biometric lock, and reading reminder toggles in app-wide preferences

public final class ThemeManager {
    private static final String PREFS_NAME = "shelfie_preferences";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";
    private static final String KEY_BIOMETRIC_LOCK = "biometric_lock_enabled";
    private static final String KEY_READING_REMINDERS = "reading_reminders_enabled";

    private ThemeManager() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void applySavedTheme(Context context) {
        boolean darkMode = prefs(context).getBoolean(KEY_DARK_MODE, false);
        AppCompatDelegate.setDefaultNightMode(
                darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    public static boolean isDarkModeEnabled(Context context) {
        return prefs(context).getBoolean(KEY_DARK_MODE, false);
    }

    public static void setDarkModeEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_DARK_MODE, enabled).apply();
        AppCompatDelegate.setDefaultNightMode(
                enabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    //when true, opening the journal requires biometric or device credential (default true)
    public static boolean isBiometricLockEnabled(Context context) {
        return prefs(context).getBoolean(KEY_BIOMETRIC_LOCK, true);
    }

    public static void setBiometricLockEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_BIOMETRIC_LOCK, enabled).apply();
    }

    public static boolean isReadingRemindersEnabled(Context context) {
        return prefs(context).getBoolean(KEY_READING_REMINDERS, false);
    }

    public static void setReadingRemindersEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_READING_REMINDERS, enabled).apply();
    }
}
