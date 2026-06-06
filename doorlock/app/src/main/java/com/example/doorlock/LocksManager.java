package com.example.doorlock;

import android.os.Handler;
import android.os.Looper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LocksManager {
    private static final String ADDRESS_HELPER_URL = "http://45.11.26.157:6000/adress-help";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    public interface SendAddressCallback {
        void onSuccess();
        void onSuccess(List<AddressItem> addresses); // Изменено на List<AddressItem>
        void onFailure(String errorMessage);
    }

    public void sendNewAddress(String userId, String title, SendAddressCallback callback) {
        SendAddressRequest request = new SendAddressRequest(userId, "add_address", title);
        performSendAddressRequest(request, callback);
    }

    public void sendChangeAddress(String userId, String title, String addressId, SendAddressCallback callback) {
        SendAddressRequest request = new SendAddressRequest(userId, "change_address", title, addressId);
        performSendAddressRequest(request, callback);
    }

    public void sendDeleteAddress(String userId, String addressId, SendAddressCallback callback) {
        SendAddressRequest request = new SendAddressRequest(userId, "delete_address", addressId);
        performSendAddressRequest(request, callback);
    }

    public void sendAllAddress(String userId, SendAddressCallback callback) {
        SendAddressRequest request = new SendAddressRequest(userId, "get_addresses");
        performSendAddressRequest(request, callback);
    }

    private void performSendAddressRequest(SendAddressRequest requestBody, SendAddressCallback callback) {
        executor.execute(() -> {
            try {
                String json = gson.toJson(requestBody);
                RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

                Request request = new Request.Builder()
                        .url(ADDRESS_HELPER_URL)
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        sendFailure(callback, "Ошибка сервера: " + response.code());
                        return;
                    }

                    String responseData = response.body() != null ? response.body().string() : "";

                    mainHandler.post(() -> {
                        if ("get_addresses".equals(requestBody.type)) {
                            callback.onSuccess(parseAddressList(responseData));
                        } else {
                            callback.onSuccess();
                        }
                    });
                }
            } catch (IOException e) {
                sendFailure(callback, "Ошибка сети: " + e.getMessage());
            } catch (Exception e) {
                sendFailure(callback, "Неожиданная ошибка: " + e.getMessage());
            }
        });
    }

    /**
     * Безопасный парсинг JSON-списка объектов AddressItem
     */
    private List<AddressItem> parseAddressList(String data) {
        if (data == null || data.trim().isEmpty() || data.equals("null")) {
            return new ArrayList<>();
        }
        try {
            // Указываем Gson парсить массив объектов вместо массива строк
            Type listType = new TypeToken<List<AddressItem>>(){}.getType();
            List<AddressItem> list = gson.fromJson(data, listType);
            return (list != null) ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void sendFailure(SendAddressCallback callback, String errorMessage) {
        mainHandler.post(() -> callback.onFailure(errorMessage));
    }

    private static class SendAddressRequest {
        String user_id;
        String type;
        String title;
        String address_id;

        SendAddressRequest(String user_id, String type, String title) {
            this.user_id = user_id;
            this.type = type;
            this.title = title;
        }

        SendAddressRequest(String user_id, String type, String title, String address_id) {
            this.user_id = user_id;
            this.type = type;
            this.title = title;
            this.address_id = address_id;
        }

        SendAddressRequest(String user_id, String type) {
            this.user_id = user_id;
            this.type = type;
        }
    }
}