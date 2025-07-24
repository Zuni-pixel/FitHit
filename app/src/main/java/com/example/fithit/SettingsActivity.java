package com.example.fithit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity {

    Button btnChangeHealthData;


    private Switch switchDarkMode, switchNotifications;
    private Button btnChangePassword;
    private Button btnLogout;



    private TextView userEmailTextView;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "settings_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load saved dark mode before anything
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);


        // Initialize UI elements
        userEmailTextView = findViewById(R.id.userEmailTextView);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchNotifications = findViewById(R.id.switchNotifications);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout = findViewById(R.id.btnLogout);
        btnChangeHealthData = findViewById(R.id.btnChangeHealthData);

        btnChangeHealthData.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, HeightDiseasesActivity.class);
            startActivity(intent);
        });



        // Get current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getEmail() != null) {
            userEmailTextView.setText("Logged in as: " + currentUser.getEmail());
        } else {
            userEmailTextView.setText("User not logged in");
        }

        // Load saved notification state
        boolean isNotificationEnabled = sharedPreferences.getBoolean("notifications", true);
        switchDarkMode.setChecked(isDarkMode);
        switchNotifications.setChecked(isNotificationEnabled);

        // Dark Mode Toggle
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("dark_mode", isChecked);
            editor.apply();

            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );

            recreate(); // Restart activity to apply theme
        });

        // Notifications Toggle
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("notifications", isChecked);
            editor.apply();

            Toast.makeText(this, isChecked ? "Notifications Enabled" : "Notifications Disabled", Toast.LENGTH_SHORT).show();
        });

        // Change Password Button
        btnChangePassword.setOnClickListener(v -> {
            if (currentUser != null && currentUser.getEmail() != null) {
                FirebaseAuth.getInstance().sendPasswordResetEmail(currentUser.getEmail())
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(SettingsActivity.this, "Reset email sent to: " + currentUser.getEmail(), Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(SettingsActivity.this, "Failed to send email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(SettingsActivity.this, "No user is logged in or email not found.", Toast.LENGTH_SHORT).show();
            }
        });
        btnLogout.setOnClickListener(v -> {
            Toast.makeText(SettingsActivity.this, "Logged out", Toast.LENGTH_SHORT).show();

            // Clear local saved settings if needed
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

            // Go back to login screen (replace with your login activity name)
            Intent intent = new Intent(SettingsActivity.this, SignActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clears all previous activities
            startActivity(intent);
            finish();
        });

    }
}
