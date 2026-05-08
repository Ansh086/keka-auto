package com.keka.helper;

import android.Manifest;
import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import java.time.LocalTime;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final String TAG = "KekaHelper";
    private TextView status;
    private TextView clockInValue;
    private TextView clockOutValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = findViewById(R.id.status);
        clockInValue = findViewById(R.id.clockInValue);
        clockOutValue = findViewById(R.id.clockOutValue);

        Button clockInButton = findViewById(R.id.clockInButton);
        Button clockOutButton = findViewById(R.id.clockOutButton);
        Button saveButton = findViewById(R.id.saveButton);
        Button immediateInButton = findViewById(R.id.immediateInButton);
        Button immediateOutButton = findViewById(R.id.immediateOutButton);
        Button accessibilityButton = findViewById(R.id.accessibilityButton);
        Button notificationButton = findViewById(R.id.notificationButton);
        Button alarmButton = findViewById(R.id.alarmButton);

        clockInButton.setOnClickListener(v -> pickTime(true));
        clockOutButton.setOnClickListener(v -> pickTime(false));
        saveButton.setOnClickListener(v -> {
            NotificationHelper.ensureChannel(this);
            requestNotificationPermissionIfNeeded();
            AlarmScheduler.scheduleAll(this);
            refreshUi();
            Toast.makeText(this, "Scheduled", Toast.LENGTH_SHORT).show();
        });
        immediateInButton.setOnClickListener(v -> triggerImmediate(ClockAction.CLOCK_IN));
        immediateOutButton.setOnClickListener(v -> triggerImmediate(ClockAction.CLOCK_OUT));
        accessibilityButton.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        notificationButton.setOnClickListener(v -> requestNotificationPermissionIfNeeded());
        alarmButton.setOnClickListener(v -> {
            android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
                return;
            }
            Toast.makeText(this, "Alarm scheduling is available", Toast.LENGTH_SHORT).show();
        });

        refreshUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUi();
    }

    private void refreshUi() {
        LocalTime in = AppPrefs.getClockIn(this);
        LocalTime out = AppPrefs.getClockOut(this);
        clockInValue.setText(String.format(Locale.US, "Clock in time: %02d:%02d", in.getHour(), in.getMinute()));
        clockOutValue.setText(String.format(Locale.US, "Clock out time: %02d:%02d", out.getHour(), out.getMinute()));
        status.setText("Accessibility needed: open settings and enable Keka Helper.");
    }

    private void pickTime(boolean isClockIn) {
        LocalTime current = isClockIn ? AppPrefs.getClockIn(this) : AppPrefs.getClockOut(this);
        new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    LocalTime chosen = LocalTime.of(hourOfDay, minute);
                    if (isClockIn) {
                        AppPrefs.setClockIn(this, chosen);
                    } else {
                        AppPrefs.setClockOut(this, chosen);
                    }
                    refreshUi();
                },
                current.getHour(),
                current.getMinute(),
                true
        ).show();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(this, "Notifications are already allowed on this Android version", Toast.LENGTH_SHORT).show();
            return;
        }
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Notification permission already granted", Toast.LENGTH_SHORT).show();
            return;
        }
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
    }

    private void triggerImmediate(ClockAction action) {
        Log.i(TAG, "Immediate trigger requested for " + action);
        NotificationHelper.ensureChannel(this);
        AppPrefs.setPendingAction(this, action);
        AutomationLauncher.launchKeka(this);
        refreshUi();
        Toast.makeText(
                this,
                action == ClockAction.CLOCK_OUT ? "Immediate clock out started" : "Immediate clock in started",
                Toast.LENGTH_SHORT
        ).show();
    }
}
