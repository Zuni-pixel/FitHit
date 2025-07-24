package com.example.fithit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class AddHealthDataActivity extends AppCompatActivity {

    private TextInputEditText etSteps, etWater, etHeartRate, etSleep, etSystolic, etDiastolic;
    private MaterialButton btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_health_data);

        initViews();
        loadCurrentValues();
        setupSaveButton();
    }

    private void initViews() {
        etSteps = findViewById(R.id.etSteps);
        etWater = findViewById(R.id.etWater);
        etHeartRate = findViewById(R.id.etHeartRate);
        etSleep = findViewById(R.id.etSleep);
        etSystolic = findViewById(R.id.etSystolic);
        etDiastolic = findViewById(R.id.etDiastolic);
        btnSave = findViewById(R.id.btnSave);
    }

    private void loadCurrentValues() {
        Intent intent = getIntent();

        // Only show current values if they exist
        int currentSteps = intent.getIntExtra("currentSteps", 0);
        if (currentSteps > 0) etSteps.setHint(String.valueOf(currentSteps));

        int currentWater = intent.getIntExtra("currentWater", 0);
        if (currentWater > 0) etWater.setHint(String.valueOf(currentWater));

        int currentHR = intent.getIntExtra("currentHeartRate", 0);
        if (currentHR > 0) etHeartRate.setHint(String.valueOf(currentHR));

        float currentSleep = intent.getFloatExtra("currentSleep", 0);
        if (currentSleep > 0) etSleep.setHint(String.valueOf(currentSleep));

        String bp = intent.getStringExtra("currentBloodPressure");
        if (bp != null && !bp.equals("--/--")) {
            String[] parts = bp.split("/");
            if (parts.length == 2) {
                etSystolic.setHint(parts[0]);
                etDiastolic.setHint(parts[1]);
            }
        }
    }

    private void setupSaveButton() {
        btnSave.setOnClickListener(v -> {
            if (!validateInputs()) return;

            Intent result = new Intent();
            result.putExtra("steps", getValidatedSteps());
            result.putExtra("water", getValidatedWater());
            result.putExtra("heartRate", getValidatedHeartRate());
            result.putExtra("sleep", getValidatedSleep());
            result.putExtra("bloodPressure", getValidatedBP());

            setResult(RESULT_OK, result);
            finish();
        });
    }

    private boolean validateInputs() {
        try {
            // Validate Steps
            if (!etSteps.getText().toString().isEmpty()) {
                int steps = Integer.parseInt(etSteps.getText().toString());
                if (steps < 0 || steps > 100000) {
                    etSteps.setError("Enter 0-100,000 steps");
                    return false;
                }
            }

            // Validate Water
            if (!etWater.getText().toString().isEmpty()) {
                int water = Integer.parseInt(etWater.getText().toString());
                if (water < 0 || water > 30) {
                    etWater.setError("Enter 0-30 glasses");
                    return false;
                }
            }

            // Validate Heart Rate
            if (!etHeartRate.getText().toString().isEmpty()) {
                int hr = Integer.parseInt(etHeartRate.getText().toString());
                if (hr < 30 || hr > 250) {
                    etHeartRate.setError("Enter 30-250 BPM");
                    return false;
                }
            }

            // Validate Sleep
            if (!etSleep.getText().toString().isEmpty()) {
                float sleep = Float.parseFloat(etSleep.getText().toString());
                if (sleep < 0 || sleep > 24) {
                    etSleep.setError("Enter 0-24 hours");
                    return false;
                }
            }

            // Validate Blood Pressure
            boolean hasSystolic = !etSystolic.getText().toString().isEmpty();
            boolean hasDiastolic = !etDiastolic.getText().toString().isEmpty();

            if (hasSystolic || hasDiastolic) {
                int sys = hasSystolic ? Integer.parseInt(etSystolic.getText().toString()) : 0;
                int dia = hasDiastolic ? Integer.parseInt(etDiastolic.getText().toString()) : 0;

                if (sys < 50 || sys > 300) {
                    etSystolic.setError("Invalid systolic (50-300)");
                    return false;
                }
                if (dia < 30 || dia > 200) {
                    etDiastolic.setError("Invalid diastolic (30-200)");
                    return false;
                }
                if (sys < dia) {
                    etSystolic.setError("Systolic must be ≥ diastolic");
                    return false;
                }
            }

            return true;
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private int getValidatedSteps() {
        return etSteps.getText().toString().isEmpty() ?
                (etSteps.getHint() == null ? 0 : Integer.parseInt(etSteps.getHint().toString())) :
                Integer.parseInt(etSteps.getText().toString());
    }

    private int getValidatedWater() {
        return etWater.getText().toString().isEmpty() ?
                (etWater.getHint() == null ? 0 : Integer.parseInt(etWater.getHint().toString())) :
                Integer.parseInt(etWater.getText().toString());
    }

    private int getValidatedHeartRate() {
        return etHeartRate.getText().toString().isEmpty() ?
                (etHeartRate.getHint() == null ? 0 : Integer.parseInt(etHeartRate.getHint().toString())) :
                Integer.parseInt(etHeartRate.getText().toString());
    }

    private float getValidatedSleep() {
        return etSleep.getText().toString().isEmpty() ?
                (etSleep.getHint() == null ? 0 : Float.parseFloat(etSleep.getHint().toString())) :
                Float.parseFloat(etSleep.getText().toString());
    }

    private String getValidatedBP() {
        String sys = etSystolic.getText().toString().isEmpty() ?
                (etSystolic.getHint() == null ? "" : etSystolic.getHint().toString()) :
                etSystolic.getText().toString();

        String dia = etDiastolic.getText().toString().isEmpty() ?
                (etDiastolic.getHint() == null ? "" : etDiastolic.getHint().toString()) :
                etDiastolic.getText().toString();

        if (sys.isEmpty() && dia.isEmpty()) return "--/--";
        if (sys.isEmpty()) sys = "0";
        if (dia.isEmpty()) dia = "0";
        return sys + "/" + dia;
    }
}