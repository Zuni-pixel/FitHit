package com.example.fithit;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class HealthDashboardActivity extends AppCompatActivity implements SensorEventListener {

    private static final int REQUEST_ACTIVITY_RECOGNITION = 1;
    private static final int REQUEST_BODY_SENSORS = 2;

    private TextView tvSteps, tvHeartRate, tvBloodPressure;
    private BarChart stepChart;
    private LineChart heartRateChart;
    private MaterialButton btnAddData;

    private SensorManager sensorManager;
    private Sensor stepCounterSensor, heartRateSensor;
    private int stepCount = 0;
    private int heartRate = 0;
    private String bloodPressure = "120/80";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_dashboard);

        // Initialize views
        tvSteps = findViewById(R.id.tvSteps);
        tvHeartRate = findViewById(R.id.tvHeartRate);
        tvBloodPressure = findViewById(R.id.tvBloodPressure);
        stepChart = findViewById(R.id.stepChart);
        heartRateChart = findViewById(R.id.heartRateChart);
        btnAddData = findViewById(R.id.btnAddData);

        // Initialize sensor manager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Check and request permissions
        checkPermissions();

        // Setup charts
        setupStepChart();
        setupHeartRateChart();

        // Button click listener
        btnAddData.setOnClickListener(v -> {
            Intent intent = new Intent(HealthDashboardActivity.this, AddHealthDataActivity.class);
            startActivityForResult(intent, 100);
        });

        // Load sample data (replace with your actual data loading logic)
        loadSampleData();
    }

    private void checkPermissions() {
        // Check for activity recognition permission (for step counter)
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                    REQUEST_ACTIVITY_RECOGNITION);
        }

        // Check for body sensors permission (for heart rate)
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BODY_SENSORS},
                    REQUEST_BODY_SENSORS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ACTIVITY_RECOGNITION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                registerStepSensor();
            } else {
                Toast.makeText(this, "Step counter requires activity recognition permission", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_BODY_SENSORS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                registerHeartRateSensor();
            } else {
                Toast.makeText(this, "Heart rate monitoring requires body sensors permission", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void registerStepSensor() {
        if (sensorManager != null) {
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            if (stepCounterSensor != null) {
                sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                Toast.makeText(this, "No step counter sensor found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void registerHeartRateSensor() {
        if (sensorManager != null) {
            heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
            if (heartRateSensor != null) {
                sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                Toast.makeText(this, "No heart rate sensor found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupStepChart() {
        // Configure step chart appearance
        stepChart.getDescription().setEnabled(false);
        stepChart.setDrawGridBackground(false);
        stepChart.setDrawBarShadow(false);
        stepChart.setPinchZoom(false);
        stepChart.setDrawValueAboveBar(true);

        XAxis xAxis = stepChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(7);

        stepChart.getAxisLeft().setEnabled(true);
        stepChart.getAxisRight().setEnabled(false);
        stepChart.getLegend().setEnabled(false);
    }

    private void setupHeartRateChart() {
        // Configure heart rate chart appearance
        heartRateChart.getDescription().setEnabled(false);
        heartRateChart.setDrawGridBackground(false);
        heartRateChart.setPinchZoom(false);

        XAxis xAxis = heartRateChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(5);

        heartRateChart.getAxisLeft().setEnabled(true);
        heartRateChart.getAxisRight().setEnabled(false);
        heartRateChart.getLegend().setEnabled(false);
    }

    private void loadSampleData() {
        // Sample step data for last 7 days
        List<BarEntry> stepEntries = new ArrayList<>();
        stepEntries.add(new BarEntry(0, 4523));
        stepEntries.add(new BarEntry(1, 6872));
        stepEntries.add(new BarEntry(2, 5231));
        stepEntries.add(new BarEntry(3, 7890));
        stepEntries.add(new BarEntry(4, 6543));
        stepEntries.add(new BarEntry(5, 8321));
        stepEntries.add(new BarEntry(6, 5678));

        BarDataSet stepDataSet = new BarDataSet(stepEntries, "Steps");
        stepDataSet.setColor(getResources().getColor(R.color.primary));
        stepDataSet.setValueTextColor(getResources().getColor(R.color.black));
        stepDataSet.setValueTextSize(10f);

        BarData stepData = new BarData(stepDataSet);
        stepChart.setData(stepData);

        // Set day labels
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        stepChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(days));
        stepChart.invalidate();

        // Sample heart rate data
        List<Entry> hrEntries = new ArrayList<>();
        hrEntries.add(new Entry(0, 72));
        hrEntries.add(new Entry(1, 75));
        hrEntries.add(new Entry(2, 68));
        hrEntries.add(new Entry(3, 80));
        hrEntries.add(new Entry(4, 77));
        hrEntries.add(new Entry(5, 71));
        hrEntries.add(new Entry(6, 74));

        LineDataSet hrDataSet = new LineDataSet(hrEntries, "Heart Rate");
        hrDataSet.setColor(getResources().getColor(R.color.red));
        hrDataSet.setCircleColor(getResources().getColor(R.color.red));
        hrDataSet.setLineWidth(2f);
        hrDataSet.setCircleRadius(4f);
        hrDataSet.setValueTextColor(getResources().getColor(R.color.black));
        hrDataSet.setValueTextSize(10f);

        LineData hrData = new LineData(hrDataSet);
        heartRateChart.setData(hrData);
        heartRateChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(new String[]{"9AM", "12PM", "3PM", "6PM", "9PM", "12AM", "3AM"}));
        heartRateChart.invalidate();

        // Set today's values
        tvSteps.setText(String.valueOf(stepEntries.get(6).getY()));
        tvHeartRate.setText(String.valueOf(hrEntries.get(6).getY()));
        tvBloodPressure.setText(bloodPressure);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            stepCount = (int) event.values[0];
            tvSteps.setText(String.valueOf(stepCount));
        } else if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            heartRate = (int) event.values[0];
            tvHeartRate.setText(String.valueOf(heartRate));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle accuracy changes if needed
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-register sensors when activity resumes
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (heartRateSensor != null) {
            sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister sensors to save battery when activity is paused
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            // Handle manual data entry
            String bp = data.getStringExtra("blood_pressure");
            String hr = data.getStringExtra("heart_rate");

            if (bp != null) {
                bloodPressure = bp;
                tvBloodPressure.setText(bp);
            }
            if (hr != null) {
                heartRate = Integer.parseInt(hr);
                tvHeartRate.setText(hr);
            }
        }
    }
}