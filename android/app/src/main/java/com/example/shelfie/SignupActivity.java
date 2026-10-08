package com.example.shelfie;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Patterns;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    // Declare all variables
    EditText edFirstName, edLastName, edEmail, edPassword;
    TextView tvPasswordReqLength;
    TextView tvPasswordReqUpper;
    TextView tvPasswordReqLower;
    TextView tvPasswordReqSpecial;
    Button btnSignup;
    TextView tvLogin;

    SessionManager sm;

    // Wires validation on signup, navigates back to login, and calls signupUser when the form is complete

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Reference all variables
        edFirstName = (EditText) findViewById(R.id.edFirstNameSignup);
        edLastName = (EditText) findViewById(R.id.edLastNameSignup);
        edEmail = (EditText) findViewById(R.id.edEmailSignup);
        edPassword = (EditText) findViewById(R.id.edPasswordSignup);
        tvPasswordReqLength = findViewById(R.id.tvPasswordReqLength);
        tvPasswordReqUpper = findViewById(R.id.tvPasswordReqUpper);
        tvPasswordReqLower = findViewById(R.id.tvPasswordReqLower);
        tvPasswordReqSpecial = findViewById(R.id.tvPasswordReqSpecial);
        btnSignup = (Button) findViewById(R.id.btnSignup);
        tvLogin = (TextView) findViewById(R.id.tvLogin);
        sm = new SessionManager(SignupActivity.this);

        edPassword.addTextChangedListener(new TextWatcher() {
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
        });
        refreshPasswordRequirementRows();

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
        });

        btnSignup.setOnClickListener(v -> {
            if(TextUtils.isEmpty(edFirstName.getText()))
            {
                edFirstName.setError(getString(R.string.error_field_first_name_empty));
            }
            else if(TextUtils.isEmpty(edLastName.getText()))
            {
                edLastName.setError(getString(R.string.error_field_last_name_empty));
            }
            else if(TextUtils.isEmpty(edEmail.getText()))
            {
                edEmail.setError(getString(R.string.error_field_email_empty));
            }
            else if (!Patterns.EMAIL_ADDRESS.matcher(edEmail.getText().toString().trim()).matches()) {
                edEmail.setError(getString(R.string.error_invalid_email));
            }
            else if(TextUtils.isEmpty(edPassword.getText()))
            {
                edPassword.setError(getString(R.string.error_field_password_empty));
            }
            else if (!PasswordRules.evaluate(edPassword.getText()).isValid()) {
                edPassword.setError(getString(R.string.error_password_requirements));
            }
            else{
                signupUser();
            }
        });
    }

    private void refreshPasswordRequirementRows() {
        PasswordRules.Result r = PasswordRules.evaluate(edPassword.getText());
        PasswordUiHelper.updateRequirementLabels(
                this,
                r,
                tvPasswordReqLength,
                tvPasswordReqUpper,
                tvPasswordReqLower,
                tvPasswordReqSpecial);
    }

    public void signupUser()
    {
        String url = ApiConfig.BASE_URL + "signup.php";
        RequestQueue queue = Volley.newRequestQueue(SignupActivity.this);
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (obj.optBoolean("success", false)) {
                            Toast.makeText(SignupActivity.this,
                                    obj.optString("message", getString(R.string.signup_success)),
                                    Toast.LENGTH_SHORT).show();
                            loginAfterSignup();
                        } else {
                            Toast.makeText(SignupActivity.this,
                                    obj.optString("message", getString(R.string.error_generic)),
                                    Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(SignupActivity.this, R.string.error_generic, Toast.LENGTH_LONG).show();
                    }
                },
                (VolleyError error) -> Toast.makeText(SignupActivity.this,
                        NetworkErrorHelper.message(SignupActivity.this, error),
                        Toast.LENGTH_LONG).show()
        ){
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> map = new HashMap<>();
                map.put("first_name", edFirstName.getText().toString().trim());
                map.put("last_name", edLastName.getText().toString().trim());
                map.put("email", edEmail.getText().toString().trim());
                map.put("password", edPassword.getText().toString());
                return map;
            }
        };

        queue.add(request);
    }

    //logs in with the same email and password so SessionManager has a user id before opening ProfileActivity onboarding
    private void loginAfterSignup() {
        String url = ApiConfig.BASE_URL + "login.php";
        RequestQueue queue = Volley.newRequestQueue(SignupActivity.this);

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (obj.optBoolean("success", false)) {
                            JSONObject user = obj.getJSONObject("user");
                            String first = user.optString("first_name", edFirstName.getText().toString().trim());
                            String last = user.optString("last_name", edLastName.getText().toString().trim());
                            boolean isAdmin = user.optInt("is_admin", 0) == 1;
                            sm.saveUser(
                                    user.getInt("id"),
                                    first,
                                    last,
                                    user.optString("email", edEmail.getText().toString()),
                                    isAdmin
                            );

                            Intent i = new Intent(SignupActivity.this, ProfileActivity.class);
                            i.putExtra("is_onboarding", true);
                            startActivity(i);
                            finish();
                        } else {
                            Toast.makeText(SignupActivity.this,
                                    obj.optString("message", getString(R.string.error_generic)),
                                    Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(SignupActivity.this, R.string.error_generic, Toast.LENGTH_LONG).show();
                    }
                },
                error -> Toast.makeText(SignupActivity.this,
                        NetworkErrorHelper.message(SignupActivity.this, error),
                        Toast.LENGTH_LONG).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> map = new HashMap<>();
                map.put("email", edEmail.getText().toString().trim());
                map.put("password", edPassword.getText().toString());
                return map;
            }
        };

        queue.add(request);
    }
}
