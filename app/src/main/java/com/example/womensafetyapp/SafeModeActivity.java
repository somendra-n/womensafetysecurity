package com.example.womensafetyapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SafeModeActivity extends AppCompatActivity {

    private EditText etEmergencyContact1, etEmergencyContact2;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safe_mode);

        // Initialize Firebase and views
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize the EditText fields
        etEmergencyContact1 = findViewById(R.id.etEmergencyContact1);
        etEmergencyContact2 = findViewById(R.id.etEmergencyContact2);
    }

    public void confirmSafety(View view) {
        // Get the emergency contact numbers from the EditText fields
        String emergencyContact1 = etEmergencyContact1.getText().toString();
        String emergencyContact2 = etEmergencyContact2.getText().toString();

        // Validate that both emergency contacts are provided
        if (TextUtils.isEmpty(emergencyContact1) || TextUtils.isEmpty(emergencyContact2)) {
            Toast.makeText(this, "Please enter both emergency contact numbers.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the current user's ID
        String userId = mAuth.getCurrentUser().getUid();

        // Update user's safety status in Firebase
        mDatabase.child("users").child(userId).child("hasConfirmedSafe").setValue(true);

        // Update the saved member count in Firebase
        incrementSavedMembersCount();

        // Update the user's emergency contact numbers in Firebase (if it's the first time)
        mDatabase.child("users").child(userId).child("emergencyContact1").setValue(emergencyContact1);
        mDatabase.child("users").child(userId).child("emergencyContact2").setValue(emergencyContact2);

        // Optionally, update the "isSaved" status if required
        mDatabase.child("users").child(userId).child("isSaved").setValue(true);

        // Toast to show that the safety confirmation is successful
        Toast.makeText(this, "You're now confirmed safe!", Toast.LENGTH_SHORT).show();

        // Navigate back to EmergencyAlertActivity after confirming safety
        Intent intent = new Intent(SafeModeActivity.this, EmergencyAlertActivity.class);
        // To ensure SafeModeActivity is removed from the activity stack, we can use the following:
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        // Close the SafeModeActivity after navigating back to EmergencyAlertActivity
        finish();
    }

    // Increment the saved members count
    private void incrementSavedMembersCount() {
        mDatabase.child("savedMembersCount").runTransaction(new com.google.firebase.database.Transaction.Handler() {
            @Override
            public com.google.firebase.database.Transaction.Result doTransaction(com.google.firebase.database.MutableData mutableData) {
                Long currentCount = mutableData.getValue(Long.class);
                if (currentCount == null) {
                    currentCount = 0L;
                }
                mutableData.setValue(currentCount + 1);
                return com.google.firebase.database.Transaction.success(mutableData);
            }

            @Override
            public void onComplete(com.google.firebase.database.DatabaseError databaseError, boolean committed, com.google.firebase.database.DataSnapshot dataSnapshot) {
                // Optional: Handle completion if needed
            }
        });

        // Increment the total app users count (this will track how many users are using the app)
        mDatabase.child("appUsersCount").runTransaction(new com.google.firebase.database.Transaction.Handler() {
            @Override
            public com.google.firebase.database.Transaction.Result doTransaction(com.google.firebase.database.MutableData mutableData) {
                Long currentCount = mutableData.getValue(Long.class);
                if (currentCount == null) {
                    currentCount = 0L;
                }
                mutableData.setValue(currentCount + 1);
                return com.google.firebase.database.Transaction.success(mutableData);
            }

            @Override
            public void onComplete(com.google.firebase.database.DatabaseError databaseError, boolean committed, com.google.firebase.database.DataSnapshot dataSnapshot) {
                // Optional: Handle completion if needed
            }
        });
    }
}
