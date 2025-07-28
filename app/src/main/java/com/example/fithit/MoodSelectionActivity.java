package com.example.fithit;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MoodSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_selection);

        // Initialize cards
        CardView cardHappy = findViewById(R.id.cardHappy);
        CardView cardTired = findViewById(R.id.cardTired);
        CardView cardStressed = findViewById(R.id.cardStressed);
        CardView cardEnergetic = findViewById(R.id.cardEnergetic);
        TextView tvWorkoutSuggestion = findViewById(R.id.tvWorkoutSuggestion);

        // Mood click listeners with animations
        cardHappy.setOnClickListener(v -> {
            animateCardClick(v);
            suggestWorkout("happy", tvWorkoutSuggestion);
        });

        cardTired.setOnClickListener(v -> {
            animateCardClick(v);
            suggestWorkout("tired", tvWorkoutSuggestion);
        });

        cardStressed.setOnClickListener(v -> {
            animateCardClick(v);
            suggestWorkout("stressed", tvWorkoutSuggestion);
        });

        cardEnergetic.setOnClickListener(v -> {
            animateCardClick(v);
            suggestWorkout("energetic", tvWorkoutSuggestion);
        });
    }

    // Suggest workout based on mood
    private void suggestWorkout(String mood, TextView tv) {
        String suggestion;
        switch (mood) {
            case "happy":
                suggestion = "Recommended: 🏃‍♂️ Brisk Walking\nEnjoy fresh air and light cardio!";
                break;
            case "tired":
                suggestion = "Recommended: 🧘 Gentle Yoga\nRestorative stretches to recharge.";
                break;
            case "stressed":
                suggestion = "Recommended: 🌿 Nature Walk + Breathing\nCalm your mind outdoors.";
                break;
            case "energetic":
                suggestion = "Recommended: 💪 Bodyweight Circuit\nFull-body energizing workout!";
                break;
            default:
                suggestion = "🚶‍♂️ Go for a refreshing walk!";
        }
        tv.setText(suggestion);
    }

    // Card click animation
    private void animateCardClick(View view) {
        view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100))
                .start();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}