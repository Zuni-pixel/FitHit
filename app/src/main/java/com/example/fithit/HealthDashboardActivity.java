package com.example.fithit;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class HealthDashboardActivity extends AppCompatActivity implements SensorEventListener {

    // Health Metrics - All initialized to zero/empty
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
    private Button btnAddManual, btnDailySummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_dashboard);

        initViews();
        setupSensors();
        updateAllMetrics();
        setupButtons();
    }

    private void initViews() {
        tvSteps = findViewById(R.id.tvSteps);
        tvWater = findViewById(R.id.tvWater);
        tvCalories = findViewById(R.id.tvCalories);
        tvHeartRate = findViewById(R.id.tvHeartRate);
        tvSleep = findViewById(R.id.tvSleep);
        tvBloodPressure = findViewById(R.id.tvBloodPressure);

        btnAddManual = findViewById(R.id.btnAddManual);
        btnDailySummary = findViewById(R.id.btnDailySummary);
    }

    private void setupSensors() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        if (heartRateSensor != null) {
            sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void updateAllMetrics() {
        tvSteps.setText(String.valueOf(steps));
        tvWater.setText(String.valueOf(waterGlasses));
        tvCalories.setText(String.valueOf(calories));
        tvHeartRate.setText(heartRate == 0 ? "--" : String.valueOf(heartRate));
        tvSleep.setText(sleepHours == 0 ? "--" : String.format("%.1f", sleepHours));
        tvBloodPressure.setText(bloodPressure);

        // Auto-calculate calories only if steps exist
        if (steps > 0) {
            calories = (int)(steps * 0.04);
        }
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

        // Steps
        summary.append("Steps: ").append(steps);
        if (steps > 0) {
            summary.append("\n- ").append(getStepsAnalysis());
        }
        summary.append("\n");

        // Water
        summary.append("Water Intake: ").append(waterGlasses).append(" glasses");
        if (waterGlasses > 0) {
            summary.append("\n- ").append(getWaterAnalysis());
        }
        summary.append("\n");

        // Calories
        summary.append("Calories Burned: ").append(calories).append("\n");

        // Heart Rate
        if (heartRate > 0) {
            summary.append("\nHeart Rate: ").append(heartRate).append(" BPM");
            summary.append("\n- ").append(getHeartRateAnalysis());
        }

        // Sleep
        if (sleepHours > 0) {
            summary.append("\n\nSleep: ").append(String.format("%.1f", sleepHours)).append(" hours");
            summary.append("\n- ").append(getSleepAnalysis());
        }

        // Blood Pressure
        if (!bloodPressure.equals("--/--")) {
            summary.append("\n\nBlood Pressure: ").append(bloodPressure).append(" mmHg");
            summary.append("\n- ").append(getBPAnalysis());
        }

        new AlertDialog.Builder(this)
                .setTitle("Daily Health Report")
                .setMessage(summary)
                .setPositiveButton("OK", null)
                .show();
    }

    private String getStepsAnalysis() {
        if (steps < 3000) return "Try to walk more tomorrow";
        if (steps < 6000) return "Good start!";
        if (steps < 10000) return "Great job! Keep it up";
        return "Excellent activity level!";
    }

    private String getWaterAnalysis() {
        if (waterGlasses < 4) return "Try to drink more water";
        if (waterGlasses < 6) return "Good hydration";
        return "Perfect water intake";
    }

    private String getHeartRateAnalysis() {
        if (heartRate < 60) return "Healthy resting heart rate";
        if (heartRate < 100) return "Normal heart rate";
        return "Consider checking with your doctor";
    }

    private String getSleepAnalysis() {
        if (sleepHours < 6) return "Try to get more sleep";
        if (sleepHours < 7) return "Almost recommended amount";
        if (sleepHours < 9) return "Healthy sleep duration";
        return "Well rested!";
    }

    private String getBPAnalysis() {
        try {
            String[] bp = bloodPressure.split("/");
            if (bp.length != 2) return "Check measurement";

            int systolic = Integer.parseInt(bp[0]);
            int diastolic = Integer.parseInt(bp[1]);

            if (systolic < 90 || diastolic < 60) return "Low - consult your doctor";
            if (systolic < 120 && diastolic < 80) return "Normal blood pressure";
            if (systolic < 140 || diastolic < 90) return "Slightly elevated - monitor";
            return "High - medical advice recommended";
        } catch (Exception e) {
            return "Invalid measurement";
        }
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
        // Handle accuracy changes if needed
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