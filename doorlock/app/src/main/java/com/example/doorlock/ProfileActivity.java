package com.example.doorlock; // Убедитесь, что пакет указан верно

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity; // Важный импорт

// Класс должен обязательно расширять AppCompatActivity
public class ProfileActivity extends AppCompatActivity {
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        sessionManager = new SessionManager(this);
        ImageView buttonBack = findViewById(R.id.Image_button_back);
        buttonBack.setOnClickListener(v -> finish());
        Button buttonExit = findViewById(R.id.button_Exit);
        buttonExit.setOnClickListener(v -> {
            sessionManager.clearAll();
            finish();
        });
    }
}