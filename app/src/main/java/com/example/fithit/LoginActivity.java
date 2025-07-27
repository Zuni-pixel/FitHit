package com.example.fithit;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private TextView signupLink, forgotPassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.loginButton);
        signupLink = findViewById(R.id.signupLink);
        forgotPassword = findViewById(R.id.forgotPassword);

        loginButton.setOnClickListener(view -> loginUser());

        signupLink.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
            finish();
        });

        forgotPassword.setOnClickListener(view -> verifyAndResetPassword());
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            if (user.isEmailVerified()) {
                                String uid = user.getUid();

                                FirebaseDatabase.getInstance().getReference("Users").child(uid)
                                        .get().addOnSuccessListener(snapshot -> {
                                            if (snapshot.exists()) {
                                                String height = snapshot.child("height").getValue(String.class);
                                                String age = snapshot.child("age").getValue(String.class);
                                                String gender = snapshot.child("gender").getValue(String.class);
                                                String diseases = snapshot.child("diseases").getValue(String.class);
                                                String goal = snapshot.child("goal").getValue(String.class);

                                                if (height != null && age != null && gender != null && diseases != null && goal != null) {
                                                    // ✅ All profile data exists → go to Home
                                                    Toast.makeText(LoginActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                                                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                                    startActivity(intent);
                                                    finish();
                                                } else {
                                                    // ❌ Incomplete profile data → go to profile input
                                                    Intent intent = new Intent(LoginActivity.this, HeightDiseasesActivity.class);
                                                    startActivity(intent);
                                                    finish();
                                                }
                                            } else {
                                                Toast.makeText(LoginActivity.this, "User data not found.", Toast.LENGTH_SHORT).show();
                                            }
                                        }).addOnFailureListener(e -> {
                                            Toast.makeText(LoginActivity.this, "Failed to read user data.", Toast.LENGTH_SHORT).show();
                                        });

                            } else {
                                user.sendEmailVerification().addOnCompleteListener(verifyTask -> {
                                    if (verifyTask.isSuccessful()) {
                                        Toast.makeText(LoginActivity.this,
                                                "Email not verified. Verification email sent to " + user.getEmail(),
                                                Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(LoginActivity.this, "Failed to send verification email.",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                                mAuth.signOut(); // Logout unverified user
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Error: User does not exist.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(LoginActivity.this, "Authentication failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void verifyAndResetPassword() {
        String email = emailEditText.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Enter your registered email", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        boolean isRegistered = !task.getResult().getSignInMethods().isEmpty();

                        if (isRegistered) {
                            sendPasswordResetEmail(email);
                        } else {
                            Toast.makeText(LoginActivity.this, "Email is not registered", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(LoginActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Reset link sent to your email", Toast.LENGTH_SHORT).show();
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(LoginActivity.this, "Failed to send reset email: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }
}