/* Класс для Проверяет валидность сессии (не истек ли срок). При успехе возвращает id пользователя;
*/
package com.example.doorlock;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.*;

public class SessionValidator {

    private static final String VALIDATE_URL = "http://45.11.26.157:6000/validate";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    public interface ValidationCallback {
        void onSuccess(String userId);
        void onFailure(String errorMessage);
    }

    /**
     * Проверяет валидность сессии через серверный API.
     * @param sessionId код сессии
     * @param callback callback для обработки результата
     */
    public void validateSession(String sessionId, ValidationCallback callback) {
        executor.execute(() -> {
            // Формируем тело запроса (только session_code)
            ValidateRequest requestBody = new ValidateRequest(sessionId);
            String json = gson.toJson(requestBody);

            RequestBody body = RequestBody.create(
                    json,
                    MediaType.get("application/json")
            );

            Request request = new Request.Builder()
                    .url(VALIDATE_URL)
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

                String responseBody = response.body().string();
                ValidateResponse validateResponse = gson.fromJson(responseBody, ValidateResponse.class);

                if (validateResponse.user_id != null && !validateResponse.user_id.isEmpty()) {
                    runOnUiThread(() -> callback.onSuccess(validateResponse.user_id));
                } else {
                    runOnUiThread(() -> callback.onFailure("Сессия не найдена или истекла"));
                }
            } catch (IOException e) {
                runOnUiThread(() -> callback.onFailure("Ошибка сети: " + e.getMessage()));
            } catch (Exception e) {
                runOnUiThread(() -> callback.onFailure("Неожиданная ошибка: " + e.getMessage()));
            }
        });
    }

    // Вспомогательные классы для JSON
    private static class ValidateRequest {
        String session_code;

        ValidateRequest(String session_code) {
            this.session_code = session_code;
        }
    }

    private static class ValidateResponse {
        String user_id;
    }

    // Выполняет код в UI-потоке
    private void runOnUiThread(Runnable runnable) {
        mainHandler.post(runnable);
    }
}
