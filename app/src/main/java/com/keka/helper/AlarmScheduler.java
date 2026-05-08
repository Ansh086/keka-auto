package com.keka.helper;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

final class AlarmScheduler {
    private AlarmScheduler() {
    }

    static void scheduleAll(Context context) {
        schedule(context, ClockAction.CLOCK_IN, AppPrefs.getClockIn(context));
        schedule(context, ClockAction.CLOCK_OUT, AppPrefs.getClockOut(context));
    }

    static void schedule(Context context, ClockAction action, LocalTime time) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        long triggerAtMillis = nextTriggerAtMillis(time);
        Intent launchIntent = new Intent(context, MainActivity.class);
        PendingIntent showIntent = PendingIntent.getActivity(
                context,
                action.ordinal() + 100,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        PendingIntent operation = AlarmReceiver.pendingIntent(context, action);
        alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent), operation);
    }

    private static long nextTriggerAtMillis(LocalTime time) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime candidate = now.withHour(time.getHour()).withMinute(time.getMinute()).withSecond(0).withNano(0);
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1);
        }
        return Date.from(candidate.atZone(ZoneId.systemDefault()).toInstant()).getTime();
    }
}
