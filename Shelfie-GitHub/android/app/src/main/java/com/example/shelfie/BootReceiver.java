package com.example.shelfie;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

//after reboot, re-registers the reading reminder alarm if the user had it enabled
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            ReadingReminderScheduler.sync(context);
        }
    }
}