package com.example.fithit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class selectExerciseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_exercise);

        RadioGroup radioGroup = findViewById(R.id.radioGroup);
        Button nextButton = findViewById(R.id.selectExerciseButton);

        String[] options = getResources().getStringArray(R.array.exercises_for_posture);

        for (int i = 0; i < options.length; i++) {
            RadioButton radioButton = new RadioButton(this);
            radioButton.setText(options[i]);
            radioButton.setId(View.generateViewId()); // Generates unique ID
            radioGroup.addView(radioButton);
        }

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectedId = radioGroup.getCheckedRadioButtonId();

                if (selectedId != -1) { // Make sure something is selected
                    RadioButton selectedButton = findViewById(selectedId);
                    String selectedOption = selectedButton.getText().toString();

                    // Pass data using Intent
                    Intent intent = new Intent(selectExerciseActivity.this, PostureActivity.class);
                    intent.putExtra("selected_option", selectedOption);
                    startActivity(intent);
                } else {
                    Toast.makeText(selectExerciseActivity.this, "Please select an option", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
