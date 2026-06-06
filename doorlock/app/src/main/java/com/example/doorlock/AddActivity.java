package com.example.doorlock;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class AddActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "extra_mode";
    public static final String EXTRA_USER_ID = "USER_ID";
    public static final int MODE_ADD_LOCK = 1;
    public static final int MODE_ADD_ADDRESS = 2;

    private EditText editTextAdd;
    private String userId;
    private LocksManager locksManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        locksManager = new LocksManager(); // Инициализируем один раз

        userId = getIntent().getStringExtra(EXTRA_USER_ID);
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Ошибка: ID пользователя не найден!", Toast.LENGTH_LONG).show();
            return; // Не пускаем запрос, если он заведомо сломает сервер
        }
        int mode = getIntent().getIntExtra(EXTRA_MODE, MODE_ADD_ADDRESS);

        ImageView buttonBack = findViewById(R.id.Image_button_back);
        buttonBack.setOnClickListener(v -> finish());

        editTextAdd = findViewById(R.id.editText_Add_1);

        if (mode == MODE_ADD_LOCK) {
            setupLockUI();
        } else {
            setupAddressUI();
        }
    }

    private void setupLockUI() {
        editTextAdd.setHint("Введите серийный номер замка");
    }

    private void setupAddressUI() {
        editTextAdd.setHint("Введите название адреса (напр. Дом)");

        findViewById(R.id.button_Add).setOnClickListener(v -> {
            String title = editTextAdd.getText().toString().trim();
            if (validate(title)) {
                sendDataToServer(title);
            }
        });
    }

    private boolean validate(String text) {
        if (text.isEmpty()) {
            Toast.makeText(this, "Поле не может быть пустым", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void sendDataToServer(String title) {
        locksManager.sendNewAddress(userId, title, new LocksManager.SendAddressCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(AddActivity.this, "Успешно добавлено", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onSuccess(List<AddressItem> addresses) {
                //Добавлен, потому что надо:)
                // Серьезно, список здесь нам не нужЁн
            }
            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(AddActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}