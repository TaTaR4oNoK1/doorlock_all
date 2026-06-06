package com.example.doorlock;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import android.widget.EditText;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;
import java.util.Locale;

import android.app.DatePickerDialog;


public class RegistrationActivity extends AppCompatActivity {

    private EditText editText_Name;
    private EditText editText_Surname;
    private EditText editText_BirthDate;
    private RadioButton checkBoxMale;
    private RadioButton checkBoxFemale;
    private EditText editText_PhoneNumber;
    private EditText editText_Email;
    private EditText editText_RecoveryCode;
    private EditText editText_Login;
    private EditText editText_RemindPassword_1;
    private EditText editText_RemindPassword_2;
    private String sex = "male";
    private AccountHelper helper;  // Экземпляр помощника
    private CountDownTimer timer;
    private TextView textForTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration);

        editText_Name = findViewById(R.id.editText_Name);
        editText_Surname = findViewById(R.id.editText_Surname);
        editText_BirthDate = findViewById(R.id.editText_BirthDate);
        editText_PhoneNumber = findViewById(R.id.editText_PhoneNumber);
        checkBoxMale = findViewById(R.id.radioButton_Man);
        checkBoxMale.setChecked(true);
        checkBoxFemale = findViewById(R.id.radioButton_Woman);
        editText_Login = findViewById(R.id.editText_Login);
        editText_Email = findViewById(R.id.editText_Email);
        editText_RecoveryCode = findViewById(R.id.editText_RecoveryCode);
        editText_RemindPassword_1 = findViewById(R.id.editText_RemindPassword_1);
        editText_RemindPassword_2 = findViewById(R.id.editText_RemindPassword_2);
        textForTimer = findViewById(R.id.textForTimer);
        ImageView imageButtonBack = findViewById(R.id.Image_button_back);
        LinearLayout linearLayout2 = findViewById(R.id.linearLayout2);
        imageButtonBack.setOnClickListener(v -> finish());
        // Логика переключения для Male
        checkBoxMale.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                checkBoxFemale.setChecked(false);
                sex = "male";
            } else if (!checkBoxFemale.isChecked()) {
                checkBoxMale.setChecked(true);
            }
        });

        // Логика переключения для Female
        checkBoxFemale.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                checkBoxMale.setChecked(false);
                sex = "female";
            } else if (!checkBoxMale.isChecked()) {
                checkBoxFemale.setChecked(true);
            }
        });
        // Логика выбора даты
        editText_BirthDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    RegistrationActivity.this,
                    (view, selectedYear, selectedMonth, dayOfMonth) -> {
                        // Форматируем дату (месяцы в Android начинаются с 0, поэтому +1)
                        String date = String.format(Locale.US, "%d-%02d-%02d", selectedYear, selectedMonth + 1, dayOfMonth);
                        editText_BirthDate.setText(date);
                        editText_BirthDate.setError(null); // Убираем ошибку, если она была
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });

        // Инициализация помощника
        helper = new AccountHelper();

        // Обработчик нажатия на linearLayout2 (отправка кода восстановления)
        linearLayout2.setOnClickListener(v -> {
            String name = editText_Name.getText().toString().trim();
            String surname = editText_Surname.getText().toString().trim();
            String birth_date = editText_BirthDate.getText().toString().trim();
            String login = editText_Login.getText().toString().trim();
            String email = editText_Email.getText().toString().trim();
            String phone = editText_PhoneNumber.getText().toString().trim();


            //Вызов обраблотчика для проверки полей
            if (!validateFields(name, surname, email, birth_date, login, phone)) {
                return;
            }
            //Вызов помощника для отправки кода
            helper.sendRegistrationCode(email,name, surname, birth_date, login, sex, phone, new AccountHelper.SendCodeCallback() {
                @Override
                public void onSuccess() {
                    editText_RecoveryCode.setEnabled(true);
                    editText_RecoveryCode.setClickable(true);
                    editText_RecoveryCode.setFocusable(true);
                    editText_RecoveryCode.setFocusableInTouchMode(true);
                    startTimer();
                    Toast.makeText(RegistrationActivity.this, "Код отправлен", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(String errorMessage) {
                    blinkRed(editText_Email);
                    Toast.makeText(RegistrationActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        });
        // Обработчик ввода кода восстановления (потеря фокуса)
        editText_RecoveryCode.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String email = editText_Email.getText().toString().trim();
                String code = editText_RecoveryCode.getText().toString().trim();
                if (code.length() != 6) {
                    blinkRed(editText_RecoveryCode); // Исправлено имя переменной
                    Toast.makeText(this, "Код должен быть из 6 цифр", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Вызов помощника для проверки кода
                helper.verifyRegistrationCode(email, code, new AccountHelper.VerifyCodeCallback() {
                    @Override
                    public void onSuccess() {
                        editText_RemindPassword_1.setEnabled(true);
                        editText_RemindPassword_1.setClickable(true);
                        editText_RemindPassword_1.setFocusable(true);
                        editText_RemindPassword_1.setFocusableInTouchMode(true);

                        editText_RemindPassword_2.setEnabled(true);
                        editText_RemindPassword_2.setClickable(true);
                        editText_RemindPassword_2.setFocusable(true);
                        editText_RemindPassword_2.setFocusableInTouchMode(true);

                        Toast.makeText(RegistrationActivity.this, "Код подтверждён", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        blinkRed(editText_RecoveryCode);
                        Toast.makeText(RegistrationActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        findViewById(R.id.button_Save).setOnClickListener(v -> {
            String password1 = editText_RemindPassword_1.getText().toString().trim();
            String password2 = editText_RemindPassword_2.getText().toString().trim();
            String email = editText_Email.getText().toString().trim();
            // поле пустое
            if (password1.isEmpty() || password2.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }
            //пароли не одинаковы
            if (!password1.equals(password2)) {
                blinkRed(editText_RemindPassword_1);
                blinkRed(editText_RemindPassword_2);
                Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show();
                return;
            }

            // Реакция от сервера (на продакшене можно убрать)
            helper.changePassword(email, password1, new AccountHelper.ChangePasswordCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(RegistrationActivity.this, "Пароль изменён!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(RegistrationActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        });

    }
    private boolean validateFields(String name, String surname, String email, String birthDate, String login, String phone) {
        if (name.isEmpty()) return notifyError(editText_Name, "Введите имя");
        if (surname.isEmpty()) return notifyError(editText_Surname, "Введите фамилию");
        if (email.isEmpty()) return notifyError(editText_Email, "Введите Email");
        if (birthDate.isEmpty()) return notifyError(editText_BirthDate, "Укажите дату рождения");
        // Проверка формата email
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return notifyError(editText_Email, "Некорректный Email");
        }

        if (login.isEmpty()) return notifyError(editText_Login, "Введите логин");
        if (phone.isEmpty()) return notifyError(editText_PhoneNumber, "Введите телефон");

        return true;
    }

    private boolean notifyError(EditText field, String message) {
        blinkRed(field);
        field.setError(message); // Добавляет стандартную иконку ошибки
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        field.requestFocus();
        return false;
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
                editText_RecoveryCode.setEnabled(false);
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
