package com.example.fithit;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class RecommendationActivity extends AppCompatActivity {

    private TextView tvExercise, tvDiet, tvEquipment, tvSets;
    private Map<Integer, String[]> allRecommendations = new HashMap<>();
    private float[] numMeans = new float[4];  // For height, weight, age, BMI
    private float[] numScales = new float[4];
    private static final int MODEL_OUTPUT_CLASSES = 5;  // Your model outputs 5 classes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendation);

        // Initialize UI components
        tvExercise = findViewById(R.id.bulletPoint1);
        tvDiet = findViewById(R.id.bulletPoint2);
        tvEquipment = findViewById(R.id.bulletPoint3);
        tvSets = findViewById(R.id.bulletPoint4);

        // Home button setup
        Button homeButton = findViewById(R.id.homeButton);
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to HomeActivity
                finish(); // This will close the current activity and return to the previous one (HomeActivity)

                // Alternatively, if you want to explicitly start HomeActivity:
                Intent intent = new Intent(RecommendationActivity.this, HomeActivity.class);
                 startActivity(intent);
                finish();
            }
        });

        try {
            // 1. Load configuration data (can load more than MODEL_OUTPUT_CLASSES entries)
            loadConfiguration();

            // 2. Get user data from Firebase
            fetchUserDataAndPredict();

        } catch (Exception e) {
            showError("Initialization failed: " + e.getMessage());
            Log.e("RecommendationActivity", "Error: ", e);
        }
    }

    private void loadConfiguration() throws Exception {
        // Load model_info.json from assets
        InputStream is = getAssets().open("model_info.json");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        JSONObject config = new JSONObject(sb.toString());
        JSONObject preprocessor = config.getJSONObject("preprocessor");

        // Load normalization parameters
        JSONArray meansArray = preprocessor.getJSONArray("means");
        JSONArray scalesArray = preprocessor.getJSONArray("scales");
        for (int i = 0; i < meansArray.length(); i++) {
            numMeans[i] = (float) meansArray.getDouble(i);
            numScales[i] = (float) scalesArray.getDouble(i);
        }

        // Load ALL recommendations from JSON (can be more than MODEL_OUTPUT_CLASSES)
        JSONArray exerciseData = config.getJSONArray("exercise_data");
        Log.d("ConfigLoad", "Found " + exerciseData.length() + " exercise entries in config");

        // We need at least as many recommendations as the model outputs
        if (exerciseData.length() < MODEL_OUTPUT_CLASSES) {
            throw new IllegalStateException("Config must contain at least " + MODEL_OUTPUT_CLASSES + " exercise entries");
        }

        for (int i = 0; i < exerciseData.length(); i++) {
            JSONObject item = exerciseData.getJSONObject(i);
            allRecommendations.put(i, new String[]{
                    item.getString("Exercises"),
                    item.getString("Diet"),
                    item.getString("Equipment"),
                    generateSetsDescription(i)
            });
        }
    }

    private void fetchUserDataAndPredict() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showError("User not authenticated");
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Users").child(user.getUid());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                try (RecommendationModelHandler model = new RecommendationModelHandler(RecommendationActivity.this)) {
                    // Parse user data
                    float height = Float.parseFloat(snapshot.child("height").getValue(String.class));
                    float weight = Float.parseFloat(snapshot.child("weight").getValue(String.class));
                    float age = Float.parseFloat(snapshot.child("age").getValue(String.class));
                    String gender = snapshot.child("gender").getValue(String.class);
                    String goal = snapshot.child("goal").getValue(String.class);

                    // Normalize inputs
                    float[] normalizedInput = normalizeInputs(height, weight, age, gender, goal);

                    // Get prediction (will return array of size MODEL_OUTPUT_CLASSES)
                    float[] predictions = model.predict(normalizedInput);
                    Log.d("Prediction", "Raw predictions: " + java.util.Arrays.toString(predictions));

                    // Process and display results
                    displayRecommendation(predictions);

                } catch (Exception e) {
                    showError("Prediction failed: " + e.getMessage());
                    Log.e("RecommendationActivity", "Prediction error: ", e);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                showError("Database error: " + error.getMessage());
            }
        });
    }

    private float[] normalizeInputs(float height, float weight, float age,
                                    String gender, String goal) {
        // Calculate BMI
        float bmi = weight / ((height / 100f) * (height / 100f));

        // Create input array
        float[] input = new float[9]; // 4 numeric + 2 gender + 3 goal

        // Normalize numerical features
        input[0] = (height - numMeans[0]) / numScales[0]; // height
        input[1] = (weight - numMeans[1]) / numScales[1]; // weight
        input[2] = (age - numMeans[2]) / numScales[2];    // age
        input[3] = (bmi - numMeans[3]) / numScales[3];    // bmi

        // One-hot encode gender (male=1,0; female=0,1)
        if (gender.equalsIgnoreCase("male")) {
            input[4] = 1f;
            input[5] = 0f;
        } else {
            input[4] = 0f;
            input[5] = 1f;
        }

        // One-hot encode goal
        switch (goal.toLowerCase()) {
            case "weight loss":
                input[6] = 1f; input[7] = 0f; input[8] = 0f;
                break;
            case "muscle gain":
                input[6] = 0f; input[7] = 1f; input[8] = 0f;
                break;
            default: // general fitness
                input[6] = 0f; input[7] = 0f; input[8] = 1f;
        }

        return input;
    }

    private void displayRecommendation(float[] predictions) {
        // 1. Validate predictions
        if (predictions == null || predictions.length != MODEL_OUTPUT_CLASSES) {
            showError("Invalid model output");
            return;
        }

        // 2. Find the class with highest probability (0 to MODEL_OUTPUT_CLASSES-1)
        int bestClass = 0;
        for (int i = 1; i < predictions.length; i++) {
            if (predictions[i] > predictions[bestClass]) {
                bestClass = i;
            }
        }
        Log.d("Prediction", "Selected class: " + bestClass);

        // 3. Get recommendation - must exist since we validated during load
        String[] recommendation = allRecommendations.get(bestClass);
        if (recommendation == null) {
            showError("Recommendation not found for class " + bestClass);
            return;
        }

        // 4. Update UI
        tvExercise.setText("\uD83C\uDFCB\uFE0FExercise: " + recommendation[0]);
        tvDiet.setText("\uD83E\uDD57Diet: " + recommendation[1]);
        tvEquipment.setText("\uD83D\uDEE0\uFE0FEquipment: " + recommendation[2]);
        tvSets.setText("\uD83D\uDCAA Sets: " + recommendation[3]);
    }

    private String generateSetsDescription(int classIndex) {
        // Customize based on your training data
        switch (classIndex % 5) {  // Ensure we stay within 0-4 range
            case 0: return "3 sets of 12 reps";
            case 1: return "4 sets of 10 reps";
            case 2: return "5 sets of 8 reps";
            case 3: return "3 sets of 15 reps";
            case 4: return "4 sets of 12 reps";
            default: return "3 sets of 10 reps";
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        tvExercise.setText("Error: " + message);
        Log.e("RecommendationError", message);
    }
}