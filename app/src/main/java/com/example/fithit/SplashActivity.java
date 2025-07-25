package com.example.fithit;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;

public class SplashActivity extends AppCompatActivity {

    private TextView skipButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize Views
        skipButton = findViewById(R.id.skipButton);


        // Skip Button Click Listener
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to another activity or perform action
                Intent intent = new Intent(SplashActivity.this, SignActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Next Button Click Listener
        findViewById(R.id.nextButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SplashActivity.this, SignActivity.class);
                startActivity(intent);
                finish();
            }

        });
    }
}