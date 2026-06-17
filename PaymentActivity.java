package com.pavani.smart_parking;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PaymentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        EditText etCardNumber = findViewById(R.id.et_card_number);
        EditText etExpiryDate = findViewById(R.id.et_expiry_date);
        EditText etCvv = findViewById(R.id.et_cvv);
        Button btnPayNow = findViewById(R.id.btn_pay_now);

        btnPayNow.setOnClickListener(v -> {
            String cardNumber = etCardNumber.getText().toString();
            String expiryDate = etExpiryDate.getText().toString();
            String cvv = etCvv.getText().toString();

            if (cardNumber.isEmpty() || expiryDate.isEmpty() || cvv.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            } else {
                // In a real app, you would integrate a payment gateway here.
                // For this simulation, we'll just show a success message.
                Toast.makeText(this, "Payment Successful!", Toast.LENGTH_SHORT).show();
                finish(); // Close the payment activity
            }
        });
    }
}
