package com.example.womensafetyapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class HomeScreenActivity extends AppCompatActivity {

    private Button btnLogin, btnRegister, btnHowToUse, btnLawsActsEmergency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);  // The XML layout

        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnHowToUse = findViewById(R.id.btnHowToUse);



        // Login Button Click
        btnLogin.setOnClickListener(v -> startActivity(new Intent(HomeScreenActivity.this, LoginActivity.class)));

        // Register Button Click
        btnRegister.setOnClickListener(v -> startActivity(new Intent(HomeScreenActivity.this, RegisterActivity.class)));

        // How to Use This App Button Click
        btnHowToUse.setOnClickListener(v -> {
            new AlertDialog.Builder(HomeScreenActivity.this)
                    .setTitle("How to Use This App")
                    .setMessage("1. Register with your phone number.\n\n" +
                            "2. Add your emergency contacts.\n\n" +
                            "3. Shake your phone to send an emergency alert with live location.\n\n" +
                            "4. After shaking you go to safemode where you should have to enter your emergency numbers to confirm that you are safe!\n\n"+
                            "5. Feel safe with app features")
                    .setPositiveButton("OK", null)
                    .show();
        });


    }
}
