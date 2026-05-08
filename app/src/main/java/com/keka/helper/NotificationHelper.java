package com.keka.helper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

final class NotificationHelper {
    private static final String CHANNEL_ID = "keka_clock";
    private static final int ID_PROMPT = 1001;
    private static final int ID_DONE = 1002;

    private NotificationHelper() {
    }

    static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Keka clock actions",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Prompts for Keka clock-in and clock-out actions");
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        manager.createNotificationChannel(channel);
    }

    static void showPrompt(Context context, ClockAction action) {
        ensureChannel(context);
        Intent open = new Intent(context, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                2000 + action.ordinal(),
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        String title = action == ClockAction.CLOCK_OUT ? "Clock out is due" : "Clock in is due";
        String body = "Unlock the phone and Keka will open automatically.";
        Notification notification = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setPriority(Notification.PRIORITY_HIGH)
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .build();
        notify(context, ID_PROMPT + action.ordinal(), notification);
    }

    static void showDone(Context context, ClockAction action) {
        ensureChannel(context);
        String title = action == ClockAction.CLOCK_OUT ? "Clock out complete" : "Clock in complete";
        Notification notification = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.checkbox_on_background)
                .setContentTitle(title)
                .setContentText("Keka was opened and the buttons were tapped.")
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build();
        notify(context, ID_DONE + action.ordinal(), notification);
    }

    private static void notify(Context context, int id, Notification notification) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(id, notification);
        }
    }
}
