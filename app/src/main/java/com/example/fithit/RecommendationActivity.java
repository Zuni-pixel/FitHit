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
    private static final String[][] RECOMMENDATIONS = {
            {"4 sets of 10-15 reps for each exercise", "High protein, low carb diet", "Dumbbells, Barbell, Bench", "4 sets of 10-15 reps"},
            {"3 sets of 12-15 reps for each exercise", "Balanced diet with moderate carbs", "Kettlebells, Resistance Bands", "3 sets of 12-15 reps"},
            {"5 sets of 8-10 reps for each exercise", "Low-fat, high-fiber diet", "Pull-up Bar, Yoga Mat", "5 sets of 8-10 reps"},
            {"2 sets of 15-20 reps for each exercise", "Protein shakes and clean eating", "Treadmill, Elliptical", "2 sets of 15-20 reps"},
            {"3 sets of 10-12 reps for each exercise", "Mediterranean-style diet", "Dumbbells, Medicine Ball", "3 sets of 10-12 reps"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendation);

        initializeViews();
        setupHomeButton();
        fetchUserData();
    }

    private void initializeViews() {
        bulletPoint1 = findViewById(R.id.bulletPoint1);
        bulletPoint2 = findViewById(R.id.bulletPoint2);
        bulletPoint3 = findViewById(R.id.bulletPoint3);
        bulletPoint4 = findViewById(R.id.bulletPoint4);
    }

    private void setupHomeButton() {
        Button homeButton = findViewById(R.id.homeButton);
        homeButton.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
    }

    private void fetchUserData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showToastAndFinish("User not authenticated!");
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        processUserData(snapshot);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        showToast("Failed to fetch user data: " + error.getMessage());
                    }
                });
    }

    private void processUserData(DataSnapshot snapshot) {
        if (!snapshot.exists()) {
            showToast("No user data found");
            return;
        }

        try {
            String heightStr = getStringValue(snapshot, "height");
            String weightStr = getStringValue(snapshot, "weight");
            String ageStr = getStringValue(snapshot, "age");
            String gender = getStringValue(snapshot, "gender");
            String goal = getStringValue(snapshot, "goal");

            if (heightStr == null || weightStr == null || ageStr == null ||
                    gender == null || goal == null) {
                showToast("Incomplete user data");
                return;
            }

            float height = Float.parseFloat(heightStr);
            float weight = Float.parseFloat(weightStr);
            float age = Float.parseFloat(ageStr);
            float bmi = calculateBMI(height, weight);

            makePrediction(height, weight, age, bmi, gender, goal);
        } catch (NumberFormatException e) {
            showToast("Invalid numeric data format");
        }
    }

    private String getStringValue(DataSnapshot snapshot, String key) {
        return snapshot.child(key).getValue(String.class);
    }

    private float calculateBMI(float height, float weight) {
        return weight / ((height / 100f) * (height / 100f));
    }

    private void makePrediction(float height, float weight, float age,
                                float bmi, String gender, String goal) {
        try {
            float genderEncoded = encodeGender(gender);
            float goalEncoded = encodeGoal(goal);

            if (genderEncoded < 0 || goalEncoded < 0) {
                showToast("Unsupported gender or goal value");
                return;
            }

            float[] inputData = {height, weight, age, bmi, genderEncoded, goalEncoded};
            predictWithModel(inputData);
        } catch (Exception e) {
            showToast("Error processing prediction");
            Log.e("Prediction", "Error", e);
        }
    }

    private void predictWithModel(float[] inputData) {
        try (RecommendationModelHandler modelHandler =
                     new RecommendationModelHandler(getApplicationContext(), "workout_recommendation_model.tflite")) {

            float[] output = modelHandler.predict(inputData);
            displayRecommendation(output);

        } catch (IOException e) {
            showToast("Model file not found");
            Log.e("Model", "File error", e);
        } catch (IllegalArgumentException e) {
            showToast("Invalid input format");
            Log.e("Model", "Input error", e);
        } catch (Exception e) {
            showToast("Prediction failed");
            Log.e("Model", "Prediction error", e);
        }
    }

    private void displayRecommendation(float[] output) {
        runOnUiThread(() -> {
            int bestIndex = findBestRecommendation(output);
            if (bestIndex >= 0 && bestIndex < RECOMMENDATIONS.length) {
                bulletPoint1.setText("Exercise: " + RECOMMENDATIONS[bestIndex][0]);
                bulletPoint2.setText("Diet: " + RECOMMENDATIONS[bestIndex][1]);
                bulletPoint3.setText("Equipment: " + RECOMMENDATIONS[bestIndex][2]);
                bulletPoint4.setText("Sets: " + RECOMMENDATIONS[bestIndex][3]);
            } else {
                showToast("No matching recommendation found");
            }
        });
    }

    private int findBestRecommendation(float[] output) {
        int bestIndex = 0;
        for (int i = 1; i < output.length; i++) {
            if (output[i] > output[bestIndex]) {
                bestIndex = i;
            }
        }
        return bestIndex;
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

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }

    private void showToastAndFinish(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            finish();
        });
    }
}