package com.example.shelfie;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Preferences plus reading data actions, discover links, and support shortcuts (moved from main overflow menu).

public class SettingsActivity extends AppCompatActivity {

    private static final int REQUEST_CALL_PERMISSION = 100;

    private boolean bindingSwitches;
    private String pendingCallNumber;

    private MaterialToolbar toolbar;
    private SwitchMaterial switchNotifications;
    private SwitchMaterial switchBiometric;
    private SwitchMaterial switchDarkMode;

    private SessionManager sm;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    ThemeManager.setReadingRemindersEnabled(this, true);
                    ReadingReminderScheduler.sync(this);
                    bindingSwitches = true;
                    switchNotifications.setChecked(true);
                    bindingSwitches = false;
                } else {
                    Toast.makeText(this, R.string.toast_notifications_permission_denied, Toast.LENGTH_LONG).show();
                    bindingSwitches = true;
                    switchNotifications.setChecked(false);
                    bindingSwitches = false;
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sm = new SessionManager(this);

        toolbar = findViewById(R.id.toolbarSettings);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
        if (toolbar.getOverflowIcon() != null) {
            toolbar.getOverflowIcon().setTint(
                    ContextCompat.getColor(this, R.color.toolbar_content));
        }

        switchNotifications = findViewById(R.id.switchNotifications);
        switchBiometric = findViewById(R.id.switchBiometric);
        switchDarkMode = findViewById(R.id.switchDarkMode);

        bindingSwitches = true;
        switchNotifications.setChecked(ThemeManager.isReadingRemindersEnabled(this));
        switchBiometric.setChecked(ThemeManager.isBiometricLockEnabled(this));
        switchDarkMode.setChecked(ThemeManager.isDarkModeEnabled(this));
        bindingSwitches = false;

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingSwitches) {
                return;
            }
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    bindingSwitches = true;
                    buttonView.setChecked(false);
                    bindingSwitches = false;
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    return;
                }
                ThemeManager.setReadingRemindersEnabled(this, true);
                ReadingReminderScheduler.sync(this);
            } else {
                ThemeManager.setReadingRemindersEnabled(this, false);
                ReadingReminderScheduler.sync(this);
            }
        });

        switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingSwitches) {
                return;
            }
            ThemeManager.setBiometricLockEnabled(this, isChecked);
        });

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingSwitches) {
                return;
            }
            ThemeManager.setDarkModeEnabled(this, isChecked);
            recreate();
        });

        MaterialButton btnChangePassword = findViewById(R.id.btnSettingsChangePassword);
        btnChangePassword.setOnClickListener(v ->
                startActivity(new Intent(this, ChangePasswordActivity.class)));

        MaterialButton btnDeleteAccount = findViewById(R.id.btnSettingsDeleteAccount);
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());

        MaterialButton btnStats = findViewById(R.id.btnSettingsReadingStats);
        MaterialButton btnExport = findViewById(R.id.btnSettingsExport);
        MaterialButton btnWeb = findViewById(R.id.btnSettingsBooksWeb);
        MaterialButton btnMap = findViewById(R.id.btnSettingsBookstoresMap);
        MaterialButton btnDial = findViewById(R.id.btnSettingsDialSupport);
        MaterialButton btnCall = findViewById(R.id.btnSettingsCallDirect);

        btnStats.setOnClickListener(v -> startActivity(new Intent(this, ReadingStatsActivity.class)));
        btnExport.setOnClickListener(v -> showExportFormatDialog());
        btnWeb.setOnClickListener(v -> openBooksWeb());
        btnMap.setOnClickListener(v -> openBookstoresMap());
        btnDial.setOnClickListener(v -> dialSupport());
        btnCall.setOnClickListener(v -> requestCallPermissionAndCall(getString(R.string.support_phone_number)));
    }

    private void openBooksWeb() {
        Uri page = Uri.parse("https://books.google.com");
        Intent i = new Intent(Intent.ACTION_VIEW, page);
        if (i.resolveActivity(getPackageManager()) != null) {
            startActivity(i);
        } else {
            Toast.makeText(this, R.string.toast_no_browser, Toast.LENGTH_SHORT).show();
        }
    }

    private void openBookstoresMap() {
        Uri location = Uri.parse("geo:0,0?q=bookstore near me");
        Intent i = new Intent(Intent.ACTION_VIEW, location);
        safeStartMap(i);
    }

    private void dialSupport() {
        Uri phoneNb = Uri.parse("tel:" + getString(R.string.support_phone_number));
        Intent i = new Intent(Intent.ACTION_DIAL, phoneNb);
        if (i.resolveActivity(getPackageManager()) != null) {
            startActivity(i);
        } else {
            Toast.makeText(this, R.string.toast_no_dial, Toast.LENGTH_SHORT).show();
        }
    }

    private void safeStartMap(Intent i) {
        PackageManager pm = getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(i, 0);
        if (!activities.isEmpty()) {
            startActivity(i);
        } else {
            Toast.makeText(this, R.string.toast_no_maps, Toast.LENGTH_SHORT).show();
        }
    }

    private void requestCallPermissionAndCall(String nb) {
        pendingCallNumber = nb;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PERMISSION);
        } else {
            Uri phoneNb = Uri.parse("tel:" + nb);
            Intent i = new Intent(Intent.ACTION_CALL, phoneNb);
            startActivity(i);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CALL_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingCallNumber != null) {
                    requestCallPermissionAndCall(pendingCallNumber);
                }
            } else {
                Toast.makeText(this, R.string.toast_call_permission_denied, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showDeleteAccountDialog() {
        if (!sm.isLoggedIn() || sm.getUserId() < 0) {
            return;
        }
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_account, null);
        TextInputEditText edPassword = dialogView.findViewById(R.id.edDeleteAccountPassword);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_delete_account_title)
                .setMessage(R.string.dialog_delete_account_body)
                .setView(dialogView)
                .setNegativeButton(R.string.dialog_negative_cancel, null)
                .setPositiveButton(R.string.dialog_positive_delete_account, null);
        AlertDialog dlg = builder.create();
        dlg.setOnShowListener(d -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String password = edPassword.getText() == null ? "" : edPassword.getText().toString().trim();
            if (TextUtils.isEmpty(password)) {
                edPassword.setError(getString(R.string.error_field_password_empty));
                return;
            }
            edPassword.setError(null);
            requestDeleteAccount(password);
            dlg.dismiss();
        }));
        dlg.show();
    }

    private void requestDeleteAccount(String currentPassword) {
        String url = ApiConfig.BASE_URL + "delete_account.php";
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (obj.optBoolean("success", false)) {
                            Toast.makeText(this,
                                    obj.optString("message", getString(R.string.toast_account_deleted)),
                                    Toast.LENGTH_SHORT).show();
                            sm.logout();
                            Intent i = new Intent(this, LoginActivity.class);
                            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(i);
                            finish();
                        } else {
                            Toast.makeText(this,
                                    obj.optString("message", getString(R.string.toast_account_delete_failed)),
                                    Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, R.string.toast_account_delete_failed, Toast.LENGTH_LONG).show();
                    }
                },
                error -> Toast.makeText(this,
                        NetworkErrorHelper.message(SettingsActivity.this, error),
                        Toast.LENGTH_LONG).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> map = new HashMap<>();
                map.put("user_id", String.valueOf(sm.getUserId()));
                map.put("current_password", currentPassword);
                return map;
            }
        };
        queue.add(request);
    }

    private void showExportFormatDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.export_choose_format)
                .setItems(new CharSequence[]{
                        getString(R.string.export_json),
                        getString(R.string.export_csv)
                }, (d, which) -> performExport(which == 1))
                .setNegativeButton(R.string.dialog_negative_cancel, null)
                .show();
    }

    private void performExport(boolean asCsv) {
        String url = ApiConfig.BASE_URL + "export_shelf_journal.php?user_id=" + sm.getUserId();
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        String payload = asCsv ? ExportHelper.jsonToCsv(response) : response;
                        String mime = asCsv ? "text/csv" : "application/json";
                        Intent send = new Intent(Intent.ACTION_SEND);
                        send.setType(mime);
                        send.putExtra(Intent.EXTRA_TEXT, payload);
                        send.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.export_share_title));
                        Intent chooser = Intent.createChooser(send, getString(R.string.export_share_title));
                        if (chooser.resolveActivity(getPackageManager()) != null) {
                            startActivity(chooser);
                        } else {
                            Toast.makeText(this, R.string.toast_no_share_handler, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, R.string.toast_export_failed, Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, R.string.toast_export_failed, Toast.LENGTH_SHORT).show());
        queue.add(request);
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
