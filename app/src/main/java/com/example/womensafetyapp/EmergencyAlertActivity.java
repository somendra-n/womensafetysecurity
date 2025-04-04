package com.example.womensafetyapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class EmergencyAlertActivity extends AppCompatActivity {

    private static final float SHAKE_THRESHOLD = 12.0f;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final int SMS_PERMISSION_REQUEST_CODE = 102;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float lastX, lastY, lastZ;
    private long lastShakeTime;
    private int shakeCount = 0;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean isSafeMode = false;
    private DatabaseReference mDatabase;
    private TextView tvShakeStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_alert);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize the sensor manager and accelerometer
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Initialize the FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize the TextView for shake status
        tvShakeStatus = findViewById(R.id.tvShakeStatus);

        setupShakeDetection();

        // Increment the user count when the activity is created
        incrementUserCount();
    }

    // Setup shake detection
    private void setupShakeDetection() {
        SensorEventListener sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];

                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastShakeTime > 500) {
                        long diffTime = currentTime - lastShakeTime;
                        lastShakeTime = currentTime;

                        float speed = Math.abs(x + y + z - lastX - lastY - lastZ) / diffTime * 10000;

                        if (speed > SHAKE_THRESHOLD) {
                            shakeCount++;

                            // If there have been 3 shakes, trigger the emergency alert
                            if (shakeCount == 3 && !isSafeMode) {
                                sendEmergencyAlert();
                                shakeCount = 0; // Reset shake count after sending alert
                            } else if (shakeCount < 3) {
                                tvShakeStatus.setText("Shake the phone " + (3 - shakeCount) + " more times to send the alert.");
                                tvShakeStatus.setVisibility(View.VISIBLE); // Make it visible
                            }
                        }

                        lastX = x;
                        lastY = y;
                        lastZ = z;
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // Not used
            }
        };

        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    // Send emergency alert
    private void sendEmergencyAlert() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_REQUEST_CODE);
            return;
        }

        // Get the user's location
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                String message = "Emergency! I am in danger. Location: " +
                        "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                sendSmsToEmergencyContacts(message); // Send SMS after getting location

                // Update the user's latitude and longitude in Firebase
                updateUserLocation(location.getLatitude(), location.getLongitude());

                updateUserStatusToSaved();
                isSafeMode = true;

                // Update the UI to reflect that the user is in Emergency Mode
                tvShakeStatus.setText("You're in Emergency Mode!");
                tvShakeStatus.setVisibility(View.VISIBLE);
            }
        });
    }

    // Send SMS to emergency contacts (2 contacts)
    private void sendSmsToEmergencyContacts(String message) {
        getEmergencyContact((contact1, contact2) -> {
            if (contact1 != null && !contact1.isEmpty()) {
                sendSms(contact1, message); // Send SMS to the first contact
            } else {
                Log.e("EmergencyAlert", "Emergency contact 1 is missing or invalid.");
            }
            if (contact2 != null && !contact2.isEmpty()) {
                sendSms(contact2, message); // Send SMS to the second contact
            } else {
                Log.e("EmergencyAlert", "Emergency contact 2 is missing or invalid.");
            }
        });
    }

    // Method to send SMS to a given contact using SmsManager
    private void sendSms(String contact, String message) {
        SmsManager smsManager = SmsManager.getDefault();
        try {
            Log.d("EmergencyAlert", "Sending SMS to: " + contact + " with message: " + message);
            smsManager.sendTextMessage(contact, null, message, null, null);
            Toast.makeText(EmergencyAlertActivity.this, "Emergency message sent to: " + contact, Toast.LENGTH_SHORT).show();

            // After sending the emergency message, navigate to SafeModeActivity
            Intent intent = new Intent(EmergencyAlertActivity.this, SafeModeActivity.class);
            startActivity(intent);
            finish();  // Close the current activity (EmergencyAlertActivity)

        } catch (Exception e) {
            Toast.makeText(EmergencyAlertActivity.this, "Failed to send emergency message to: " + contact, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    // Get emergency contacts from Firebase asynchronously
    private void getEmergencyContact(final EmergencyContactCallback callback) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Fetch user's emergency contacts from Firebase
        mDatabase.child("users").child(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot dataSnapshot = task.getResult();
                String emergencyContact1 = dataSnapshot.child("emergencyContact1").getValue(String.class);
                String emergencyContact2 = dataSnapshot.child("emergencyContact2").getValue(String.class);

                if (emergencyContact1 != null && emergencyContact2 != null) {
                    callback.onContactFetched(emergencyContact1, emergencyContact2);
                } else {
                    Toast.makeText(EmergencyAlertActivity.this, "No emergency contacts found!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(EmergencyAlertActivity.this, "Failed to retrieve emergency contacts", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Callback interface to handle fetched contacts
    private interface EmergencyContactCallback {
        void onContactFetched(String contact1, String contact2);
    }

    // Update user's location (latitude, longitude) to Firebase
    private void updateUserLocation(double latitude, double longitude) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Update the user's latitude and longitude in the Firebase database
        mDatabase.child("users").child(userId).child("latitude").setValue(latitude);
        mDatabase.child("users").child(userId).child("longitude").setValue(longitude)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(EmergencyAlertActivity.this, "Location updated to Firebase", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(EmergencyAlertActivity.this, "Failed to update location", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Increment the user count in Firebase
    private void incrementUserCount() {
        mDatabase.child("totalUsers").runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Long currentCount = mutableData.getValue(Long.class);
                if (currentCount == null) {
                    currentCount = 0L;
                }
                mutableData.setValue(currentCount + 1);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean committed, DataSnapshot dataSnapshot) {
                // Optional: Handle completion if needed
            }
        });
    }

    // Update user status to 'saved'
    private void updateUserStatusToSaved() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Update the user's 'isSaved' status to true
        mDatabase.child("users").child(userId).child("isSaved").setValue(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Successfully updated the user status to "saved"
                        Toast.makeText(EmergencyAlertActivity.this, "You are saved now!", Toast.LENGTH_SHORT).show();
                        incrementSavedMembersCount();
                    } else {
                        // Error occurred while updating the status
                        Toast.makeText(EmergencyAlertActivity.this, "Failed to update user status", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Increment the savedMembersCount after marking the user as saved
    private void incrementSavedMembersCount() {
        mDatabase.child("savedMembersCount").runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Long currentCount = mutableData.getValue(Long.class);
                if (currentCount == null) {
                    currentCount = 0L;
                }
                mutableData.setValue(currentCount + 1);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean committed, DataSnapshot dataSnapshot) {
                // Optional: Handle completion if needed
            }
        });
    }

    // Handle permission results
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendEmergencyAlert();
            } else {
                Toast.makeText(this, "Location permission is required.", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendEmergencyAlert();
            } else {
                Toast.makeText(this, "SMS permission is required.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
