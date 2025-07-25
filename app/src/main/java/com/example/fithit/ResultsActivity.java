package com.example.fithit;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class ResultsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);
        TextView tvReport = findViewById(R.id.tvReport);

        //Get report from intent
        String report = getIntent().getStringExtra("REPORT");
        if (report != null){
            tvReport.setText(report);
        } else {
            tvReport.setText("No report available");
        }
    }
}
