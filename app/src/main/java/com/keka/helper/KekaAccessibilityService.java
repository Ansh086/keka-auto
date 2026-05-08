package com.keka.helper;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.graphics.Path;
import android.graphics.Rect;
import android.accessibilityservice.GestureDescription;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

public final class KekaAccessibilityService extends AccessibilityService {
    private static final String TAG = "KekaHelper";
    private static final String COMMENT_IN = "in";
    private static final String COMMENT_OUT = "out";
    private boolean launchRequested;

    private final BroadcastReceiver unlockReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
                tryProcessPending();
            }
        }
    };

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Log.i(TAG, "Accessibility service connected");
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                | AccessibilityEvent.TYPE_VIEW_CLICKED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                | AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        info.notificationTimeout = 0;
        info.packageNames = new String[]{AutomationLauncher.KEKA_PACKAGE};
        setServiceInfo(info);
        registerReceiver(unlockReceiver, new IntentFilter(Intent.ACTION_USER_PRESENT));
        tryProcessPending();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event != null && event.getPackageName() != null) {
            Log.i(TAG, "Accessibility event: " + event.getEventType() + " from " + event.getPackageName());
        }
        tryProcessPending();
    }

    @Override
    public void onInterrupt() {
        // No-op.
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(unlockReceiver);
        super.onDestroy();
    }

    private void tryProcessPending() {
        ClockAction action = AppPrefs.getPendingAction(this);
        if (action == null) {
            launchRequested = false;
            return;
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            Log.i(TAG, "No active window yet for pending " + action);
            return;
        }

        String pkg = String.valueOf(root.getPackageName());
        Log.i(TAG, "Pending " + action + " phase=" + AppPrefs.getPendingPhase(this) + " window=" + pkg);
        if (!AutomationLauncher.KEKA_PACKAGE.equals(pkg)) {
            if (!launchRequested) {
                launchRequested = true;
                AutomationLauncher.launchKeka(this);
            }
            return;
        }

        launchRequested = false;

        int phase = AppPrefs.getPendingPhase(this);
        if (phase == AppPrefs.PHASE_HOME) {
            if (clickHomeButton(root, action)) {
                Log.i(TAG, "Clicked home button for " + action);
                AppPrefs.setPendingPhase(this, AppPrefs.PHASE_CONFIRM);
            }
            return;
        }

        if (phase == AppPrefs.PHASE_CONFIRM) {
            if (maybeEnterComment(root, action) || action == ClockAction.CLOCK_OUT) {
                if (clickConfirmButton(root, action)) {
                    Log.i(TAG, "Clicked confirm button for " + action);
                    AppPrefs.clearPendingAction(this);
                    NotificationHelper.showDone(this, action);
                }
            } else if (clickConfirmButton(root, action)) {
                Log.i(TAG, "Clicked confirm button for " + action);
                AppPrefs.clearPendingAction(this);
                NotificationHelper.showDone(this, action);
            }
        }
    }

    private boolean clickHomeButton(AccessibilityNodeInfo root, ClockAction action) {
        String[] ids = action == ClockAction.CLOCK_OUT
                ? new String[]{"com.keka.xhr:id/btnClockOut", "com.keka.xhr:id/tvClockOut"}
                : new String[]{"com.keka.xhr:id/btnClockIn", "com.keka.xhr:id/tvClockIn"};
        AccessibilityNodeInfo node = firstByViewId(root, ids);
        if (node == null) {
            node = firstByText(root, action == ClockAction.CLOCK_OUT ? "Clock Out" : "Clock In");
        }
        if (clickNode(node, action == ClockAction.CLOCK_OUT ? "home clock out" : "home clock in")) {
            return true;
        }
        return tapAt(540, 834, action == ClockAction.CLOCK_OUT ? "home clock out fallback" : "home clock in fallback");
    }

    private boolean clickConfirmButton(AccessibilityNodeInfo root, ClockAction action) {
        String[] ids = action == ClockAction.CLOCK_OUT
                ? new String[]{"com.keka.xhr:id/btnClockOut", "com.keka.xhr:id/btnClockIn"}
                : new String[]{"com.keka.xhr:id/btnClockIn"};
        AccessibilityNodeInfo node = firstByViewId(root, ids);
        if (node == null) {
            node = firstByText(root, "Confirm");
        }
        if (clickNode(node, action == ClockAction.CLOCK_OUT ? "confirm clock out" : "confirm clock in")) {
            return true;
        }
        return tapAt(540, 834, action == ClockAction.CLOCK_OUT ? "confirm clock out fallback" : "confirm clock in fallback");
    }

    private boolean clickNode(AccessibilityNodeInfo node, String label) {
        if (node == null) {
            Log.w(TAG, "No node found for " + label);
            return false;
        }

        Log.i(TAG, "Click target for " + label + ": class=" + node.getClassName()
                + " text=" + node.getText()
                + " id=" + node.getViewIdResourceName()
                + " clickable=" + node.isClickable()
                + " enabled=" + node.isEnabled());

        if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return true;
        }

        AccessibilityNodeInfo clickableParent = node;
        while (clickableParent != null && !clickableParent.isClickable()) {
            clickableParent = clickableParent.getParent();
        }
        if (clickableParent != null && clickableParent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            Log.i(TAG, "Clicked parent for " + label);
            return true;
        }

        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        if (bounds.isEmpty()) {
            Log.w(TAG, "No bounds available for " + label);
            return false;
        }

        float centerX = bounds.centerX();
        float centerY = bounds.centerY();
        Path path = new Path();
        path.moveTo(centerX, centerY);
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0, 1);
        GestureDescription gesture = new GestureDescription.Builder().addStroke(stroke).build();
        boolean dispatched = dispatchGesture(gesture, null, null);
        Log.i(TAG, "Dispatched tap for " + label + " at " + centerX + "," + centerY + " result=" + dispatched);
        return dispatched;
    }

    private boolean tapAt(float centerX, float centerY, String label) {
        Path path = new Path();
        path.moveTo(centerX, centerY);
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0, 1);
        GestureDescription gesture = new GestureDescription.Builder().addStroke(stroke).build();
        boolean dispatched = dispatchGesture(gesture, null, null);
        Log.i(TAG, "Dispatched fallback tap for " + label + " at " + centerX + "," + centerY + " result=" + dispatched);
        return dispatched;
    }

    private boolean maybeEnterComment(AccessibilityNodeInfo root, ClockAction action) {
        if (action == ClockAction.CLOCK_OUT) {
            return true;
        }
        AccessibilityNodeInfo field = firstByViewId(root, new String[]{"com.keka.xhr:id/etComment"});
        if (field == null) {
            return false;
        }
        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, COMMENT_IN);
        return field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
    }

    private AccessibilityNodeInfo firstByViewId(AccessibilityNodeInfo root, String[] ids) {
        for (String id : ids) {
            List<AccessibilityNodeInfo> matches = root.findAccessibilityNodeInfosByViewId(id);
            if (matches != null && !matches.isEmpty()) {
                return matches.get(0);
            }
        }
        return null;
    }

    private AccessibilityNodeInfo firstByText(AccessibilityNodeInfo root, String text) {
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(text);
        if (nodes != null && !nodes.isEmpty()) {
            return nodes.get(0);
        }
        return null;
    }
}
