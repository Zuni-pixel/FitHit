package com.example.fithit;

import static com.example.fithit.PoseComparisonKt.changeLongPressValue;
import static com.example.fithit.PoseComparisonKt.notLongPress;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

public class ResultsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);
        TextView tvReport = findViewById(R.id.tvReport);

        if (notLongPress()){
            ImageView userImage = findViewById(R.id.posture1);
            ImageView datasetImage = findViewById(R.id.posture2);

            File userFile = new File(getFilesDir(), "posture_user.png");
            File dataFile = new File(getFilesDir(), "posture_correct.png");
            if (userFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(userFile.getAbsolutePath());
                if (bitmap != null) {
                    userImage.setImageBitmap(bitmap);
                }
            }
            if (dataFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(dataFile.getAbsolutePath());
                if (bitmap != null) {
                    datasetImage.setImageBitmap(bitmap);
                }
            }
        }

        changeLongPressValue(false);

        findViewById(R.id.detailButton).setOnClickListener(v ->{
            finish();
        });

        findViewById(R.id.detailButton).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v){
                //Get report from intent
                String report = getIntent().getStringExtra("REPORT");
                if (report != null){
                    tvReport.setText(report);
                } else {
                    tvReport.setText("No report available");
                }
                return true;
            }
        });
    }
}
