package com.keka.helper;

import android.content.Context;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

final class AutomationLauncher {
    static final String KEKA_PACKAGE = "com.keka.xhr";
    private static final String TAG = "KekaHelper";

    private AutomationLauncher() {
    }

    static void launchKeka(Context context) {
        PackageManager pm = context.getPackageManager();
        ComponentName[] candidates = new ComponentName[]{
                new ComponentName(KEKA_PACKAGE, KEKA_PACKAGE + ".MainActivity"),
                new ComponentName(KEKA_PACKAGE, KEKA_PACKAGE + ".HomeActivity")
        };

        for (ComponentName candidate : candidates) {
            Intent launch = new Intent(Intent.ACTION_MAIN);
            launch.addCategory(Intent.CATEGORY_LAUNCHER);
            launch.setComponent(candidate);
            launch.setPackage(KEKA_PACKAGE);
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            try {
                Log.i(TAG, "Launching Keka via explicit component " + candidate);
                context.startActivity(launch);
                return;
            } catch (ActivityNotFoundException e) {
                Log.w(TAG, "Keka launch component not found: " + candidate, e);
            } catch (RuntimeException e) {
                Log.w(TAG, "Keka launch failed for component " + candidate, e);
            }
        }

        Intent launch = pm.getLaunchIntentForPackage(KEKA_PACKAGE);
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            Log.i(TAG, "Launching Keka via package-manager fallback " + launch.getComponent());
            context.startActivity(launch);
            return;
        }

        Log.w(TAG, "No launch intent found for " + KEKA_PACKAGE + " and explicit components failed");
    }
}
