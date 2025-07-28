package com.example.fithit;

import static android.content.Intent.getIntent;

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

import java.io.File;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

public class PostureActivity extends AppCompatActivity {
    private MainViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        findViewById(R.id.backBtn).setOnClickListener(v -> {finish();});
        findViewById(R.id.refreshBtn).setOnClickListener(v -> {
            viewModel.triggerRestart();
            recreate();
            Toast.makeText(this, "Posture Correction refreshed!", Toast.LENGTH_SHORT).show();
        });
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
    }

    private void restartExerciseExecution() {
        // Get the current exercise from ViewModel
        String exercise = viewModel.getCurrentExercise().getValue();
    }

    private void setupCompleteButton() {
        //button listener
        findViewById(R.id.btnComplete).setOnClickListener(v -> {
            viewModel.getExercisePercentage().observe(this, percentage -> {
                TextView tvReport = findViewById(R.id.btnComplete);
                String displayResult;

                if (percentage == 0f) {
                    displayResult = "Did not detect (╥‸╥)";
                } else if (percentage < 50f) {
                    displayResult = "Bad posture (๑﹏๑//)";
                } else if (percentage < 70f) {
                    displayResult = "Need to improve (˶ᵔ ᵕ ᵔ˶)";
                } else if (percentage >= 70f) {
                    displayResult = "PERFECT POSTURE ⸜(｡˃ ᵕ ˂ )⸝";
                }
                else {
                    displayResult = "Not exercising (x-x)";
                }

                tvReport.setText(displayResult);
            });
            ExerciseReport reportResults = viewModel.completeExercise();
            TextView detailsAvailable = findViewById(R.id.seeDetails);
//            if (reportResults.getOverallScore() == 0f) {
//                detailsAvailable.setText("No details available");
//            } else
            if (reportResults.getOverallScore() >= 70f){
                detailsAvailable.setText("No Improvement needed");
            } else if (reportResults.getOverallScore() < 70f && reportResults.getOverallScore() >= 0f){
                detailsAvailable.setText("See Detailed Feedback");
                if (reportResults.getOverallScore()!= 0f){
                    drawPoseDiagramToStorage(this, reportResults.getUserAngles(), "posture_user.png");
                    drawPoseDiagramToStorage(this, reportResults.getReferAngles(), "posture_correct.png");
                }
                findViewById(R.id.seeDetails).setOnClickListener(w -> {
                    viewModel.seeDetails(reportResults);
                });
            }
            else {
                detailsAvailable.setText("Do it seriously");
            }
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

    public static void drawPoseDiagramToStorage(Context context, Map<String, Float> angleValues, String filename) {
        // Segment lengths (in pixels)
        float shoulderAngle = angleValues.get("Shoulder_Angle");
        float elbowAngle = angleValues.get("Elbow_Angle");
        float hipAngle = angleValues.get("Hip_Angle");
        float kneeAngle = angleValues.get("Knee_Angle");

        float torsoLength = 200f;
        float upperArmLength = 150f;
        float forearmLength = 150f;
        float upperLegLength = 200f;
        float lowerLegLength = 200f;

        int width = 500;
        int height = 800;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        float originX = width / 2f;
        float originY = height / 2f;

        canvas.scale(1, -1);
        canvas.translate(0, -height);
        // Then center vertically and horizontally
        float offsetX = (width / 2f) - originX;
        float offsetY = (height / 2f) - (torsoLength / 2f + upperLegLength / 2f); // Adjust based on your drawing origin
        canvas.translate(offsetX, offsetY);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(8);
        paint.setStyle(Paint.Style.STROKE);

        Paint jointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        jointPaint.setColor(Color.BLACK);
        jointPaint.setStyle(Paint.Style.FILL);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(36f);

        // Joint positions
        PointF torsoTop = new PointF(originX, originY);
        PointF torsoBottom = getEndpoint(torsoTop.x, torsoTop.y, torsoLength, -90);

        PointF shoulder = torsoTop;
        PointF elbow = getEndpoint(shoulder.x, shoulder.y, upperArmLength, -90 + shoulderAngle);
        PointF forearmEnd = getEndpoint(elbow.x, elbow.y, forearmLength, -90 + shoulderAngle + (180 - elbowAngle));

        PointF hip = torsoBottom;
        PointF knee = getEndpoint(hip.x, hip.y, upperLegLength, -90 + (180 - hipAngle));
        PointF lowerLegEnd = getEndpoint(knee.x, knee.y, lowerLegLength, -90 + (180 - hipAngle + (180 - kneeAngle)));

        // Draw limbs
        paint.setColor(Color.BLACK);
        canvas.drawLine(torsoTop.x, torsoTop.y, torsoBottom.x, torsoBottom.y, paint);

        paint.setColor(Color.BLUE);
        canvas.drawLine(shoulder.x, shoulder.y, elbow.x, elbow.y, paint);

        paint.setColor(Color.CYAN);
        canvas.drawLine(elbow.x, elbow.y, forearmEnd.x, forearmEnd.y, paint);

        paint.setColor(Color.RED);
        canvas.drawLine(hip.x, hip.y, knee.x, knee.y, paint);

        paint.setColor(Color.MAGENTA);
        canvas.drawLine(knee.x, knee.y, lowerLegEnd.x, lowerLegEnd.y, paint);

        // Draw joints
        for (PointF point : new PointF[]{shoulder, elbow, forearmEnd, hip, knee, lowerLegEnd}) {
            canvas.drawCircle(point.x, point.y, 10f, jointPaint);
        }

        // Draw title
//        canvas.drawText("Pose Diagram (Front View)", 50f, 50f, textPaint);

        // Save to internal storage
        try {
            File file = new File(context.getFilesDir(), filename);
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            System.out.println("Saved image to: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //This is the helper function you must define OUTSIDE the main function
    public static PointF getEndpoint(float x, float y, float length, float angleDeg) {
        float rad = (float) Math.toRadians(angleDeg);
        return new PointF(x + (float) Math.cos(rad) * length, y + (float) Math.sin(rad) * length);
    }
}
