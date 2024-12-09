package com.example.wmpprojectfireaicamera;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AlertActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert);
        TextView alertMessage = findViewById(R.id.alertMessage);
        alertMessage.setText("Fire detected! Please take action immediately.");
    }
}
