package com.nayanpote.edgeassist;

import android.content.Context;
import android.media.AudioManager;
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