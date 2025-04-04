package com.example.womensafetyapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {

    private EditText etPhoneNumber, etOtp;
    private Button btnSendOtp, btnVerifyOtp;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String verificationId;  // For storing the verification ID after OTP is sent.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth and Database
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize UI components
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etOtp = findViewById(R.id.etOtp);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);

        // Set up the button to send OTP
        btnSendOtp.setOnClickListener(v -> sendOtp());

        // Set up the button to verify OTP
        btnVerifyOtp.setOnClickListener(v -> verifyOtp());
    }

    // Method to send OTP to the phone number
    private void sendOtp() {
        String phoneNumber = etPhoneNumber.getText().toString().trim();

        // Validate the phone number (Indian number with country code +91)
        if (TextUtils.isEmpty(phoneNumber) || phoneNumber.length() != 10) {
            Toast.makeText(LoginActivity.this, "Enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add the Indian country code (+91) to the phone number
        String phoneNumberWithCountryCode = "+91" + phoneNumber;

        // Check if the phone number exists in Firebase Realtime Database
        checkPhoneNumberRegistered(phoneNumberWithCountryCode);
    }

    // Method to check if the phone number is registered in the database
    private void checkPhoneNumberRegistered(String phoneNumber) {
        mDatabase.child("users").orderByChild("phoneNumber").equalTo(phoneNumber)
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().exists()) {
                            // If the phone number is registered, proceed to send OTP
                            sendOtpToPhone(phoneNumber);
                        } else {
                            // If the phone number is not registered
                            Toast.makeText(LoginActivity.this, "This phone number is not registered.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Handle any errors during the query
                        Toast.makeText(LoginActivity.this, "Error checking phone number registration: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Method to send OTP using Firebase Phone Authentication
    private void sendOtpToPhone(String phoneNumber) {
        // Send OTP using Firebase Phone Authentication
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,  // Phone number with country code (e.g., +91XXXXXXXXXX)
                60,           // Timeout duration in seconds
                TimeUnit.SECONDS,
                this,         // Context (Activity)
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                        // This callback will be triggered automatically if OTP is auto-detected
                        signInWithPhoneAuthCredential(phoneAuthCredential);
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        // If OTP sending fails, show an error message
                        Toast.makeText(LoginActivity.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        // Store the verificationId, which will be used for OTP verification
                        LoginActivity.this.verificationId = verificationId;

                        // Inform the user to enter the OTP
                        Toast.makeText(LoginActivity.this, "OTP sent! Enter the OTP below.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Method to verify OTP entered by the user
    private void verifyOtp() {
        String otp = etOtp.getText().toString().trim();

        if (TextUtils.isEmpty(otp)) {
            Toast.makeText(LoginActivity.this, "Enter the OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a PhoneAuthCredential using the verificationId and OTP entered
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);

        // Sign in the user with the phone number
        signInWithPhoneAuthCredential(credential);
    }

    // Method to sign in with the PhoneAuthCredential
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // If login is successful, navigate to EmergencyAlertActivity
                        Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, EmergencyAlertActivity.class);
                        startActivity(intent);
                        finish(); // Finish the LoginActivity to prevent back navigation
                    } else {
                        // If login fails, show an error message
                        Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
