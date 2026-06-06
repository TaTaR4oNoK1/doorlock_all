// Класс получает на вход логин и пароль. Сам извлекает ip устройства и передает все на сервер. Если такой пользователь существует, то получает обратно метку успеха. Возвращает обратно либо код сессии, либо причину ошибки

package com.example.doorlock;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.*;

public class AuthorizationManager {
    private static final String BASE_URL = "http://45.11.26.157:6000/auth";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    public interface AuthCallback {
        void onSuccess(String sessionCode, String userId);
        void onFailure(String errorMessage);
    }

    /**
     * Отправляет запрос на авторизацию.
     * @param login логин пользователя
     * @param password пароль пользователя
     * @param callback callback для обработки результата
     */
    public void authorize(String login, String password, AuthCallback callback) {
        executor.execute(() -> {
            // Получаем текущий IP-адрес устройства
            String ipAddress = getDeviceIpAddress();
            if (ipAddress == null) {
                ipAddress = "000.000.000.000:0000";  // значение по умолчанию
            }

            // Формируем JSON-тело запроса из логина, пароля, ip
            AuthRequest requestBody = new AuthRequest(login, password, ipAddress);
            String json = gson.toJson(requestBody);

            RequestBody body = RequestBody.create(
                    json, MediaType.get("application/json")
            );

            Request request = new Request.Builder()
                    .url(BASE_URL)
                    .post(body)
                    .build();

            try {
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> callback.onFailure("Ошибка сервера: " + response.code()));
                    return;
                }

                String responseBody = response.body().string();
                AuthResponse authResponse = gson.fromJson(responseBody, AuthResponse.class);

                if (authResponse.session_code != null && !authResponse.session_code.isEmpty()) {
                    runOnUiThread(() -> callback.onSuccess(authResponse.session_code, authResponse.user_id));
                } else {
                    runOnUiThread(() -> callback.onFailure("Неверный логин или пароль"));
                }
            } catch (IOException e) {
                runOnUiThread(() -> callback.onFailure("Ошибка сети: " + e.getMessage()));
                
            }
        });
    }

    /**
     * Получает текущий IP-адрес устройства (IPv4).
     * @return IP-адрес в формате строки или null, если не найден
     */
    private String getDeviceIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    // Фильтруем IPv4 и исключаем loopback-интерфейсы (127.0.0.1)
                    if (!address.isLoopbackAddress() && address.getHostAddress().indexOf(':') == -1) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) { }
        return null;
    }

    // Вспомогательные классы для JSON
    private static class AuthRequest {
        String login;
        String password;
        String ip_address;

        AuthRequest(String login, String password, String ip_address) {
            this.login = login;
            this.password = password;
            this.ip_address = ip_address;
        }
    }

    private static class AuthResponse {
        String session_code;
        String user_id;
    }

    private void runOnUiThread(Runnable runnable) {
        mainHandler.post(runnable);
    }
}
