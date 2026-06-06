package com.example.doorlock;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private SessionValidator sessionValidator;
    private boolean isFinishingFlag = false;
    private android.view.View noInternetLayout;
    private SwipeRefreshLayout swipeRefreshLayout;
    private AddressAdapter adapter;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Инициализация UI
        noInternetLayout = findViewById(R.id.noInternetLayout);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        FrameLayout addButton = findViewById(R.id.frameLayout2);
        FrameLayout button_Profile = findViewById(R.id.button_Profile);
        recyclerView = findViewById(R.id.addressesRecyclerView);

        sessionManager = new SessionManager(this);
        sessionValidator = new SessionValidator();

        // Настройка SwipeRefresh
        swipeRefreshLayout.setOnRefreshListener(this::checkSessionAndNavigate);

        String userId = sessionManager.getUserId();

        // Кнопка добавления
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddActivity.class);
            intent.putExtra(AddActivity.EXTRA_MODE, AddActivity.MODE_ADD_ADDRESS);
            intent.putExtra(AddActivity.EXTRA_USER_ID, userId);
            startActivity(intent);
        });

        // Кнопка профиля
        button_Profile.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        });

        // Обработка системных отступов
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Настройка RecyclerView
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new AddressAdapter(new ArrayList<>(), item -> {
            Intent intent = new Intent(MainActivity.this, MainLockActivity.class);
            intent.putExtra("address_id", item.getId());
            intent.putExtra("address_title", item.getTitle());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        checkSessionAndNavigate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkSessionAndNavigate();
    }

    private void checkSessionAndNavigate() {
        if (isFinishing() || isDestroyed()) return;

        if (!isNetworkAvailable()) {
            swipeRefreshLayout.setRefreshing(false);
            noInternetLayout.setVisibility(android.view.View.VISIBLE);
            Toast.makeText(this, "Нет интернета", Toast.LENGTH_SHORT).show();
            return;
        } else {
            noInternetLayout.setVisibility(android.view.View.GONE);
        }

        if (!sessionManager.hasSession()) {
            startEntranceActivity();
            return;
        }

        String sessionId = sessionManager.getSessionId();
        sessionValidator.validateSession(sessionId, new SessionValidator.ValidationCallback() {
            @Override
            public void onSuccess(String userId) {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        noInternetLayout.setVisibility(android.view.View.GONE);
                        loadAddresses();
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        sessionManager.clearSession();
                        startEntranceActivity();
                    }
                });
            }
        });
    }

    private void loadAddresses() {
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        LocksManager locksManager = new LocksManager();
        locksManager.sendAllAddress(userId, new LocksManager.SendAddressCallback() {
            @Override
            public void onSuccess(List<AddressItem> addresses) {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Log.d("DEBUG_LOCKS", "Получено адресов: " + addresses.size());
                        adapter.updateData(addresses);
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onSuccess() {}

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(MainActivity.this, "Ошибка: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    // ИСПРАВЛЕННЫЙ МЕТОД ПРОВЕРКИ ИНТЕРНЕТА
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork == null) return false;

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
        return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        );
    }

    private void startEntranceActivity() {
        if (isFinishingFlag) return;
        isFinishingFlag = true;
        Intent intent = new Intent(this, EntranceActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isFinishingFlag = true;
    }
}