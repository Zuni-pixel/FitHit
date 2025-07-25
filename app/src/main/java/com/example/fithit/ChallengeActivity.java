package com.example.fithit;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;

public class ChallengeActivity extends AppCompatActivity {

    private EditText etOpponentEmail;
    private Button btnSendChallenge;
    private TextView tvResult;

    private DatabaseReference leaderboardRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge);

        etOpponentEmail = findViewById(R.id.etOpponentEmail);
        btnSendChallenge = findViewById(R.id.btnSendChallenge);
        tvResult = findViewById(R.id.tvResult);

        leaderboardRef = FirebaseDatabase.getInstance().getReference("Leaderboard");

        btnSendChallenge.setOnClickListener(v -> {
            String opponentEmail = etOpponentEmail.getText().toString().trim();
            if (!opponentEmail.isEmpty()) {
                challengeFriend(opponentEmail);
            } else {
                tvResult.setText("Please enter opponent's email.");
            }
        });
    }

    private void challengeFriend(String opponentEmail) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            tvResult.setText("User not logged in.");
            return;
        }

        String myEmail = currentUser.getEmail();
        String myKey = myEmail.replace(".", "_");
        String opponentKey = opponentEmail.replace(".", "_");

        leaderboardRef.child(myKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot mySnap) {
                leaderboardRef.child(opponentKey).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot opSnap) {
                        if (mySnap.exists() && opSnap.exists()) {
                            int myExercises = mySnap.child("exercisesCompleted").getValue(Integer.class);
                            int opponentExercises = opSnap.child("exercisesCompleted").getValue(Integer.class);

                            String myUser = myEmail.split("@")[0];
                            String opponentUser = opponentEmail.split("@")[0];

                            String winner;
                            if (myExercises > opponentExercises) {
                                winner = myUser;
                            } else if (opponentExercises > myExercises) {
                                winner = opponentUser;
                            } else {
                                winner = "Draw";
                            }

                            // Save to Firebase under "Challenges"
                            DatabaseReference challengeRef = FirebaseDatabase.getInstance().getReference("Challenges");
                            String challengeKey = myUser + "_vs_" + opponentUser;

                            Map<String, Object> challengeData = new HashMap<>();
                            challengeData.put("challenger", myUser);
                            challengeData.put("opponent", opponentUser);
                            challengeData.put("challengerExercises", myExercises);
                            challengeData.put("opponentExercises", opponentExercises);
                            challengeData.put("winner", winner);

                            challengeRef.child(challengeKey).setValue(challengeData);

                            // Show result
                            String msg = "You: " + myExercises + "\n" + opponentUser + ": " + opponentExercises + "\n\nWinner: " + winner;
                            tvResult.setText(msg);
                        } else {
                            tvResult.setText("One of the users is not on leaderboard.");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        tvResult.setText("Error reading opponent data.");
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvResult.setText("Error reading your data.");
            }
        });
    }
}

