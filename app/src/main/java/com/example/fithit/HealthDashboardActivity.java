package com.example.fithit;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
    private static final String TAG = "HealthDashboard";

    private TextView tvSteps, tvHeartRate, tvBloodPressure;
    private BarChart stepChart;
    private LineChart heartRateChart;
    private MaterialButton btnAddData;

    private SensorManager sensorManager;
    private Sensor stepCounterSensor, heartRateSensor;
    private int stepCount = 0;
    private int heartRate = 0;
    private String bloodPressure = "120/80";
    private boolean sensorsAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // 1. First set the content view
            setContentView(R.layout.activity_health_dashboard);
            Log.d(TAG, "Layout inflated successfully");

            // 2. Initialize all UI components
            initializeBasicViews();

            // 3. Try to initialize advanced features (won't crash if fails)
            try {
                initializeAdvancedFeatures();
            } catch (Exception e) {
                Log.e(TAG, "Advanced features initialization failed", e);
                Toast.makeText(this, "Some features limited", Toast.LENGTH_LONG).show();
            }

            // 4. Always load sample data (works even without sensors)
            loadSampleData();

        } catch (Exception e) {
            Log.e(TAG, "Critical initialization failed", e);
            Toast.makeText(this, "Dashboard loaded with limited features", Toast.LENGTH_LONG).show();
            // Continue running the activity with basic functionality
        }
    }

    private void initializeBasicViews() {
        try {
            // Initialize all text views
            tvSteps = findViewById(R.id.tvSteps);
            tvHeartRate = findViewById(R.id.tvHeartRate);
            tvBloodPressure = findViewById(R.id.tvBloodPressure);

            // Set default values
            if (tvSteps != null) tvSteps.setText("0");
            if (tvHeartRate != null) tvHeartRate.setText("--");
            if (tvBloodPressure != null) tvBloodPressure.setText("--/--");

            // Initialize button
            btnAddData = findViewById(R.id.btnAddData);
            if (btnAddData != null) {
                btnAddData.setOnClickListener(v -> {
                    try {
                        startActivity(new Intent(this, AddHealthDataActivity.class));
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to start AddHealthDataActivity", e);
                        Toast.makeText(this, "Failed to open data entry", Toast.LENGTH_SHORT).show();
                    }
                });
            }

        } catch (Exception e) {
            Log.e(TAG, "Basic view initialization failed", e);
            Toast.makeText(this, "UI components loading failed", Toast.LENGTH_LONG).show();
        }
    }

    private void initializeAdvancedFeatures() {
        // 1. Initialize sensor manager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager == null) {
            Log.w(TAG, "Sensor service not available");
            Toast.makeText(this, "Sensor features unavailable", Toast.LENGTH_LONG).show();
            return;
        }

        // 2. Initialize charts
        stepChart = findViewById(R.id.stepChart);
        heartRateChart = findViewById(R.id.heartRateChart);
        setupCharts();

        // 3. Check and setup sensors
        checkSensorAvailability();
        checkPermissions();
    }

    private void checkSensorAvailability() {
        try {
            // Check step counter sensor
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            boolean hasStepCounter = (stepCounterSensor != null);

            // Check heart rate sensor
            heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
            boolean hasHeartRate = (heartRateSensor != null);

            sensorsAvailable = hasStepCounter || hasHeartRate;

            Log.d(TAG, "Step counter available: " + hasStepCounter);
            Log.d(TAG, "Heart rate sensor available: " + hasHeartRate);

            if (!hasStepCounter && tvSteps != null) {
                tvSteps.setText("Sensor not available");
            }
            if (!hasHeartRate && tvHeartRate != null) {
                tvHeartRate.setText("Sensor not available");
            }

        } catch (Exception e) {
            Log.e(TAG, "Sensor check failed", e);
            sensorsAvailable = false;
        }
    }

    private void setupCharts() {
        try {
            // Step Chart Configuration
            if (stepChart != null) {
                stepChart.getDescription().setEnabled(false);
                stepChart.setDrawGridBackground(false);
                stepChart.setDrawBarShadow(false);
                stepChart.setPinchZoom(false);
                stepChart.setDrawValueAboveBar(true);
                stepChart.setNoDataText("No step data available");

                XAxis xAxis = stepChart.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setDrawGridLines(false);
                xAxis.setGranularity(1f);
                xAxis.setLabelCount(7);

                stepChart.getAxisLeft().setEnabled(true);
                stepChart.getAxisRight().setEnabled(false);
                stepChart.getLegend().setEnabled(false);
            }

            // Heart Rate Chart Configuration
            if (heartRateChart != null) {
                heartRateChart.getDescription().setEnabled(false);
                heartRateChart.setDrawGridBackground(false);
                heartRateChart.setPinchZoom(false);
                heartRateChart.setNoDataText("No heart rate data available");

                XAxis xAxis = heartRateChart.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setDrawGridLines(false);
                xAxis.setGranularity(1f);
                xAxis.setLabelCount(5);

                heartRateChart.getAxisLeft().setEnabled(true);
                heartRateChart.getAxisRight().setEnabled(false);
                heartRateChart.getLegend().setEnabled(false);
            }

        } catch (Exception e) {
            Log.e(TAG, "Chart setup failed", e);
            Toast.makeText(this, "Chart display limited", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadSampleData() {
        try {
            // Sample step data for last 7 days
            List<BarEntry> stepEntries = new ArrayList<>();
            stepEntries.add(new BarEntry(0, 4523));
            stepEntries.add(new BarEntry(1, 6872));
            stepEntries.add(new BarEntry(2, 5231));
            stepEntries.add(new BarEntry(3, 7890));
            stepEntries.add(new BarEntry(4, 6543));
            stepEntries.add(new BarEntry(5, 8321));
            stepEntries.add(new BarEntry(6, 5678));

            if (stepChart != null) {
                BarDataSet stepDataSet = new BarDataSet(stepEntries, "Steps");
                stepDataSet.setColor(getResources().getColor(R.color.primary));
                stepDataSet.setValueTextColor(getResources().getColor(R.color.black));
                stepDataSet.setValueTextSize(10f);

                BarData stepData = new BarData(stepDataSet);
                stepChart.setData(stepData);

                String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
                stepChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(days));
                stepChart.invalidate();

                if (tvSteps != null) {
                    tvSteps.setText(String.valueOf(stepEntries.get(6).getY()));
                }
            }

            // Sample heart rate data
            List<Entry> hrEntries = new ArrayList<>();
            hrEntries.add(new Entry(0, 72));
            hrEntries.add(new Entry(1, 75));
            hrEntries.add(new Entry(2, 68));
            hrEntries.add(new Entry(3, 80));
            hrEntries.add(new Entry(4, 77));
            hrEntries.add(new Entry(5, 71));
            hrEntries.add(new Entry(6, 74));

            if (heartRateChart != null) {
                LineDataSet hrDataSet = new LineDataSet(hrEntries, "Heart Rate");
                hrDataSet.setColor(getResources().getColor(R.color.red));
                hrDataSet.setCircleColor(getResources().getColor(R.color.red));
                hrDataSet.setLineWidth(2f);
                hrDataSet.setCircleRadius(4f);
                hrDataSet.setValueTextColor(getResources().getColor(R.color.black));
                hrDataSet.setValueTextSize(10f);

                LineData hrData = new LineData(hrDataSet);
                heartRateChart.setData(hrData);
                heartRateChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(
                        new String[]{"9AM", "12PM", "3PM", "6PM", "9PM", "12AM", "3AM"}));
                heartRateChart.invalidate();

                if (tvHeartRate != null) {
                    tvHeartRate.setText(String.valueOf(hrEntries.get(6).getY()));
                }
            }

            if (tvBloodPressure != null) {
                tvBloodPressure.setText(bloodPressure);
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to load sample data", e);
            Toast.makeText(this, "Data display limited", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkPermissions() {
        try {
            if (sensorManager == null) return;

            // Only request permissions if sensors are available
            if (stepCounterSensor != null &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                            != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                        REQUEST_ACTIVITY_RECOGNITION);
            }

            if (heartRateSensor != null &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                            != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BODY_SENSORS},
                        REQUEST_BODY_SENSORS);
            }

            // Register listeners if we already have permissions
            registerSensorListeners();

        } catch (Exception e) {
            Log.e(TAG, "Permission check failed", e);
            Toast.makeText(this, "Sensor permissions error", Toast.LENGTH_SHORT).show();
        }
    }

    private void registerSensorListeners() {
        try {
            if (stepCounterSensor != null &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                            == PackageManager.PERMISSION_GRANTED) {
                sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }

            if (heartRateSensor != null &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                            == PackageManager.PERMISSION_GRANTED) {
                sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to register sensor listeners", e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        try {
            if (requestCode == REQUEST_ACTIVITY_RECOGNITION) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (stepCounterSensor != null && sensorManager != null) {
                        sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
                    }
                } else if (tvSteps != null) {
                    tvSteps.setText("Permission required");
                }
            } else if (requestCode == REQUEST_BODY_SENSORS) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (heartRateSensor != null && sensorManager != null) {
                        sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
                    }
                } else if (tvHeartRate != null) {
                    tvHeartRate.setText("Permission required");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Permission result handling failed", e);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        try {
            if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER && tvSteps != null) {
                stepCount = (int) event.values[0];
                tvSteps.setText(String.valueOf(stepCount));
            } else if (event.sensor.getType() == Sensor.TYPE_HEART_RATE && tvHeartRate != null) {
                heartRate = (int) event.values[0];
                tvHeartRate.setText(String.valueOf(heartRate));
            }
        } catch (Exception e) {
            Log.e(TAG, "Sensor data processing failed", e);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Optional accuracy handling
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            registerSensorListeners();
        } catch (Exception e) {
            Log.e(TAG, "Failed to resume sensors", e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (sensorManager != null) {
                sensorManager.unregisterListener(this);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to unregister sensors", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
                String bp = data.getStringExtra("blood_pressure");
                String hr = data.getStringExtra("heart_rate");

                if (bp != null && !bp.isEmpty() && tvBloodPressure != null) {
                    bloodPressure = bp;
                    tvBloodPressure.setText(bp);
                }
                if (hr != null && !hr.isEmpty() && tvHeartRate != null) {
                    heartRate = Integer.parseInt(hr);
                    tvHeartRate.setText(hr);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to process activity result", e);
            Toast.makeText(this, "Failed to update health data", Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean hasSensors(Context context) {
        if (context == null) return false;

        SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sm == null) return false;

        return sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null ||
                sm.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null;
    }
}