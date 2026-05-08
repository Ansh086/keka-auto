package com.keka.helper;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.KeyguardManager;

public final class AlarmReceiver extends BroadcastReceiver {
    static final String EXTRA_ACTION = "extra_action";

    static PendingIntent pendingIntent(Context context, ClockAction action) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(EXTRA_ACTION, action.toPrefValue());
        return PendingIntent.getBroadcast(
                context,
                action.ordinal(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ClockAction action = ClockAction.from(intent.getStringExtra(EXTRA_ACTION));
        AppPrefs.setPendingAction(context, action);
        NotificationHelper.showPrompt(context, action);

        if (action == ClockAction.CLOCK_OUT) {
            AlarmScheduler.schedule(context, action, AppPrefs.getClockOut(context));
        } else {
            AlarmScheduler.schedule(context, action, AppPrefs.getClockIn(context));
        }

        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        boolean locked = keyguardManager != null && keyguardManager.isKeyguardLocked();
        if (!locked) {
            AutomationLauncher.launchKeka(context);
        }
    }
}
