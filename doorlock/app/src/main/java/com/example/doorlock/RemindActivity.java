package com.example.doorlock;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Looper;

import androidx.activity.EdgeToEdge;

public class RemindActivity extends AppCompatActivity {

    private EditText editTextRemindLogin;
    private EditText editTextRecoveryCode;
    private EditText editTextRemindPassword1;
    private EditText editTextRemindPassword2;
    private TextView textForTimer;

    private AccountHelper helper;  // Экземпляр помощника
    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_remind);

        // Инициализация полей
        editTextRemindLogin = findViewById(R.id.editText_RemindLogin);
        editTextRecoveryCode = findViewById(R.id.editText_RecoveryCode);
        editTextRemindPassword1 = findViewById(R.id.editText_RemindPassword_1);
        editTextRemindPassword2 = findViewById(R.id.editText_RemindPassword_2);
        textForTimer = findViewById(R.id.textForTimer);
        LinearLayout linearLayout2 = findViewById(R.id.linearLayout2);

        ImageView imageButtonBack = findViewById(R.id.Image_button_back);
        imageButtonBack.setOnClickListener(v -> finish());

        // Инициализация помощника
        helper = new AccountHelper();

        // Обработчик нажатия на linearLayout2 (отправка кода восстановления)
        linearLayout2.setOnClickListener(v -> {
            String email = editTextRemindLogin.getText().toString().trim();
            if (email.isEmpty()) {
                blinkRed(editTextRemindLogin);
                Toast.makeText(this, "Введите email", Toast.LENGTH_SHORT).show();
                return;
            }

            helper.sendRecoveryCode(email, new AccountHelper.SendCodeCallback() {
                @Override
                public void onSuccess() {
                    editTextRecoveryCode.setEnabled(true);
                    editTextRecoveryCode.setClickable(true);
                    editTextRecoveryCode.setFocusable(true);
                    editTextRecoveryCode.setFocusableInTouchMode(true);
                    startTimer();
                    Toast.makeText(RemindActivity.this, "Код отправлен", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(String errorMessage) {
                    blinkRed(editTextRemindLogin);
                    Toast.makeText(RemindActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Обработчик ввода кода восстановления (потеря фокуса)
        editTextRecoveryCode.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String email = editTextRemindLogin.getText().toString().trim();
                String code = editTextRecoveryCode.getText().toString().trim();

                if (code.length() != 6) {
                    blinkRed(editTextRecoveryCode);
                    Toast.makeText(this, "Код должен быть из 6 цифр", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Вызов функции проверки кода восстановления
                helper.verifyRecoveryCode(email, code, new AccountHelper.VerifyCodeCallback() {
                    @Override
                    public void onSuccess() {
                        editTextRemindPassword1.setEnabled(true);
                        editTextRemindPassword1.setClickable(true);
                        editTextRemindPassword1.setFocusable(true);
                        editTextRemindPassword1.setFocusableInTouchMode(true);
                        editTextRemindPassword2.setEnabled(true);
                        editTextRemindPassword2.setClickable(true);
                        editTextRemindPassword2.setFocusable(true);
                        editTextRemindPassword2.setFocusableInTouchMode(true);
                        Toast.makeText(RemindActivity.this, "Код подтверждён", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        blinkRed(editTextRecoveryCode);
                        Toast.makeText(RemindActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // Кнопка сохранения нового пароля
        findViewById(R.id.button_Save).setOnClickListener(v -> {
            String password1 = editTextRemindPassword1.getText().toString().trim();
            String password2 = editTextRemindPassword2.getText().toString().trim();
            String email = editTextRemindLogin.getText().toString().trim();
            // поле пустое
            if (password1.isEmpty() || password2.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }
            //пароли не одинаковы
            if (!password1.equals(password2)) {
                blinkRed(editTextRemindPassword1);
                blinkRed(editTextRemindPassword2);
                Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show();
                return;
            }

            // Реакция от сервера (на продакшене можно убрать)
            helper.changePassword(email, password1, new AccountHelper.ChangePasswordCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(RemindActivity.this, "Пароль изменён!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(RemindActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // Метод для мигания поля красным
    private void blinkRed(EditText editText) {
        int originalColor = editText.getCurrentTextColor();
        editText.setTextColor(0xFFFF0000);  // Красный

        new android.os.Handler(Looper.getMainLooper()).postDelayed(() -> editText.setTextColor(originalColor), 2000);
    }

    // Запуск таймера на 1 минуту
    private void startTimer() {
        textForTimer.setText(getString(R.string.GetCodeTimer));

        timer = new CountDownTimer(60000, 1000) {
            @SuppressLint("SetTextI18n")
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                textForTimer.setText("Осталось: " + seconds + " сек");
            }

            @Override
            public void onFinish() {
                textForTimer.setText(getString(R.string.GetCodeTwice));
                editTextRecoveryCode.setEnabled(false);
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }
    }
}
