package com.example.fithit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;

public class PostureActivity extends AppCompatActivity {
    private MainViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //EdgeToEdge.enable(this);
        setContentView(R.layout.activity_posture_detect);
        Intent intent = getIntent();
        String selectedExercise = intent.getStringExtra("selected_option");

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "USER NULL IN POSTURE ACTIVITY", Toast.LENGTH_SHORT).show();
        }

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        TextView exerciseView = findViewById(R.id.posture_exercise);
        exerciseView.setText(selectedExercise);

        observeViewModel();
        setupCompleteButton();
        //setupNavigator();

        assert selectedExercise != null;
        viewModel.setExercise(this, selectedExercise);
    }

    private void observeViewModel() {
        viewModel.getRestartExercise().observe(this, restart -> {
            if (restart != null && restart) {
                restartExerciseExecution();
            }
        });

        viewModel.getFullExerciseReport().observe(this, report-> {
            if (report !=null && !report.isEmpty()){
                //Start ResultsActivity with the report
                Intent intent = new Intent(PostureActivity.this, ResultsActivity.class);
                intent.putExtra("REPORT", report);
                startActivity(intent);

                //clear report after showing
                viewModel.clearExerciseReport();

            }
        });

        viewModel.getExercisePercentage().observe(this, percentage-> {
            TextView tvReport = findViewById(R.id.btnComplete);
            String displayResult = "Score: " + percentage + "%";
            tvReport.setText(displayResult);
        });
    }

    private void restartExerciseExecution() {
        // Get the current exercise from ViewModel
        String exercise = viewModel.getCurrentExercise().getValue();
    }

    private void setupCompleteButton() {
        //button listener
        findViewById(R.id.btnComplete).setOnClickListener(v -> {
            ExerciseReport reportResults = viewModel.completeExercise();
            TextView detailsAvailable = findViewById(R.id.seeDetails);
            detailsAvailable.setText("See Detailed Feedback");
            findViewById(R.id.seeDetails).setOnClickListener(w -> {
                viewModel.seeDetails(reportResults);
            });
        });
        viewModel.resetExerciseTracking();
    }

    private void setupNavigator(){
        // Get the NavHostFragment and NavController
        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    //finish();
                }
            });
        } else {
            // Handle error: Log or show a message
            throw new IllegalStateException("NavHostFragment not found in layout");
        }
    }
}
