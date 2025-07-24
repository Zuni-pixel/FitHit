package com.example.fithit;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fithit.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private EditText usernameField, emailField, passwordField;
    private CheckBox termsCheckbox;
    private Button signupButton;
    private TextView loginText, termsLink;

    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase Auth and Database
        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        // Initialize views
        usernameField = findViewById(R.id.usernameField);
        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        termsCheckbox = findViewById(R.id.termsCheckbox);
        signupButton = findViewById(R.id.signupButton);
        loginText = findViewById(R.id.loginText);
        termsLink = findViewById(R.id.termsLink);

        // Signup Button
        signupButton.setOnClickListener(v -> handleSignup());

        // Go to Login Page
        loginText.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        // Terms & Conditions Link
        termsLink.setOnClickListener(v -> showTermsAndConditions());
    }

    private void showTermsAndConditions() {
        // Create a simple dialog or start a new activity to show terms
        // For simplicity, we'll show a dialog with basic terms
        new android.app.AlertDialog.Builder(this)
                .setTitle("Terms & Conditions")
                .setMessage("By using FitHit, you agree to:\n\n" +
                        "1. Use the app for personal fitness purposes only\n" +
                        "2. Provide accurate health information\n" +
                        "3. Not share your account with others\n" +
                        "4. Accept all risks associated with exercise\n" +
                        "5. Consult a physician before starting any new fitness program\n\n" +
                        "We reserve the right to modify these terms at any time.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void handleSignup() {
        String username = usernameField.getText().toString().trim();
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!termsCheckbox.isChecked()) {
            Toast.makeText(this, "Please accept the Terms & Conditions!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if username already exists
        checkUsernameAvailability(username, available -> {
            if (available) {
                proceedWithSignup(username, email, password);
            } else {
                Toast.makeText(SignupActivity.this,
                        "Username already taken. Please choose another one.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void proceedWithSignup(String username, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Set Display Name in Firebase Auth
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(username)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            // Save user data in existing Users table
                                            saveUserData(user.getUid(), username, email);
                                        } else {
                                            Toast.makeText(SignupActivity.this,
                                                    "Failed to update profile: " + profileTask.getException().getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    } else {
                        Toast.makeText(SignupActivity.this,
                                "Signup failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUsernameAvailability(String username, UsernameCheckCallback callback) {
        usersRef.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        callback.onResult(!dataSnapshot.exists());
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(SignupActivity.this,
                                "Error checking username availability",
                                Toast.LENGTH_SHORT).show();
                        callback.onResult(false);
                    }
                });
    }

    private void saveUserData(String uid, String username, String email) {
        // Create user data map - add only new fields to avoid overwriting existing ones
        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("username", username);
        userUpdates.put("email", email);

        // Update the existing Users table without overwriting other fields
        usersRef.child(uid).updateChildren(userUpdates)
                .addOnSuccessListener(aVoid -> {
                    // Send email verification after successful data save
                    sendEmailVerification(mAuth.getCurrentUser());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SignupActivity.this,
                            "Failed to save user data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void sendEmailVerification(FirebaseUser user) {
        if (user == null) return;

        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(SignupActivity.this,
                                "Verification email sent to " + user.getEmail() + ". Please verify before logging in.",
                                Toast.LENGTH_LONG).show();
                        mAuth.signOut(); // Sign out after sending verification

                        // Go to login screen
                        Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(SignupActivity.this,
                                "Failed to send verification email: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    interface UsernameCheckCallback {
        void onResult(boolean available);
    }
}