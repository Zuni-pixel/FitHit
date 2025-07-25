package com.example.fithit;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;

public class HomeActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;
    private TextView greeting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Log.d("USER_UID", "UID is: " + user.getUid());
        } else {
            Log.d("USER_UID", "User not logged in");
        }

        // Notification permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                );
            }
        }

        // Notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "fitness_channel",
                    "Fitness Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications for workout reminders");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        NotificationScheduler.scheduleNotifications(this);


        TextView workoutDescription = findViewById(R.id.workoutDescription);
        ImageView workoutImage = findViewById(R.id.middleImage);
        TextView dateText = findViewById(R.id.dateText);
        ImageView bellIcon = findViewById(R.id.bellIcon);

        // Date
        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy");
        dateText.setText(dateFormat.format(currentDate));

        workoutDescription.setText("Day 1 - Squat");
        workoutImage.setImageResource(R.drawable.sample_image);

        // Button Clicks
        findViewById(R.id.btnHealth).setOnClickListener(v -> {
            try {
                startActivity(new Intent(HomeActivity.this, HealthDashboardActivity.class));
            } catch (Exception e) {
                Toast.makeText(this, "Couldn't open health dashboard", Toast.LENGTH_SHORT).show();
            }
        });
        // Button Clicks
        findViewById(R.id.btnrecommendation).setOnClickListener(v -> {
            try {
                startActivity(new Intent(HomeActivity.this, RecommendationActivity.class));
            } catch (Exception e) {
                Toast.makeText(this, "Couldn't open health dashboard", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnPosture).setOnClickListener(v -> {
            // Verify this targets YOUR feature's activity (not SplashActivity)
            Intent intent = new Intent(this, selectExerciseActivity.class);
            startActivity(intent);
        });

        bellIcon.setOnClickListener(v ->
                Toast.makeText(this, "Bell icon clicked!", Toast.LENGTH_SHORT).show()
        );


        // ✅ Bottom Navigation (placed correctly INSIDE onCreate)
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                Toast.makeText(this, "You're already on Home 🏠", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.navigation_workouts) {
                startActivity(new Intent(HomeActivity.this, MoodSelectionActivity.class));
                return true;
            } else if (itemId == R.id.navigation_settings) {
                startActivity(new Intent(HomeActivity.this, SettingsActivity.class));
                return true;
            } else if (itemId == R.id.navigation_gamification) {
                startActivity(new Intent(HomeActivity.this, BadgesActivity.class));
                return true;
            } else if (itemId == R.id.navigation_person) {
                startActivity(new Intent(HomeActivity.this, CommunityActivity.class));
                return true;
            }
            return false;
        });
    }

    private void updateExerciseCount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(user.getUid());
            userRef.child("exercisesCompleted").setValue(ServerValue.increment(1));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

