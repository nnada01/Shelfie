package com.example.shelfie;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
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

public class LoginActivity extends AppCompatActivity {

    // Declare all variables
    EditText edEmail, edPassword;
    Button btnLogin;
    TextView tvSignup;

    SessionManager sm;

    // inflates login, binds views, validates on button tap, and opens signup when the link is pressed
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Reference all variables
        edEmail = (EditText) findViewById(R.id.edEmail);
        edPassword = (EditText) findViewById(R.id.edPassword);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        tvSignup = (TextView) findViewById(R.id.tvSignup);
        sm = new SessionManager(LoginActivity.this);
        btnLogin.setOnClickListener(v -> {
            if(TextUtils.isEmpty(edEmail.getText()))
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
            else{
                loginUser();
            }
        });

        tvSignup.setOnClickListener(v -> {
            Intent i = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(i);
        });
    }

    // POSTs credentials to login.php, parses JSON, saves the user via SessionManager, then finishes into MainActivity
    public void loginUser() {
        String url = ApiConfig.BASE_URL + "login.php";
        RequestQueue queue = Volley.newRequestQueue(LoginActivity.this);
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try{
                        JSONObject obj = new JSONObject(response);

                        if(obj.getBoolean("success"))
                        {
                            JSONObject user = obj.getJSONObject("user");
                            String firstName = user.optString("first_name", "");
                            String lastName = user.optString("last_name", "");
                            if (firstName.isEmpty() && lastName.isEmpty()) {
                                String[] split = splitFullName(user.optString("full_name", ""));
                                firstName = split[0];
                                lastName = split[1];
                            }

                            boolean isAdmin = user.optInt("is_admin", 0) == 1;
                            sm.saveUser(
                                    user.getInt("id"),
                                    firstName,
                                    lastName,
                                    user.getString("email"),
                                    isAdmin
                            );

                            Toast.makeText(LoginActivity.this, R.string.toast_login_success, Toast.LENGTH_SHORT).show();
                            Intent i = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(i);
                            finish();
                        }
                        else{
                            Toast.makeText(LoginActivity.this, obj.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    }
                    catch (Exception e){
                        Toast.makeText(LoginActivity.this, R.string.error_login_parsing, Toast.LENGTH_SHORT).show();
                    }
                },
                (VolleyError error) -> Toast.makeText(LoginActivity.this,
                        NetworkErrorHelper.message(LoginActivity.this, error),
                        Toast.LENGTH_LONG).show()
        ){
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

    //splits a full name on the first space so the API can return either split names or one combined string
    private String[] splitFullName(String fullName) {
        String cleaned = fullName == null ? "" : fullName.trim();
        if (cleaned.isEmpty()) return new String[]{"", ""};
        int firstSpace = cleaned.indexOf(' ');
        if (firstSpace == -1) return new String[]{cleaned, ""};
        return new String[]{
                cleaned.substring(0, firstSpace).trim(),
                cleaned.substring(firstSpace + 1).trim()
        };
    }
}
