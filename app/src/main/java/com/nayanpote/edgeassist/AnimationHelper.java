package com.nayanpote.edgeassist;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;

public class AnimationHelper {
    private static final String TAG = "AnimationHelper";

    private Context context;
    private WindowManager windowManager;

    public AnimationHelper(Context context, WindowManager windowManager) {
        this.context = context;
        this.windowManager = windowManager;
    }

    public void animateViewToPosition(View view, WindowManager.LayoutParams params,
                                      int targetX, int targetY, long duration,
                                      Animator.AnimatorListener listener) {
        if (view == null || params == null) return;

        ValueAnimator animatorX = ValueAnimator.ofInt(params.x, targetX);
        ValueAnimator animatorY = ValueAnimator.ofInt(params.y, targetY);

        animatorX.addUpdateListener(animation -> {
            params.x = (int) animation.getAnimatedValue();
            try {
                windowManager.updateViewLayout(view, params);
            } catch (Exception e) {
                // Ignore layout update errors
            }
        });

        animatorY.addUpdateListener(animation -> {
            params.y = (int) animation.getAnimatedValue();
            try {
                windowManager.updateViewLayout(view, params);
            } catch (Exception e) {
                // Ignore layout update errors
            }
        });

        animatorX.setDuration(duration);
        animatorY.setDuration(duration);
        animatorX.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorY.setInterpolator(new AccelerateDecelerateInterpolator());

        if (listener != null) {
            animatorX.addListener(listener);
        }

        animatorX.start();
        animatorY.start();
    }

    public void animateFadeIn(View view, long duration) {
        if (view == null) return;

        view.setAlpha(0f);
        view.animate()
                .alpha(1f)
                .setDuration(duration)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    public void animateFadeOut(View view, long duration, Runnable onComplete) {
        if (view == null) return;

        view.animate()
                .alpha(0f)
                .setDuration(duration)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    }
                })
                .start();
    }

    public void animateScale(View view, float fromScale, float toScale, long duration) {
        if (view == null) return;

        view.setScaleX(fromScale);
        view.setScaleY(fromScale);

        view.animate()
                .scaleX(toScale)
                .scaleY(toScale)
                .setDuration(duration)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    public void animatePress(View view, boolean pressed) {
        if (view == null) return;

        float scale = pressed ? 0.85f : 1.0f;
        view.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(100)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    public void snapToEdge(View view, WindowManager.LayoutParams params) {
        if (view == null || params == null) return;

        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = context.getResources().getDisplayMetrics().heightPixels;

        boolean snapToLeft = params.x < screenWidth / 2;
        int targetX = snapToLeft ? 0 : screenWidth - view.getWidth();
        int targetY = Math.max(0, Math.min(params.y, screenHeight - view.getHeight()));

        animateViewToPosition(view, params, targetX, targetY, 300, null);
    }

    public void hideToEdge(View view, WindowManager.LayoutParams params, Runnable onComplete) {
        if (view == null || params == null) return;

        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        boolean isOnLeft = params.x < screenWidth / 2;
        int hideX = isOnLeft ? -view.getWidth() / 2 : screenWidth - view.getWidth() / 2;

        animateViewToPosition(view, params, hideX, params.y, 400,
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    }
                });

        // Make semi-transparent
        view.animate()
                .alpha(0.3f)
                .setDuration(400)
                .start();
    }

    public void showFromEdge(View view, WindowManager.LayoutParams params) {
        if (view == null || params == null) return;

        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        boolean wasOnLeft = params.x < screenWidth / 2;
        int showX = wasOnLeft ? 0 : screenWidth - view.getWidth();

        animateViewToPosition(view, params, showX, params.y, 400, null);

        // Fade back to full opacity
        view.animate()
                .alpha(1.0f)
                .setDuration(400)
                .start();
    }
}