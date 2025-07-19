package com.example.fithit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

public class AddHealthDataActivity extends AppCompatActivity {

    private EditText etSystolic, etDiastolic, etHeartRate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_health_data);

        etSystolic = findViewById(R.id.etSystolic);
        etDiastolic = findViewById(R.id.etDiastolic);
        etHeartRate = findViewById(R.id.etHeartRate);
        Button btnSave = findViewById(R.id.btnSave);

        btnSave.setOnClickListener(v -> saveData());
    }

    private void saveData() {
        String systolic = etSystolic.getText().toString().trim();
        String diastolic = etDiastolic.getText().toString().trim();
        String heartRate = etHeartRate.getText().toString().trim();

        if (systolic.isEmpty() || diastolic.isEmpty()) {
            Toast.makeText(this, "Please enter blood pressure values", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra("blood_pressure", systolic + "/" + diastolic);

        if (!heartRate.isEmpty()) {
            resultIntent.putExtra("heart_rate", heartRate);
        }

        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
}