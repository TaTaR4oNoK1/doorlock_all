package com.example.doorlock;

import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainLockActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_lock);

        sessionManager = new SessionManager(this);

        // Инициализация детектора жестов
        setupSwipeDetector();

        int addressId = getIntent().getIntExtra("address_id", -1);
        String addressTitle = getIntent().getStringExtra("address_title");

        TextView title = findViewById(R.id.MyLock);
        if (title != null) {
            title.setText(addressTitle);
        }

        // Кнопка назад
        ImageView buttonBack = findViewById(R.id.Image_button_back);
        if (buttonBack != null) {
            buttonBack.setOnClickListener(v -> {
                Log.d("DEBUG_LOCKS", "Кнопка НАЗАД нажата!");
                finish(); // Просто закрываем без кастомной анимации
            });
        }
    }

    private void setupSwipeDetector() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 150;
            private static final int SWIPE_VELOCITY_THRESHOLD = 150;

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;

                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                // Только горизонтальные жесты
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    // СТРОГО ВПРАВО
                    if (diffX > SWIPE_THRESHOLD && velocityX > SWIPE_VELOCITY_THRESHOLD) {
                        Log.d("DEBUG_LOCKS", "Свайп ВПРАВО обнаружен");
                        finish(); // Просто закрываем
                        return true;
                    }

                    // Поглощаем свайп влево, чтобы ничего не происходило
                    if (diffX < -SWIPE_THRESHOLD) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (gestureDetector != null) {
            // Если жест распознан (вправо или влево), прерываем дальнейшую передачу события
            if (gestureDetector.onTouchEvent(ev)) {
                return true;
            }
        }
        return super.dispatchTouchEvent(ev);
    }
}