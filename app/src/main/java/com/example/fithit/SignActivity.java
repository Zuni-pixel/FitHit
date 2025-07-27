package com.example.fithit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class SignActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("settings_prefs", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false); // default to light mode
        AppCompatDelegate.setDefaultNightMode(isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign);

        Button signupButton = findViewById(R.id.signupButton);
        TextView loginText = findViewById(R.id.loginText);

        // Handle Sign-Up Button Click
        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });

        // Handle Log In Text Click
        loginText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignActivity.this,LoginActivity.class);
                startActivity(intent);
            }
        });
    }
}
