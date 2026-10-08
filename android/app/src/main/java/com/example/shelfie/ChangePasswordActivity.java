package com.example.shelfie;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ChangePasswordActivity extends AppCompatActivity {

    private TextInputEditText edCurrent;
    private TextInputEditText edNew;
    private TextInputEditText edConfirm;
    private SessionManager sm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        sm = new SessionManager(this);
        if (!sm.isLoggedIn() || sm.getUserId() < 0) {
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbarChangePassword);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(
                    ContextCompat.getColor(this, R.color.toolbar_content));
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        edCurrent = findViewById(R.id.edCurrentPassword);
        edNew = findViewById(R.id.edNewPassword);
        edConfirm = findViewById(R.id.edConfirmNewPassword);

        MaterialButton btnSave = findViewById(R.id.btnSavePassword);

        TextWatcher refreshNewRules = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                refreshPasswordRequirementRows();
            }
        };
        edNew.addTextChangedListener(refreshNewRules);

        refreshPasswordRequirementRows();

        btnSave.setOnClickListener(v -> attemptChangePassword());
    }

    private void refreshPasswordRequirementRows() {
        PasswordRules.Result r = PasswordRules.evaluate(edNew.getText());
        PasswordUiHelper.updateRequirementLabels(
                this,
                r,
                findViewById(R.id.tvPasswordReqLength),
                findViewById(R.id.tvPasswordReqUpper),
                findViewById(R.id.tvPasswordReqLower),
                findViewById(R.id.tvPasswordReqSpecial));
    }

    private void attemptChangePassword() {
        String current = textOf(edCurrent);
        String newPass = textOf(edNew);
        String confirm = textOf(edConfirm);

        if (TextUtils.isEmpty(current)) {
            edCurrent.setError(getString(R.string.error_current_password_empty));
            edCurrent.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(newPass)) {
            edNew.setError(getString(R.string.error_field_password_empty));
            edNew.requestFocus();
            return;
        }
        PasswordRules.Result rules = PasswordRules.evaluate(newPass);
        if (!rules.isValid()) {
            edNew.setError(getString(R.string.error_password_requirements));
            edNew.requestFocus();
            return;
        }
        if (!newPass.equals(confirm)) {
            edConfirm.setError(getString(R.string.error_confirm_password_mismatch));
            edConfirm.requestFocus();
            return;
        }

        String url = ApiConfig.BASE_URL + "change_password.php";
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (obj.optBoolean("success", false)) {
                            Toast.makeText(this,
                                    obj.optString("message", getString(R.string.toast_password_updated)),
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this,
                                    obj.optString("message", getString(R.string.toast_password_update_failed)),
                                    Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, R.string.error_generic, Toast.LENGTH_LONG).show();
                    }
                },
                (VolleyError error) -> Toast.makeText(this,
                        NetworkErrorHelper.message(ChangePasswordActivity.this, error),
                        Toast.LENGTH_LONG).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> map = new HashMap<>();
                map.put("user_id", String.valueOf(sm.getUserId()));
                map.put("current_password", current);
                map.put("new_password", newPass);
                return map;
            }
        };
        queue.add(request);
    }

    private static String textOf(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString();
    }
}
