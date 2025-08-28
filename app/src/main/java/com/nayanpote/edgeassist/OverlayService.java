package com.nayanpote.edgeassist;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.core.app.NotificationCompat;

public class OverlayService extends Service {

    private static final String TAG = "OverlayService";
    private static final String CHANNEL_ID = "EdgeAssistChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final long HIDE_DELAY = 4000; // 4 seconds

    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams floatingParams;
    private Vibrator vibrator;
    private AudioManager audioManager;
    private GestureDetector gestureDetector;
    private Handler hideHandler;
    private Runnable hideRunnable;

    // Helper classes
    private AnimationHelper animationHelper;
    private SpeedDialManager speedDialManager;
    private HardwareController hardwareController;

    private boolean isDragging = false;
    private boolean isVisible = true;
    private boolean isLongPressing = false; // New flag to prevent other actions during long press
    private float initialX, initialY;
    private float initialTouchX, initialTouchY;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        hideHandler = new Handler();

        // Initialize helper classes
        animationHelper = new AnimationHelper(this, windowManager);
        speedDialManager = new SpeedDialManager(this, windowManager, animationHelper);
        hardwareController = new HardwareController(this);

        gestureDetector = new GestureDetector(this, new GestureListener());
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        startForeground(NOTIFICATION_ID, createNotification());
        createFloatingView();
        return START_STICKY;
    }

    private void createFloatingView() {
        if (floatingView != null) return;

        try {
            LayoutInflater inflater = LayoutInflater.from(this);
            floatingView = inflater.inflate(R.layout.floating_control, null);

            int layoutFlag;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
            }

            floatingParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    layoutFlag,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);

            floatingParams.gravity = Gravity.TOP | Gravity.START;
            floatingParams.x = 0;
            floatingParams.y = 200;

            floatingView.setOnTouchListener(new FloatingTouchListener());
            windowManager.addView(floatingView, floatingParams);

            // Start hide timer
            scheduleHide();

            Log.d(TAG, "Floating view created successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error creating floating view", e);
            stopSelf();
        }
    }

    private class FloatingTouchListener implements View.OnTouchListener {
        private long lastTouchTime = 0;
        private int tapCount = 0;
        private static final long DOUBLE_TAP_TIMEOUT = 400;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            // Always pass touch events to gesture detector
            gestureDetector.onTouchEvent(event);

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isDragging = false;
                    isLongPressing = false; // Reset long press flag
                    initialX = floatingParams.x;
                    initialY = floatingParams.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();

                    // Show view if hidden
                    if (!isVisible) {
                        showView();
                    }

                    // Cancel hide timer
                    cancelHide();
                    animationHelper.animatePress(floatingView, true);
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float deltaX = event.getRawX() - initialTouchX;
                    float deltaY = event.getRawY() - initialTouchY;

                    if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                        isDragging = true;
                        isLongPressing = false; // Cancel long press if dragging
                        floatingParams.x = (int) (initialX + deltaX);
                        floatingParams.y = (int) (initialY + deltaY);

                        try {
                            windowManager.updateViewLayout(floatingView, floatingParams);
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating view layout", e);
                        }
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    animationHelper.animatePress(floatingView, false);

                    if (isDragging) {
                        animationHelper.snapToEdge(floatingView, floatingParams);
                        isDragging = false;
                    } else if (!isLongPressing) {
                        // Only handle tap if it's not a long press
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastTouchTime < DOUBLE_TAP_TIMEOUT) {
                            tapCount++;
                        } else {
                            tapCount = 1;
                        }
                        lastTouchTime = currentTime;

                        // Use handler to detect single vs double tap
                        hideHandler.postDelayed(() -> {
                            if (tapCount == 1 && !isLongPressing) {
                                // Single tap - Open Control Panel
                                Log.d(TAG, "Single tap - Opening Control Panel");
                                hardwareController.openControlPanel();
                                vibrateFeedback();
                            } else if (tapCount == 2 && !isLongPressing) {
                                // Double tap - Open Volume Control
                                Log.d(TAG, "Double tap - Opening Volume Control");
                                hardwareController.openVolumeControl();
                                vibrateFeedback();
                            }
                            tapCount = 0;
                        }, DOUBLE_TAP_TIMEOUT);
                    }

                    // Reset long press flag
                    isLongPressing = false;

                    // Schedule hide after action
                    scheduleHide();
                    return true;
            }
            return false;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public void onLongPress(MotionEvent e) {
            if (!isDragging) {
                // Set flag to prevent other actions
                isLongPressing = true;

                // Long press - Speed dial
                Log.d(TAG, "Long press detected - Opening speed dial");
                speedDialManager.showSpeedDial();
                vibrateLongFeedback();
            }
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    }

    private void scheduleHide() {
        cancelHide();
        hideRunnable = this::hideView;
        hideHandler.postDelayed(hideRunnable, HIDE_DELAY);
    }

    private void cancelHide() {
        if (hideRunnable != null) {
            hideHandler.removeCallbacks(hideRunnable);
        }
    }

    private void hideView() {
        if (!isVisible) return;

        isVisible = false;
        if (floatingView != null) {
            animationHelper.hideToEdge(floatingView, floatingParams, null);
        }
    }

    private void showView() {
        if (isVisible) return;

        isVisible = true;
        if (floatingView != null) {
            animationHelper.showFromEdge(floatingView, floatingParams);
        }
    }

    private void vibrateFeedback() {
        if (vibrator != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(50);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error with vibration", e);
            }
        }
    }

    private void vibrateLongFeedback() {
        if (vibrator != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(200);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error with vibration", e);
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Edge Assist Service",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Floating controls service");
            channel.setShowBadge(false);
            channel.setSound(null, null);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Edge Assist")
                .setContentText("Floating controls are active")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSound(null)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");

        cancelHide();

        // Cleanup helper classes
        if (speedDialManager != null) {
            speedDialManager.cleanup();
        }

        if (hardwareController != null) {
            hardwareController.cleanup();
        }

        try {
            if (floatingView != null && windowManager != null) {
                windowManager.removeView(floatingView);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error removing floating view", e);
        } finally {
            floatingView = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}