package com.nayanpote.edgeassist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    private static final String PREF_NAME = "EdgeAssistPrefs";
    private static final String KEY_SERVICE_ENABLED = "service_enabled";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
                Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction()) ||
                Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction())) {

            Log.d(TAG, "Boot completed or package replaced received");

            try {
                // Check if service was previously enabled
                SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                boolean serviceEnabled = prefs.getBoolean(KEY_SERVICE_ENABLED, false);

                if (serviceEnabled) {
                    Log.d(TAG, "Service was enabled, starting EdgeAssist service");

                    Intent serviceIntent = new Intent(context, OverlayService.class);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent);
                    } else {
                        context.startService(serviceIntent);
                    }

                    Log.d(TAG, "EdgeAssist service started successfully");
                } else {
                    Log.d(TAG, "Service was not enabled, skipping auto-start");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error starting service on boot", e);
            }
        }
    }
}