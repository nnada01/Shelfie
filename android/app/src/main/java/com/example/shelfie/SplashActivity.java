package com.example.shelfie;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

//First screen: waits a few seconds then sends the user to MainActivity if logged in, else LoginActivity

public class SplashActivity extends AppCompatActivity {

    SessionManager sm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        sm = new SessionManager(SplashActivity.this);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if(sm.isLoggedIn())
                {
                    Intent i = new Intent(SplashActivity.this, MainActivity.class);
                    startActivity(i);
                }
                else{
                    Intent i = new Intent(SplashActivity.this, LoginActivity.class);
                    startActivity(i);
                }
                finish();
            }
        }, 3000);
    }
}
