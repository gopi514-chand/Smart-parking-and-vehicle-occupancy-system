package com.pavani.smart_parking;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Button btnSignUp = findViewById(R.id.btn_sign_up);
        Button btnSignIn = findViewById(R.id.btn_sign_in);

        btnSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(SplashActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        btnSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        // Check if user is signed in (non-null) and update UI accordingly.
        new Handler().postDelayed(() -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }, 1000); // 1 second
    }
}
