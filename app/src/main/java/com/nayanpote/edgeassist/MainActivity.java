package com.nayanpote.edgeassist;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String PREF_NAME = "EdgeAssistPrefs";
    private static final String KEY_SERVICE_ENABLED = "service_enabled";
    private static final String KEY_SPEED_DIAL_CONTACTS = "speed_dial_contacts";

    private SwitchMaterial serviceToggle;
    private Button permissionBtn, selectContactBtn, saveContactBtn, manageContactsBtn;
    private EditText phoneNumberEdit, contactNameEdit;
    private TextView statusText, savedContactsText;
    private MaterialCardView statusCard;
    private SharedPreferences prefs;
    private LinearLayout contactsListLayout;

    private ActivityResultLauncher<String[]> multiplePermissionsLauncher;
    private ActivityResultLauncher<Intent> overlayPermissionLauncher;
    private ActivityResultLauncher<Intent> writeSettingsLauncher;
    private ActivityResultLauncher<Intent> contactPickerLauncher;

    private List<SpeedDialContact> speedDialContacts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initPreferences();
        initPermissionLaunchers();
        loadSpeedDialContacts();
        updateUI();
    }

    private void initViews() {
        serviceToggle = findViewById(R.id.serviceToggle);
        permissionBtn = findViewById(R.id.permissionBtn);
        selectContactBtn = findViewById(R.id.selectContactBtn);
        saveContactBtn = findViewById(R.id.saveContactBtn);
        manageContactsBtn = findViewById(R.id.manageContactsBtn);
        phoneNumberEdit = findViewById(R.id.phoneNumberEdit);
        contactNameEdit = findViewById(R.id.contactNameEdit);
        statusText = findViewById(R.id.statusText);
        statusCard = findViewById(R.id.statusCard);
        savedContactsText = findViewById(R.id.savedContactsText);
        contactsListLayout = findViewById(R.id.contactsListLayout);

        serviceToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (hasAllPermissions()) {
                    startOverlayService();
                } else {
                    serviceToggle.setChecked(false);
                    requestPermissions();
                }
            } else {
                stopOverlayService();
            }
        });

        permissionBtn.setOnClickListener(v -> requestPermissions());
        selectContactBtn.setOnClickListener(v -> openContactPicker());
        saveContactBtn.setOnClickListener(v -> saveSpeedDialContact());
        manageContactsBtn.setOnClickListener(v -> showManageContactsDialog());
    }

    private void initPreferences() {
        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
    }

    private void saveSpeedDialContact() {
        String number = phoneNumberEdit.getText().toString().trim();
        String name = contactNameEdit.getText().toString().trim();

        if (number.isEmpty()) {
            Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (name.isEmpty()) {
            name = number; // Use number as name if name is empty
        }

        // Check if contact already exists
        for (SpeedDialContact contact : speedDialContacts) {
            if (contact.phoneNumber.equals(number)) {
                Toast.makeText(this, "This number already exists in speed dial", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        SpeedDialContact newContact = new SpeedDialContact(name, number);
        speedDialContacts.add(newContact);
        saveSpeedDialContactsToPrefs();
        updateContactsDisplay();

        // Clear input fields
        phoneNumberEdit.setText("");
        contactNameEdit.setText("");

        Toast.makeText(this, "Contact saved to speed dial", Toast.LENGTH_SHORT).show();
    }

    public void loadSpeedDialContacts() {
        speedDialContacts.clear();
        String contactsJson = prefs.getString(KEY_SPEED_DIAL_CONTACTS, "[]");
        try {
            JSONArray jsonArray = new JSONArray(contactsJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String name = jsonObject.getString("name");
                String number = jsonObject.getString("number");
                speedDialContacts.add(new SpeedDialContact(name, number));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        updateContactsDisplay();
    }

    private void saveSpeedDialContactsToPrefs() {
        JSONArray jsonArray = new JSONArray();
        for (SpeedDialContact contact : speedDialContacts) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", contact.name);
                jsonObject.put("number", contact.phoneNumber);
                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        prefs.edit().putString(KEY_SPEED_DIAL_CONTACTS, jsonArray.toString()).apply();
    }

    public List<SpeedDialContact> getSpeedDialContacts() {
        return new ArrayList<>(speedDialContacts);
    }

    private void updateContactsDisplay() {
        contactsListLayout.removeAllViews();

        if (speedDialContacts.isEmpty()) {
            savedContactsText.setText("No speed dial contacts saved");
            savedContactsText.setVisibility(View.VISIBLE);
        } else {
            savedContactsText.setText("Saved Contacts (" + speedDialContacts.size() + ")");
            savedContactsText.setVisibility(View.VISIBLE);

            for (SpeedDialContact contact : speedDialContacts) {
                View contactView = getLayoutInflater().inflate(R.layout.contact_item, contactsListLayout, false);
                TextView nameText = contactView.findViewById(R.id.contactName);
                TextView numberText = contactView.findViewById(R.id.contactNumber);
                Button deleteBtn = contactView.findViewById(R.id.deleteBtn);

                nameText.setText(contact.name);
                numberText.setText(contact.phoneNumber);

                deleteBtn.setOnClickListener(v -> {
                    speedDialContacts.remove(contact);
                    saveSpeedDialContactsToPrefs();
                    updateContactsDisplay();
                    Toast.makeText(MainActivity.this, "Contact removed", Toast.LENGTH_SHORT).show();
                });

                contactsListLayout.addView(contactView);
            }
        }
    }

    private void showManageContactsDialog() {
        if (speedDialContacts.isEmpty()) {
            Toast.makeText(this, "No contacts saved yet", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Manage Speed Dial Contacts");

        String[] contactNames = new String[speedDialContacts.size()];
        for (int i = 0; i < speedDialContacts.size(); i++) {
            contactNames[i] = speedDialContacts.get(i).name + " (" + speedDialContacts.get(i).phoneNumber + ")";
        }

        builder.setItems(contactNames, (dialog, which) -> {
            SpeedDialContact selectedContact = speedDialContacts.get(which);
            showContactOptionsDialog(selectedContact);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showContactOptionsDialog(SpeedDialContact contact) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(contact.name);
        builder.setMessage("Phone: " + contact.phoneNumber);

        builder.setPositiveButton("Call", (dialog, which) -> {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + contact.phoneNumber));
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                startActivity(callIntent);
            } else {
                Toast.makeText(this, "Call permission required", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Delete", (dialog, which) -> {
            speedDialContacts.remove(contact);
            saveSpeedDialContactsToPrefs();
            updateContactsDisplay();
            Toast.makeText(MainActivity.this, "Contact deleted", Toast.LENGTH_SHORT).show();
        });

        builder.setNeutralButton("Cancel", null);
        builder.show();
    }

    private void openContactPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        contactPickerLauncher.launch(intent);
    }

    private void initPermissionLaunchers() {
        multiplePermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean allGranted = true;
                    for (Boolean granted : result.values()) {
                        if (!granted) {
                            allGranted = false;
                            break;
                        }
                    }

                    if (allGranted) {
                        checkSpecialPermissions();
                    } else {
                        showToast("All permissions are required for the app to work");
                    }
                    updateUI();
                });

        overlayPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Settings.canDrawOverlays(this)) {
                        checkWriteSettingsPermission();
                    } else {
                        showToast("Overlay permission is required");
                    }
                    updateUI();
                });

        writeSettingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Settings.System.canWrite(this)) {
                        showToast("All permissions granted!");
                    } else {
                        showToast("Write settings permission is required");
                    }
                    updateUI();
                });

        contactPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        handleContactSelection(result.getData());
                    }
                });
    }

    private void handleContactSelection(Intent data) {
        try {
            Uri contactUri = data.getData();
            String[] projection = {
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            };

            Cursor cursor = getContentResolver().query(
                    contactUri, projection, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int numberColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                int nameColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);

                String phoneNumber = cursor.getString(numberColumnIndex);
                String contactName = cursor.getString(nameColumnIndex);

                phoneNumberEdit.setText(phoneNumber);
                contactNameEdit.setText(contactName != null ? contactName : "");

                cursor.close();
                showToast("Contact selected");
            }
        } catch (Exception e) {
            showToast("Error selecting contact");
        }
    }

    private void requestPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this)) {
            requestWriteSettingsPermission();
        } else {
            checkBasicPermissions();
        }
    }

    private void checkBasicPermissions() {
        String[] permissions = {
                Manifest.permission.VIBRATE,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_CONTACTS
        };

        boolean needsRequest = false;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                needsRequest = true;
                break;
            }
        }

        if (needsRequest) {
            multiplePermissionsLauncher.launch(permissions);
        } else {
            checkSpecialPermissions();
        }
    }

    private void checkSpecialPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this)) {
            requestWriteSettingsPermission();
        }
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        overlayPermissionLauncher.launch(intent);
    }

    private void requestWriteSettingsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            writeSettingsLauncher.launch(intent);
        }
    }

    private void checkWriteSettingsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                requestWriteSettingsPermission();
            }
        }
    }

    private boolean hasAllPermissions() {
        boolean basicPermissions = ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE)
                == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                        == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                        == PackageManager.PERMISSION_GRANTED;

        boolean overlayPermission = Settings.canDrawOverlays(this);
        boolean writeSettingsPermission = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            writeSettingsPermission = Settings.System.canWrite(this);
        }

        return basicPermissions && overlayPermission && writeSettingsPermission;
    }

    private void startOverlayService() {
        Intent intent = new Intent(this, OverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        prefs.edit().putBoolean(KEY_SERVICE_ENABLED, true).apply();
        showToast("Edge Assist activated!");
    }

    private void stopOverlayService() {
        Intent intent = new Intent(this, OverlayService.class);
        stopService(intent);
        prefs.edit().putBoolean(KEY_SERVICE_ENABLED, false).apply();
        showToast("Edge Assist deactivated");
    }

    private void updateUI() {
        boolean hasPermissions = hasAllPermissions();
        boolean serviceEnabled = prefs.getBoolean(KEY_SERVICE_ENABLED, false);

        serviceToggle.setEnabled(hasPermissions);
        serviceToggle.setChecked(serviceEnabled && hasPermissions);
        permissionBtn.setVisibility(hasPermissions ? View.GONE : View.VISIBLE);

        if (hasPermissions) {
            statusText.setText(serviceEnabled ? "Service Active" : "Service Inactive");
            statusCard.setCardBackgroundColor(
                    ContextCompat.getColor(this,
                            serviceEnabled ? R.color.success_color : R.color.inactive_color));
        } else {
            statusText.setText("Permissions Required");
            statusCard.setCardBackgroundColor(
                    ContextCompat.getColor(this, R.color.warning_color));
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    // Inner class for Speed Dial Contact
    public static class SpeedDialContact {
        public String name;
        public String phoneNumber;

        public SpeedDialContact(String name, String phoneNumber) {
            this.name = name;
            this.phoneNumber = phoneNumber;
        }
    }
}