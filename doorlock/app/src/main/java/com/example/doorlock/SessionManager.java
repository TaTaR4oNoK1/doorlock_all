/*
    Класс работы с сессией на устройстве:
    1. Сохраняет в памяти устройства id сессии;
    2. Извлекает из памяти устройства id сессии;
    3. Проверяет в памяти устройства id сессии;
    4. Удаляет из памяти устройства id сессии;
    5. Сохраняет в памяти устройства флаг сохранения авторизации;
    6. Извлекает из памяти устройства флаг сохранения авторизации;
    7. Удаляет из памяти устройства флаг сохранения авторизации;
    8. Удаляет из памяти устройства id сессии и флаг сохранения авторизации;
 */
package com.example.doorlock;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "AppPrefs";
    private static final String KEY_SESSION_ID = "SESSION_ID";

    private static final String KEY_USER_ID = "USER_ID";
    private static final String KEY_CHECK_ME = "CHECK_ME"; // флаг: 1 или 0

    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    /**
     * Сохранить ID сессии
     * @param sessionId ID сессии (строка)
     */
    public void saveSessionId(String sessionId) {
        editor.putString(KEY_SESSION_ID, sessionId);
        editor.apply(); // асинхронное сохранение
    }

    /**
     * Сохранить ID пользователя
     * @param userId ID пользователя (строка)
     */

    public void saveUserId(String userId) {
        editor.putString(KEY_USER_ID, userId);
        editor.apply(); // асинхронное сохранение
    }

    /**
     * Получить ID сессии
     * @return ID сессии или null, если её нет
     */
    public String getSessionId() {
        return sharedPreferences.getString(KEY_SESSION_ID, null);
    }

    /**
     * Получить ID пользователя
     * @return ID пользователя или null, если её нет
     */
    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID, null);
    }

    /**
     * Проверить, есть ли сохранённая сессия
     * @return true, если сессия есть
     */
    public boolean hasSession() {
        return sharedPreferences.contains(KEY_SESSION_ID);
    }

    /**
     * Удалить ID сессии (например, при выходе из аккаунта)
     */
    public void clearSession() {
        editor.remove(KEY_SESSION_ID);
        editor.apply();
    }

    /**
     * Сохранить флаг "Запомнить меня" (1 = да, 0 = нет)
     * @param isChecked true — запомнить, false — не запоминать
     */
    public void saveCheckMe(boolean isChecked) {
        editor.putInt(KEY_CHECK_ME, isChecked ? 1 : 0);
        editor.apply();
    }

    /**
     * Получить значение флага "Запомнить меня"
     * @return 1, если нужно запомнить; 0 — если не нужно
     */
    public int getCheckMe() {
        return sharedPreferences.getInt(KEY_CHECK_ME, 0); // по умолчанию 0
    }

    /**
     * Очистить флаг "Запомнить меня"
     */
    public void clearCheckMe() {
        editor.remove(KEY_CHECK_ME);
        editor.apply();
    }

    /**
     * Полностью очистить все сохранённые данные (сессия + флаг)
     */
    public void clearAll() {
        editor.remove(KEY_SESSION_ID);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_CHECK_ME);
        editor.apply();
    }
}
