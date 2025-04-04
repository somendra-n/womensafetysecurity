package com.example.womensafetyapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private EditText etUsername, etPhoneNumber, etEmergencyContact1, etEmergencyContact2, etOTP;
    private Button btnRegister, btnVerifyOTP;
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;
    private String verificationId;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase Authentication and Realtime Database
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();

        // Initialize UI components
        etUsername = findViewById(R.id.etUsername);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etEmergencyContact1 = findViewById(R.id.etEmergencyContact1);
        etEmergencyContact2 = findViewById(R.id.etEmergencyContact2);
        etOTP = findViewById(R.id.etOTP);
        btnRegister = findViewById(R.id.btnRegister);
        btnVerifyOTP = findViewById(R.id.btnVerifyOTP);
        progressBar = findViewById(R.id.progressBar);

        // Initially hide OTP EditText and Verify OTP Button
        etOTP.setVisibility(EditText.GONE);
        btnVerifyOTP.setVisibility(Button.GONE);

        // Set up the register button click listener
        btnRegister.setOnClickListener(v -> registerUser());
        btnVerifyOTP.setOnClickListener(v -> verifyOTP());
    }

    // Register the user by sending OTP to the phone number
    private void registerUser() {
        String username = etUsername.getText().toString().trim();
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        String emergencyContact1 = etEmergencyContact1.getText().toString().trim();
        String emergencyContact2 = etEmergencyContact2.getText().toString().trim();

        // Validate input fields
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(phoneNumber) ||
                TextUtils.isEmpty(emergencyContact1) || TextUtils.isEmpty(emergencyContact2)) {
            Toast.makeText(RegisterActivity.this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Send OTP to the phone number
        sendOTP(phoneNumber);
    }

    // Send OTP to the user's phone number using PhoneAuthOptions
    private void sendOTP(String phoneNumber) {
        progressBar.setVisibility(ProgressBar.VISIBLE);

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber("+91" + phoneNumber)       // Phone number with country code
                        .setTimeout(60L, TimeUnit.SECONDS)         // Timeout duration
                        .setActivity(this)                         // Activity context
                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                            @Override
                            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                                Log.d(TAG, "onVerificationCompleted: OTP automatically detected");
                                // Directly call verifyOTPWithCredential with the received credential
                                verifyOTPWithCredential(phoneAuthCredential);
                            }

                            @Override
                            public void onVerificationFailed(FirebaseException e) {
                                Log.e(TAG, "onVerificationFailed: " + e.getMessage());
                                Toast.makeText(RegisterActivity.this, "OTP verification failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(ProgressBar.GONE);  // Hide progress bar
                            }

                            @Override
                            public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                                RegisterActivity.this.verificationId = verificationId;

                                etOTP.setVisibility(EditText.VISIBLE);
                                btnVerifyOTP.setVisibility(Button.VISIBLE);
                                Log.d(TAG, "onCodeSent: OTP sent successfully");

                                progressBar.setVisibility(ProgressBar.GONE);
                            }
                        })
                        .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    // Verify the OTP entered by the user
    private void verifyOTP() {
        String otp = etOTP.getText().toString().trim();

        if (TextUtils.isEmpty(otp)) {
            Toast.makeText(this, "Please enter OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress bar while verifying OTP
        progressBar.setVisibility(ProgressBar.VISIBLE);

        // Create PhoneAuthCredential with the entered OTP
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);

        // Call the method to verify the OTP
        verifyOTPWithCredential(credential);
    }

    // Verify OTP with PhoneAuthCredential
    private void verifyOTPWithCredential(PhoneAuthCredential phoneAuthCredential) {
        mAuth.signInWithCredential(phoneAuthCredential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // OTP verified successfully, proceed to save user data
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserData(user.getUid());
                        }
                    } else {
                        // Handle failure to verify OTP
                        Log.e(TAG, "verifyOTPWithCredential: OTP verification failed");
                        Toast.makeText(RegisterActivity.this, "Invalid OTP", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(ProgressBar.GONE);  // Hide progress bar if OTP fails
                    }
                });
    }

    // Save the user's data to Firebase Realtime Database
    private void saveUserData(String userId) {
        String username = etUsername.getText().toString().trim();
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        String emergencyContact1 = etEmergencyContact1.getText().toString().trim();
        String emergencyContact2 = etEmergencyContact2.getText().toString().trim();

        // Get current timestamp for the user
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String timestamp = sdf.format(new Date());

        // Create a HashMap to store user data
        Map<String, Object> userData = new HashMap<>();
        userData.put("userName", username);
        userData.put("phoneNumber", phoneNumber);
        userData.put("emergencyContact1", emergencyContact1);
        userData.put("emergencyContact2", emergencyContact2);
        userData.put("isSaved", false);  // Set default value
        userData.put("hasConfirmedSafe", false);  // Set default value
        userData.put("location", new HashMap<String, Object>() {{
            put("latitude", 0.0);  // Set default latitude
            put("longitude", 0.0);  // Set default longitude
        }});
        userData.put("timestamp", timestamp);

        // Save user data in Firebase Realtime Database
        DatabaseReference usersRef = mDatabase.getReference("users");
        usersRef.child(userId)  // Create a document for the user with their UID
                .setValue(userData)
                .addOnSuccessListener(aVoid -> {
                    // Data saved successfully, navigate to the next screen
                    Log.d(TAG, "saveUserData: Data saved successfully");
                    Toast.makeText(RegisterActivity.this, "User registered successfully", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(ProgressBar.GONE);  // Hide progress bar

                    // Navigate to EmergencyAlertActivity
                    startActivity(new Intent(RegisterActivity.this, EmergencyAlertActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Handle failure to save data
                    Log.e(TAG, "saveUserData: Error saving user data", e);
                    Toast.makeText(RegisterActivity.this, "Error saving user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(ProgressBar.GONE);  // Hide progress bar
                });
    }
}
