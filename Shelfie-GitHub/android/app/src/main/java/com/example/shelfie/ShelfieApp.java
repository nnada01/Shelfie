package com.example.shelfie;

import android.app.Application;

//Application entry: restores theme and syncs reading reminders before any activity starts

public class ShelfieApp extends Application {
    //Applies saved light/dark mode and schedules or cancels the daily reminder alarm
    @Override
    public void onCreate() {
        super.onCreate();
        ThemeManager.applySavedTheme(this);
        ReadingReminderScheduler.sync(this);
    }
}
