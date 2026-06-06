package com.example.doorlock;

import android.os.Handler;
import android.os.Looper;  // Импорт Looper

import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.OkHttpClient;

public class AccountHelper {

    private static final String SEND_CODE_URL = "http://45.11.26.157:6000/send-code";
    private static final String VERIFY_CODE_URL = "http://45.11.26.157:6000/check-code";
    private static final String CHANGE_PASSWORD_URL = "http://45.11.26.157:6000/change-password";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    // Интерфейсы callback
    public interface SendCodeCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public interface VerifyCodeCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public interface ChangePasswordCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    //Вызов кода восстановления
    public void sendRecoveryCode(String email, SendCodeCallback callback) {
        SendCodeRequest request = new SendCodeRequest(email, "recovery");
        performSendCodeRequest(request, callback);
    }

    // Вызов кода регистрации
    public void sendRegistrationCode(String email, String name, String surname, String birth_date, String login, String sex, String phone, SendCodeCallback callback) {

        SendCodeRequest request = new SendCodeRequest(
                email,
                "registration",
                name,
                surname,
                birth_date,
                login,
                sex,
                phone
        );
        performSendCodeRequest(request, callback);
    }
    private void performSendCodeRequest(SendCodeRequest requestBody, SendCodeCallback callback) {
        executor.execute(() -> {
            String json = gson.toJson(requestBody);

            RequestBody body = RequestBody.create(
                    json,
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(SEND_CODE_URL) // Используем одну константу URL
                    .post(body)
                    .build();

            try {
                Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    runOnUiThread(() -> callback.onFailure("Ошибка сервера: " + response.code()));
                    return;
                }

                runOnUiThread(callback::onSuccess);
            } catch (IOException e) {
                runOnUiThread(() -> callback.onFailure("Ошибка сети: " + e.getMessage()));
            } catch (Exception e) {
                runOnUiThread(() -> callback.onFailure("Неожиданная ошибка: " + e.getMessage()));
            }
        });
    }
    /*
     Функция проверки кода восстановления .
     */
    // Функция проверки кода восстановления
    public void verifyRecoveryCode(String email, String code, VerifyCodeCallback callback) {
        VerifyCodeRequest request = new VerifyCodeRequest(email, code, "recovery");
        performVerifyCodeRequest(request, callback);
    }

    // Функция проверки кода регистрации
    public void verifyRegistrationCode(String email, String code, VerifyCodeCallback callback) {
        VerifyCodeRequest request = new VerifyCodeRequest(email, code, "registration");
        performVerifyCodeRequest(request, callback);
    }

    // Общий метод для выполнения запроса проверки кода
    private void performVerifyCodeRequest(VerifyCodeRequest requestBody, VerifyCodeCallback callback) {
        executor.execute(() -> {
            String json = gson.toJson(requestBody);

            RequestBody body = RequestBody.create(
                    json,
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(VERIFY_CODE_URL)
                    .post(body)
                    .build();

            try {
                Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    runOnUiThread(() -> callback.onFailure("Ошибка сервера: " + response.code()));
                    return;
                }

                runOnUiThread(callback::onSuccess);
            } catch (IOException e) {
                runOnUiThread(() -> callback.onFailure("Ошибка сети: " + e.getMessage()));
            } catch (Exception e) {
                runOnUiThread(() -> callback.onFailure("Неожиданная ошибка: " + e.getMessage()));
            }
        });
    }

    /**
     * Меняет пароль пользователя.
     */
    public void changePassword(String email, String newPassword, ChangePasswordCallback callback) {
        executor.execute(() -> {
            ChangePasswordRequest requestBody = new ChangePasswordRequest(email, newPassword);
            String json = gson.toJson(requestBody);

            RequestBody body = RequestBody.create(
                    json,
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(CHANGE_PASSWORD_URL)
                    .post(body)
                    .build();

            try {
                Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    runOnUiThread(() -> callback.onFailure(
                            "Ошибка сервера: " + response.code()
                    ));
                    return;
                }

                runOnUiThread(callback::onSuccess);
            } catch (IOException e) {
                runOnUiThread(() -> callback.onFailure("Ошибка сети: " + e.getMessage()));
            } catch (Exception e) {
                runOnUiThread(() -> callback.onFailure("Неожиданная ошибка: " + e.getMessage()));
            }
        });
    }

    // Вспомогательные классы для JSON
    private static class SendCodeRequest {
        String email;
        String type;
        String name;
        String surname;
        String birth_date;
        String login;
        String sex;
        String phone;

        SendCodeRequest(String email, String type) {
            this.email = email;
            this.type = type;
        }

        SendCodeRequest(String email, String type, String name, String surname, String birth_date, String login, String sex, String phone) {
            this.email = email;
            this.type = type;
            this.name = name;
            this.surname = surname;
            this.birth_date = birth_date;
            this.login = login;
            this.sex = sex;
            this.phone = phone;
        }
    }

    private static class VerifyCodeRequest {
        String email;
        String code;
        String type_code; // Убедитесь, что сервер ждет именно это имя поля

        VerifyCodeRequest(String email, String code, String type_code) {
            this.email = email;
            this.code = code;
            this.type_code = type_code;
        }
    }

    private static class ChangePasswordRequest {
        String email;
        String password;
        ChangePasswordRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    // Выполнение кода в UI-потоке
    private void runOnUiThread(Runnable runnable) {
        mainHandler.post(runnable);
    }
}
