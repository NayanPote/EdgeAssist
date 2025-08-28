package com.nayanpote.edgeassist;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

public class HardwareController {
    private static final String TAG = "HardwareController";

    private Context context;
    private AudioManager audioManager;

    public HardwareController(Context context) {
        this.context = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public void openControlPanel() {
        try {
            // Method 1: Try to expand notification panel (requires root/system permissions)
            try {
                Runtime.getRuntime().exec("cmd statusbar expand-notifications");
                showToast("Opening Notification Panel");
                Log.d(TAG, "Notification panel command sent");
                return;
            } catch (Exception e) {
                Log.d(TAG, "Cannot expand notification panel - no system permissions");
            }

            // Method 2: Try accessibility service approach (if available)
            try {
                // This sends a "swipe down" gesture from top of screen
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                showToast("Please enable accessibility for notification panel access");
                return;
            } catch (Exception e) {
                Log.d(TAG, "Accessibility settings not available");
            }

            // Method 3: Open Quick Settings Panel (Android Q+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    Intent panelIntent = new Intent("android.settings.panel.action.INTERNET_CONNECTIVITY");
                    panelIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(panelIntent);
                    Log.d(TAG, "Quick settings panel opened");
                    showToast("Opening Quick Settings Panel");
                    return;
                } catch (Exception e) {
                    Log.d(TAG, "Quick settings panel not available");
                }
            }

            // Fallback: Show user how to access notification panel
            showToast("Swipe down from top of screen for notification panel");
            Log.d(TAG, "Showed instruction for manual notification panel access");

        } catch (Exception e) {
            Log.e(TAG, "Error accessing notification panel", e);
            showToast("Cannot access notification panel");
        }
    }

    public void openVolumeControl() {
        try {
            if (audioManager != null) {
                // Method 1: Show volume slider by simulating a volume adjustment with UI
                audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_SAME, // Don't change volume, just show UI
                        AudioManager.FLAG_SHOW_UI | AudioManager.FLAG_VIBRATE);
                Log.d(TAG, "Volume slider popup shown");
                showToast("Volume Control");
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing volume popup with ADJUST_SAME", e);
        }

        try {
            // Method 2: Alternative - slightly adjust volume to trigger UI
            if (audioManager != null) {
                int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

                // Adjust volume by 0 (or minimal amount) to show UI
                if (currentVolume < maxVolume) {
                    audioManager.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_RAISE,
                            AudioManager.FLAG_SHOW_UI);
                    // Immediately revert back
                    audioManager.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_LOWER,
                            0); // No UI flag for revert
                } else {
                    audioManager.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_LOWER,
                            AudioManager.FLAG_SHOW_UI);
                    // Immediately revert back
                    audioManager.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_RAISE,
                            0); // No UI flag for revert
                }
                Log.d(TAG, "Volume slider shown via adjustment");
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing volume popup with adjustment method", e);
        }

        // Fallback: Open volume settings if popup methods fail
        try {
            Intent intent = new Intent(Settings.ACTION_SOUND_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Log.d(TAG, "Volume settings opened as fallback");
            showToast("Opening Volume Settings");
        } catch (Exception e) {
            Log.e(TAG, "Error opening volume settings fallback", e);
            showToast("Failed to open Volume Control");
        }
    }

    public void volumeUp() {
        if (audioManager != null) {
            try {
                audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_SHOW_UI);
                Log.d(TAG, "Volume up executed");
            } catch (Exception e) {
                Log.e(TAG, "Error adjusting volume up", e);
                showToast("Failed to adjust volume");
            }
        }
    }

    public void volumeDown() {
        if (audioManager != null) {
            try {
                audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_SHOW_UI);
                Log.d(TAG, "Volume down executed");
            } catch (Exception e) {
                Log.e(TAG, "Error adjusting volume down", e);
                showToast("Failed to adjust volume");
            }
        }
    }

    public void muteVolume() {
        if (audioManager != null) {
            try {
                audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_MUTE,
                        AudioManager.FLAG_SHOW_UI);
                showToast("Volume Muted");
            } catch (Exception e) {
                Log.e(TAG, "Error muting volume", e);
                showToast("Failed to mute volume");
            }
        }
    }

    public void unmuteVolume() {
        if (audioManager != null) {
            try {
                audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_UNMUTE,
                        AudioManager.FLAG_SHOW_UI);
                showToast("Volume Unmuted");
            } catch (Exception e) {
                Log.e(TAG, "Error unmuting volume", e);
                showToast("Failed to unmute volume");
            }
        }
    }

    public int getCurrentVolume() {
        if (audioManager != null) {
            return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }
        return 0;
    }

    public int getMaxVolume() {
        if (audioManager != null) {
            return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }
        return 0;
    }

    public boolean isVolumeFixed() {
        if (audioManager != null) {
            return audioManager.isVolumeFixed();
        }
        return false;
    }

    private void showToast(String message) {
        try {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing toast", e);
        }
    }

    public void cleanup() {
        // Cleanup resources if needed
        audioManager = null;
        context = null;
    }
}