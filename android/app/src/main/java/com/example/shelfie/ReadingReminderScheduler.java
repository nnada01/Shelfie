package com.example.shelfie;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

// Registers or cancels a daily inexact alarm that fires ReminderReceiver based on ThemeManager

public final class ReadingReminderScheduler {

    private static final int REQUEST_CODE = 7101;

    private ReadingReminderScheduler() {
    }

    //if reminders are off, cancels the pending intent; if on, schedules the next 9:00 alarm and repeats daily
    public static void sync(Context context) {
        Context app = context.getApplicationContext();
        AlarmManager am = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(app, ReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
                app,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (!ThemeManager.isReadingRemindersEnabled(app)) {
            am.cancel(pi);
            return;
        }

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        am.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                cal.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pi
        );
    }
}
