package com.nayanpote.edgeassist;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SpeedDialManager {
    private static final String TAG = "SpeedDialManager";
    private static final String PREF_NAME = "EdgeAssistPrefs";
    private static final String KEY_SPEED_DIAL_CONTACTS = "speed_dial_contacts";

    private Context context;
    private WindowManager windowManager;
    private SharedPreferences prefs;
    private AnimationHelper animationHelper;

    private View speedDialView;
    private WindowManager.LayoutParams speedDialParams;
    private LinearLayout contactsContainer;
    private boolean isVisible = false;

    private List<MainActivity.SpeedDialContact> speedDialContacts = new ArrayList<>();

    public SpeedDialManager(Context context, WindowManager windowManager, AnimationHelper animationHelper) {
        this.context = context;
        this.windowManager = windowManager;
        this.animationHelper = animationHelper;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadSpeedDialContacts();
    }

    public void showSpeedDial() {
        if (isVisible) return;

        // Reload contacts to get latest data
        loadSpeedDialContacts();

        if (speedDialContacts.isEmpty()) {
            Toast.makeText(context, "No speed dial contacts saved", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            createSpeedDialView();
            isVisible = true;
            animationHelper.animateFadeIn(speedDialView, 300);
        } catch (Exception e) {
            Log.e(TAG, "Error showing speed dial", e);
            Toast.makeText(context, "Error showing speed dial", Toast.LENGTH_SHORT).show();
        }
    }

    public void hideSpeedDial() {
        if (!isVisible || speedDialView == null) return;

        isVisible = false;
        animationHelper.animateFadeOut(speedDialView, 300, () -> {
            try {
                if (speedDialView != null && windowManager != null) {
                    windowManager.removeView(speedDialView);
                    speedDialView = null;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error hiding speed dial", e);
            }
        });
    }

    private void createSpeedDialView() {
        if (speedDialView != null) {
            // Remove existing view first
            try {
                windowManager.removeView(speedDialView);
            } catch (Exception e) {
                // Ignore if view wasn't added
            }
            speedDialView = null;
        }

        // Create a simple LinearLayout instead of inflating complex layout
        speedDialView = createSimpleSpeedDialLayout();

        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        speedDialParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        speedDialParams.gravity = Gravity.CENTER;

        speedDialView.setAlpha(0f);
        windowManager.addView(speedDialView, speedDialParams);
    }

    private View createSimpleSpeedDialLayout() {
        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setGravity(Gravity.CENTER);
        mainLayout.setBackgroundColor(Color.parseColor("#CC000000")); // Semi-transparent black
        mainLayout.setPadding(40, 40, 40, 40);

        // Title
        TextView titleText = new TextView(context);
        titleText.setText("Speed Dial Contacts");
        titleText.setTextColor(Color.WHITE);
        titleText.setTextSize(20);
        titleText.setGravity(Gravity.CENTER);
        titleText.setPadding(0, 0, 0, 30);
        mainLayout.addView(titleText);

        // Contacts container
        contactsContainer = new LinearLayout(context);
        contactsContainer.setOrientation(LinearLayout.VERTICAL);
        contactsContainer.setGravity(Gravity.CENTER);
        populateContacts(contactsContainer);
        mainLayout.addView(contactsContainer);

        // Close button
        Button closeButton = new Button(context);
        closeButton.setText("Close");
        closeButton.setBackgroundColor(Color.parseColor("#666666"));
        closeButton.setTextColor(Color.WHITE);
        closeButton.setPadding(40, 20, 40, 20);
        closeButton.setOnClickListener(v -> hideSpeedDial());

        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        closeParams.topMargin = 40;
        closeButton.setLayoutParams(closeParams);
        mainLayout.addView(closeButton);

        return mainLayout;
    }

    private void populateContacts(LinearLayout container) {
        container.removeAllViews();

        if (speedDialContacts.isEmpty()) {
            TextView noContactsText = new TextView(context);
            noContactsText.setText("No speed dial contacts saved");
            noContactsText.setTextColor(Color.WHITE);
            noContactsText.setTextSize(16);
            noContactsText.setGravity(Gravity.CENTER);
            noContactsText.setPadding(20, 20, 20, 20);
            container.addView(noContactsText);
            return;
        }

        for (MainActivity.SpeedDialContact contact : speedDialContacts) {
            LinearLayout contactLayout = new LinearLayout(context);
            contactLayout.setOrientation(LinearLayout.HORIZONTAL);
            contactLayout.setGravity(Gravity.CENTER_VERTICAL);
            contactLayout.setPadding(20, 15, 20, 15);
            contactLayout.setBackgroundColor(Color.parseColor("#33FFFFFF")); // Semi-transparent white

            LinearLayout.LayoutParams contactParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            contactParams.bottomMargin = 10;
            contactLayout.setLayoutParams(contactParams);

            // Contact info layout
            LinearLayout infoLayout = new LinearLayout(context);
            infoLayout.setOrientation(LinearLayout.VERTICAL);
            infoLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView nameText = new TextView(context);
            nameText.setText(contact.name);
            nameText.setTextColor(Color.WHITE);
            nameText.setTextSize(16);
            nameText.setTypeface(nameText.getTypeface(), android.graphics.Typeface.BOLD);

            TextView numberText = new TextView(context);
            numberText.setText(contact.phoneNumber);
            numberText.setTextColor(Color.LTGRAY);
            numberText.setTextSize(14);

            infoLayout.addView(nameText);
            infoLayout.addView(numberText);

            // Call button
            Button callButton = new Button(context);
            callButton.setText("Call");
            callButton.setBackgroundColor(Color.parseColor("#4CAF50"));
            callButton.setTextColor(Color.WHITE);
            callButton.setPadding(30, 15, 30, 15);
            callButton.setOnClickListener(v -> {
                makePhoneCall(contact.phoneNumber);
                hideSpeedDial();
            });

            contactLayout.addView(infoLayout);
            contactLayout.addView(callButton);
            container.addView(contactLayout);
        }
    }

    private void makePhoneCall(String phoneNumber) {
        try {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + phoneNumber));
            callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(callIntent);
            Toast.makeText(context, "Calling " + phoneNumber, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error making phone call", e);
            Toast.makeText(context, "Cannot make call", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadSpeedDialContacts() {
        speedDialContacts.clear();
        String contactsJson = prefs.getString(KEY_SPEED_DIAL_CONTACTS, "[]");
        try {
            JSONArray jsonArray = new JSONArray(contactsJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String name = jsonObject.getString("name");
                String number = jsonObject.getString("number");
                speedDialContacts.add(new MainActivity.SpeedDialContact(name, number));
            }
            Log.d(TAG, "Loaded " + speedDialContacts.size() + " contacts");
        } catch (JSONException e) {
            Log.e(TAG, "Error loading contacts", e);
        }
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void cleanup() {
        hideSpeedDial();
    }
}