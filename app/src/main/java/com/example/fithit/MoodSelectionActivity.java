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

        // Add similar listeners for other cards...
    }

    // Suggest workout based on mood
    private void suggestWorkout(String mood, TextView tv) {
        String suggestion;
        switch (mood) {
            case "happy":
                suggestion = "Recommended: 💃 Dance Workout\nBoost your joy with fun cardio!";
                break;
            case "tired":
                suggestion = "Recommended: 🧘 Light Yoga\nGentle stretches to recharge.";
                break;
            case "stressed":
                suggestion = "Recommended: 🌿 Meditation + Walk\nCalm your mind.";
                break;
            case "energetic":
                suggestion = "Recommended: 🔥 HIIT Challenge\nBurn energy fast!";
                break;
            default:
                suggestion = "🚶‍♂️ Go for a walk!";
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
}