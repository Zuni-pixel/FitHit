package com.example.fithit;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class HealthDashboardActivity extends AppCompatActivity implements SensorEventListener {

    // Health Metrics
    private int steps = 0;
    private int waterGlasses = 0;
    private int calories = 0;
    private int heartRate = 0;
    private float sleepHours = 0;
    private String bloodPressure = "--/--";

    // Sensors
    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private Sensor heartRateSensor;

    // UI Components
    private TextView tvSteps, tvWater, tvCalories, tvHeartRate, tvSleep, tvBloodPressure;
    private TextView tvActivityStatus, tvMotivationalQuote;
    private ProgressBar pbActivityProgress;
    private Button btnAddManual, btnDailySummary;

    // Activity tracking
    private int dailyGoal = 10000;
    private String[] motivationalQuotes = {
            "Every step counts! Keep going!",
            "You're closer to your goal than yesterday!",
            "Small progress is still progress!",
            "Your health is your wealth!",
            "Stay hydrated and keep moving!"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_dashboard);

        initViews();
        setupSensors();
        updateAllMetrics();
        setupButtons();
        startMotivationalQuoteCycle();
    }

    private void initViews() {
        tvSteps = findViewById(R.id.tvSteps);
        tvWater = findViewById(R.id.tvWater);
        tvCalories = findViewById(R.id.tvCalories);
        tvHeartRate = findViewById(R.id.tvHeartRate);
        tvSleep = findViewById(R.id.tvSleep);
        tvBloodPressure = findViewById(R.id.tvBloodPressure);

        // New interactive elements
        tvActivityStatus = findViewById(R.id.tvActivityStatus);
        tvMotivationalQuote = findViewById(R.id.tvMotivationalQuote);
        pbActivityProgress = findViewById(R.id.pbActivityProgress);

        btnAddManual = findViewById(R.id.btnAddManual);
        btnDailySummary = findViewById(R.id.btnDailySummary);
    }

    private void setupSensors() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            tvActivityStatus.setText("Step counter not available");
        }

        if (heartRateSensor != null) {
            sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void updateAllMetrics() {
        tvSteps.setText(String.valueOf(steps));
        tvWater.setText(String.valueOf(waterGlasses));

        // Auto-calculate calories (approx 0.04 calories per step)
        calories = (int)(steps * 0.04);
        tvCalories.setText(String.valueOf(calories));

        tvHeartRate.setText(heartRate == 0 ? "--" : String.valueOf(heartRate));
        tvSleep.setText(sleepHours == 0 ? "--" : String.format("%.1f", sleepHours));
        tvBloodPressure.setText(bloodPressure);

        // Update activity progress
        updateActivityStatus();
    }

    private void updateActivityStatus() {
        int progress = (int)((steps * 100f) / dailyGoal);
        pbActivityProgress.setProgress(progress > 100 ? 100 : progress);

        if (steps == 0) {
            tvActivityStatus.setText("Start moving to begin tracking!");
            pbActivityProgress.setProgressTintList(ContextCompat.getColorStateList(this, R.color.progress_low));
        } else if (steps < dailyGoal/2) {
            tvActivityStatus.setText("Keep going! You're making progress");
            pbActivityProgress.setProgressTintList(ContextCompat.getColorStateList(this, R.color.progress_medium));
        } else if (steps < dailyGoal) {
            tvActivityStatus.setText("Almost there! You can do it!");
            pbActivityProgress.setProgressTintList(ContextCompat.getColorStateList(this, R.color.progress_high));
        } else {
            tvActivityStatus.setText("Goal achieved! Excellent work!");
            pbActivityProgress.setProgressTintList(ContextCompat.getColorStateList(this, R.color.progress_complete));
        }
    }

    private void startMotivationalQuoteCycle() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            int currentIndex = 0;
            @Override
            public void run() {
                tvMotivationalQuote.setText(motivationalQuotes[currentIndex]);
                currentIndex = (currentIndex + 1) % motivationalQuotes.length;
                handler.postDelayed(this, 10000); // Change every 10 seconds
            }
        }, 1000);
    }

    private void setupButtons() {
        btnAddManual.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddHealthDataActivity.class);
            intent.putExtra("currentSteps", steps);
            intent.putExtra("currentWater", waterGlasses);
            intent.putExtra("currentHeartRate", heartRate);
            intent.putExtra("currentSleep", sleepHours);
            intent.putExtra("currentBloodPressure", bloodPressure);
            startActivityForResult(intent, 1);
        });

        btnDailySummary.setOnClickListener(v -> generateDailySummary());
    }

    private void generateDailySummary() {
        StringBuilder summary = new StringBuilder("Today's Health Summary:\n\n");

        summary.append("Steps: ").append(steps).append(" (")
                .append(String.format("%.1f%%", (steps * 100f) / dailyGoal)).append(" of goal)\n");
        summary.append("Water Intake: ").append(waterGlasses).append(" glasses\n");
        summary.append("Calories Burned: ").append(calories).append("\n");

        if (heartRate > 0) {
            summary.append("\nHeart Rate: ").append(heartRate).append(" BPM\n");
        }

        if (sleepHours > 0) {
            summary.append("Sleep: ").append(String.format("%.1f", sleepHours)).append(" hours\n");
        }

        if (!bloodPressure.equals("--/--")) {
            summary.append("\nBlood Pressure: ").append(bloodPressure).append(" mmHg\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("Daily Health Report")
                .setMessage(summary)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            steps = data.getIntExtra("steps", steps);
            waterGlasses = data.getIntExtra("water", waterGlasses);
            heartRate = data.getIntExtra("heartRate", heartRate);
            sleepHours = data.getFloatExtra("sleep", sleepHours);
            bloodPressure = data.getStringExtra("bloodPressure");

            updateAllMetrics();
            Toast.makeText(this, "Health data updated!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            steps = (int) event.values[0];
            updateAllMetrics();
        } else if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            heartRate = (int) event.values[0];
            updateAllMetrics();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle accuracy changes
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (heartRateSensor != null) {
            sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }
}