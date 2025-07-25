package com.example.fithit;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BadgesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private BadgeAdapter badgeAdapter;
    private List<Badge> badgeList;
    private LinearLayout layoutExercises;
    private LinearLayout layoutCoins;
    private int exerciseDoneCount = 0;
    private final int TOTAL_EXERCISES = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_badges);
        Button btnLeaderboard = findViewById(R.id.btnLeaderboard);
        btnLeaderboard.setOnClickListener(v -> {
            Intent intent = new Intent(BadgesActivity.this, LeaderboardActivity.class);
            startActivity(intent);
        });
        Button btnChallenge = findViewById(R.id.btnChallenge);
        btnChallenge.setOnClickListener(v -> {
            Intent intent = new Intent(BadgesActivity.this, ChallengeActivity.class);
            startActivity(intent);
        });



        recyclerView = findViewById(R.id.recyclerViewBadges);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        badgeList = new ArrayList<>();
        badgeAdapter = new BadgeAdapter(badgeList);
        recyclerView.setAdapter(badgeAdapter);

        layoutExercises = findViewById(R.id.layoutExercises);
        layoutCoins = findViewById(R.id.layoutCoins); // this must be in XML
        layoutCoins.setVisibility(LinearLayout.GONE); // hidden initially

        setupExercises();
        loadBadgesFromFirebase();
    }

    // ✅ Handles both "users" and "Users" in Firebase
    private void loadBadgesFromFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        DatabaseReference badgeRefLower = rootRef.child("users").child(uid);
        DatabaseReference badgeRefUpper = rootRef.child("Users").child(uid);

        badgeRefLower.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    loadBadgesFromSnapshot(snapshot);
                } else {
                    badgeRefUpper.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshotUpper) {
                            if (snapshotUpper.exists()) {
                                loadBadgesFromSnapshot(snapshotUpper);
                            } else {
                                Toast.makeText(BadgesActivity.this, "User ID not found in Firebase", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(BadgesActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(BadgesActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ✅ Common method to parse Firebase badge data
    private void loadBadgesFromSnapshot(DataSnapshot snapshot) {
        badgeList.clear();

        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        Long exercisesCompleted = snapshot.child("exercisesCompleted").getValue(Long.class);
        Boolean postureDone = snapshot.child("postureDone").getValue(Boolean.class);
        Long loginStreak = snapshot.child("loginStreak").getValue(Long.class);

        if (exercisesCompleted != null && exercisesCompleted >= 1) {
            badgeList.add(new Badge("Beginner", R.drawable.badge_beginner));
        }

        if (postureDone != null && postureDone) {
            badgeList.add(new Badge("Posture Corrector", R.drawable.badge_posture));
        }

        if (loginStreak != null && loginStreak >= 7) {
            badgeList.add(new Badge("Streak 7 Days", R.drawable.badge_streak7));
        }

        if (badgeList.isEmpty()) {
            Toast.makeText(this, "No badges earned yet", Toast.LENGTH_SHORT).show();
        }

        badgeAdapter.notifyDataSetChanged();
        // ✅ Add leaderboard entry
        updateLeaderboard(userEmail, exercisesCompleted != null ? exercisesCompleted.intValue() : 0);

    }

    // ✅ Hardcoded exercise section with click-to-complete logic
    private void setupExercises() {
        String[] exercises = {"Neck Stretch", "Shoulder Rolls", "Back Twist"};

        for (String exercise : exercises) {
            TextView exerciseView = new TextView(this);
            exerciseView.setText("◻️ " + exercise);
            exerciseView.setTextSize(16);
            exerciseView.setPadding(8, 12, 8, 12);
            exerciseView.setTextColor(ContextCompat.getColor(this, R.color.text_color));
            exerciseView.setBackgroundResource(R.drawable.badge_border);

            exerciseView.setOnClickListener(v -> {
                if (!exerciseView.getText().toString().contains("✅")) {
                    exerciseView.setText("✅ " + exercise);
                    exerciseView.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                    exerciseDoneCount++;

                    if (exerciseDoneCount == TOTAL_EXERCISES) {
                        showCoinReward();
                    }
                }
            });

            layoutExercises.addView(exerciseView);
        }
    }

    // ✅ Show coin reward when all exercises are marked
    private void showCoinReward() {
        layoutCoins.setVisibility(LinearLayout.VISIBLE);

        ImageView coinImage = new ImageView(this);
        coinImage.setImageResource(R.drawable.coin); // make sure coin.png is in drawable
        coinImage.setLayoutParams(new LinearLayout.LayoutParams(120, 120));

        TextView rewardText = new TextView(this);
        rewardText.setText("🎉 You earned 50 coins!");
        rewardText.setTextSize(18);
        rewardText.setPadding(8, 16, 8, 16);
        rewardText.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));

        layoutCoins.addView(coinImage);
        layoutCoins.addView(rewardText);
    }
    // ✅ 🔄 This method updates leaderboard node in Firebase
    private void updateLeaderboard(String userEmail, int exercisesCompleted) {
        DatabaseReference leaderboardRef = FirebaseDatabase.getInstance().getReference("Leaderboard");

        String userKey = userEmail.replace(".", "_");
        int score = exercisesCompleted * 10;

        Map<String, Object> leaderboardData = new HashMap<>();
        leaderboardData.put("email", userEmail);
        leaderboardData.put("exercisesCompleted", exercisesCompleted);
        leaderboardData.put("score", score);

        leaderboardRef.child(userKey).setValue(leaderboardData);
    }
}



