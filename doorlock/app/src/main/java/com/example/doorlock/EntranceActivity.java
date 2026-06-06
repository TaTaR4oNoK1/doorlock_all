/* Класс входа выполняет несколько основых функций:
   1. Открывает форму восстановления пароля;
   2. Открывает форму создания пользователя;
   3. Проверяет введенные данные для входа используя класс AuthorizationManager
*/
package com.example.doorlock;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper; // Добавлено
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class EntranceActivity extends AppCompatActivity {

    private EditText editTextLogin;
    private EditText editTextPassword;
    private CheckBox checkBoxSaveMe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entrance);

        editTextLogin = findViewById(R.id.editText_Login);
        editTextPassword = findViewById(R.id.editText_Password);
        checkBoxSaveMe = findViewById(R.id.SaveMe);

        View buttonEntrance = findViewById(R.id.button_Entrance);

        TextView textRegistration = findViewById(R.id.text_Registration);
        TextView textForgetPassword = findViewById(R.id.text_ForgetPassword);

        // Пользователь не зарегистрирован
        textRegistration.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegistrationActivity.class);
            startActivity(intent);
        });

        // Пользователь забыл пароль
        textForgetPassword.setOnClickListener(v -> {
            Intent intent = new Intent(this, RemindActivity.class);
            startActivity(intent);
        });

        // Пользователь входит
        buttonEntrance.setOnClickListener(v -> {
            String login = editTextLogin.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            if (login.isEmpty() || password.isEmpty()) {
                blinkRed(editTextLogin, 2000);
                blinkRed(editTextPassword, 2000);
                Toast.makeText(EntranceActivity.this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            // Экземпляр класса AuthorizationManager
            AuthorizationManager authManager = new AuthorizationManager();

            // Вызов метода для передачи логина и пароля на сервер
            authManager.authorize(login, password, new AuthorizationManager.AuthCallback() {
                @Override
                public void onSuccess(String sessionCode, String userId) {
                    SessionManager sessionManager = new SessionManager(EntranceActivity.this);

                    // Сохраняем данные сессии
                    sessionManager.saveSessionId(sessionCode);
                    sessionManager.saveUserId(userId);

                    // Сохраняем флаг saveMe
                    sessionManager.saveCheckMe(checkBoxSaveMe.isChecked());

                    // Переходим в основное меню
                    Intent intent = new Intent(EntranceActivity.this, MainActivity.class);
                    // Очищаем стек, чтобы нельзя было вернуться назад к экрану входа
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onFailure(String errorMessage) {
                    blinkRed(editTextLogin, 3000);
                    blinkRed(editTextPassword, 3000);
                    Toast.makeText(EntranceActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    /**
     * Метод для визуального выделения поля красным цветом при ошибке
     */
    private void blinkRed(EditText editText, long duration) {
        if (editText == null) return;

        int originalColor = editText.getCurrentTextColor();
        editText.setTextColor(0xFFFF0000); // Красный цвет

        // ИСПРАВЛЕНО: Добавлен Looper.getMainLooper() для устранения предупреждения [deprecation]
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            editText.setTextColor(originalColor);
        }, duration);
    }
}