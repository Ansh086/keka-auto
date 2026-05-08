package com.keka.helper;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

final class AppPrefs {
    private static final String FILE = "keka_helper";
    private static final String KEY_IN = "clock_in";
    private static final String KEY_OUT = "clock_out";
    private static final String KEY_PENDING = "pending_action";
    private static final String KEY_PHASE = "pending_phase";

    static final int PHASE_NONE = 0;
    static final int PHASE_HOME = 1;
    static final int PHASE_CONFIRM = 2;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private AppPrefs() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static LocalTime getClockIn(Context context) {
        return readTime(context, KEY_IN, LocalTime.of(9, 0));
    }

    static LocalTime getClockOut(Context context) {
        return readTime(context, KEY_OUT, LocalTime.of(18, 0));
    }

    static void setClockIn(Context context, LocalTime time) {
        prefs(context).edit().putString(KEY_IN, FORMATTER.format(time)).apply();
    }

    static void setClockOut(Context context, LocalTime time) {
        prefs(context).edit().putString(KEY_OUT, FORMATTER.format(time)).apply();
    }

    static void setPendingAction(Context context, ClockAction action) {
        prefs(context).edit()
                .putString(KEY_PENDING, action.toPrefValue())
                .putInt(KEY_PHASE, PHASE_HOME)
                .apply();
    }

    static ClockAction getPendingAction(Context context) {
        String raw = prefs(context).getString(KEY_PENDING, null);
        if (raw == null) {
            return null;
        }
        return ClockAction.from(raw);
    }

    static int getPendingPhase(Context context) {
        return prefs(context).getInt(KEY_PHASE, PHASE_NONE);
    }

    static void setPendingPhase(Context context, int phase) {
        prefs(context).edit().putInt(KEY_PHASE, phase).apply();
    }

    static void clearPendingAction(Context context) {
        prefs(context).edit().remove(KEY_PENDING).putInt(KEY_PHASE, PHASE_NONE).apply();
    }

    static String summary(Context context) {
        return "In " + FORMATTER.format(getClockIn(context)) + " | Out " + FORMATTER.format(getClockOut(context));
    }

    private static LocalTime readTime(Context context, String key, LocalTime fallback) {
        String raw = prefs(context).getString(key, null);
        if (raw == null) {
            return fallback;
        }
        try {
            return LocalTime.parse(raw, FORMATTER);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
