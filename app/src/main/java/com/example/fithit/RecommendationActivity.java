package com.example.fithit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;

public class RecommendationActivity extends ComponentActivity {

    private TextView bulletPoint1, bulletPoint2, bulletPoint3, bulletPoint4;
    private Button homeButton;

    private static final String[][] RECOMMENDATIONS = {
            {"4 sets of 10-15 reps for each exercise.", "High protein, low carb diet.", "Dumbbells, Barbell, Bench.", "4 sets of 10-15 reps."},
            {"3 sets of 12-15 reps for each exercise.", "Balanced diet with moderate carbs.", "Kettlebells, Resistance Bands.", "3 sets of 12-15 reps."},
            {"5 sets of 8-10 reps for each exercise.", "Low-fat, high-fiber diet.", "Pull-up Bar, Yoga Mat.", "5 sets of 8-10 reps."},
            {"2 sets of 15-20 reps for each exercise.", "Protein shakes and clean eating.", "Treadmill, Elliptical.", "2 sets of 15-20 reps."},
            {"3 sets of 10-12 reps for each exercise.", "Mediterranean-style diet.", "Dumbbells, Medicine Ball.", "3 sets of 10-12 reps."}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendation);

        bulletPoint1 = findViewById(R.id.bulletPoint1);
        bulletPoint2 = findViewById(R.id.bulletPoint2);
        bulletPoint3 = findViewById(R.id.bulletPoint3);
        bulletPoint4 = findViewById(R.id.bulletPoint4);
        homeButton = findViewById(R.id.homeButton);

        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(RecommendationActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        });

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = currentUser.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(RecommendationActivity.this, "No data found for this user!", Toast.LENGTH_SHORT).show();
                    return;
                }

                String heightStr = snapshot.child("height").getValue(String.class);
                String weightStr = snapshot.child("weight").getValue(String.class);
                String ageStr = snapshot.child("age").getValue(String.class);
                String gender = snapshot.child("gender").getValue(String.class);
                String goal = snapshot.child("goal").getValue(String.class);

                if (heightStr == null || weightStr == null || ageStr == null || gender == null || goal == null) {
                    Toast.makeText(RecommendationActivity.this, "Incomplete user data!", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    float height = Float.parseFloat(heightStr);
                    float weight = Float.parseFloat(weightStr);
                    float age = Float.parseFloat(ageStr);
                    float bmi = weight / ((height / 100f) * (height / 100f));
                    makePrediction(height, weight, age, bmi, gender, goal);
                } catch (NumberFormatException e) {
                    Toast.makeText(RecommendationActivity.this, "Invalid numeric data!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(RecommendationActivity.this, "Failed to fetch data!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void makePrediction(float height, float weight, float age, float bmi, String gender, String goal) {
        float genderEncoded = encodeGender(gender);
        float goalEncoded = encodeGoal(goal);

        if (genderEncoded < 0 || goalEncoded < 0) {
            Toast.makeText(this, "Unsupported gender or goal.", Toast.LENGTH_SHORT).show();
            return;
        }

        float[] inputData = {height, weight, age, bmi, genderEncoded, goalEncoded};

        try (RecommendationModelHandler modelHandler = new RecommendationModelHandler(this, "recommendation_model.tflite")) {
            float[] output = modelHandler.predict(inputData);

            int bestIndex = 0;
            for (int i = 1; i < output.length; i++) {
                if (output[i] > output[bestIndex]) bestIndex = i;
            }

            if (bestIndex < RECOMMENDATIONS.length) {
                bulletPoint1.setText("Exercise Recommendation: " + RECOMMENDATIONS[bestIndex][0]);
                bulletPoint2.setText("Diet Plan: " + RECOMMENDATIONS[bestIndex][1]);
                bulletPoint3.setText("Equipment Needed: " + RECOMMENDATIONS[bestIndex][2]);
                bulletPoint4.setText("Custom Exercise Sets: " + RECOMMENDATIONS[bestIndex][3]);
            } else {
                Toast.makeText(this, "Model output error!", Toast.LENGTH_SHORT).show();
            }

        } catch (IOException e) {
            Toast.makeText(this, "Failed to load model!", Toast.LENGTH_SHORT).show();
        }
    }

    private float encodeGender(String gender) {
        if (gender.equalsIgnoreCase("male")) return 0.0f;
        if (gender.equalsIgnoreCase("female")) return 1.0f;
        return -1.0f;
    }

    private float encodeGoal(String goal) {
        switch (goal.toLowerCase()) {
            case "weight loss": return 0.0f;
            case "muscle gain": return 1.0f;
            case "general fitness": return 2.0f;
            default: return -1.0f;
        }
    }
}
