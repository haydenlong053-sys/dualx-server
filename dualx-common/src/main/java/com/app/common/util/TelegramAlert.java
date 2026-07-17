package com.app.common.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Telegram 告警发送工具类。
 */
public final class TelegramAlert {

    private static final String BOT_TOKEN_ENV = "TELEGRAM_BOT_TOKEN";
    private static final String CHAT_ID_ENV = "TELEGRAM_CHAT_ID";

    private static final String TELEGRAM_API_URL_FORMAT =
            "https://api.telegram.org/bot%s/sendMessage";

    private static final String WARNING_ICON = "⚠️";
    private static final String ERROR_ICON = "🔴";
    private static final String SUCCESS_ICON = "✅";
    private static final String NOTICE_ICON = "📢";
    private static final String TIME_ICON = "⏰";

    private static final String TITLE_FORMAT = "%s 【%s】%n%n%s";
    private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private static final int HTTP_OK = 200;
    private static final int CONNECT_TIMEOUT = 10_000;
    private static final int READ_TIMEOUT = 10_000;

    private TelegramAlert() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static boolean send(String message, String botToken, String chatId) {
        HttpURLConnection connection = null;

        try {
            String telegramApiUrl = String.format(TELEGRAM_API_URL_FORMAT, botToken);
            String finalMessage = appendTime(message);

            String encodedMessage = URLEncoder.encode(finalMessage, StandardCharsets.UTF_8.name());
            String urlString = telegramApiUrl + "?chat_id=" + chatId + "&text=" + encodedMessage;

            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);

            int responseCode = connection.getResponseCode();
            String responseBody = readResponse(connection, responseCode);

            System.out.println("Telegram response code: " + responseCode);
            System.out.println("Telegram response body: " + responseBody);

            return responseCode == HTTP_OK;
        } catch (Exception e) {
            System.err.println("Failed to send Telegram message: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static boolean sendAlert(String title, String content, String botToken, String chatId) {
        return send(formatMessage(WARNING_ICON, title, content), botToken, chatId);
    }

    public static boolean sendError(String errorMessage, String botToken, String chatId) {
        return send(formatMessage(ERROR_ICON, "错误通知", errorMessage), botToken, chatId);
    }

    public static boolean sendSuccess(String successMessage,String botToken, String chatId) {
        return send(formatMessage(SUCCESS_ICON, "成功通知", successMessage), botToken, chatId);
    }

    public static boolean sendNotice(String message, String botToken, String chatId) {
        return send(NOTICE_ICON + " " + message, botToken, chatId);
    }

    private static String formatMessage(String icon, String title, String content) {
        return String.format(TITLE_FORMAT, icon, title, content);
    }

    private static String appendTime(String message) {
        String timestamp = new SimpleDateFormat(TIME_FORMAT).format(new Date());
        return message + "\n\n" + TIME_ICON + " 时间：" + timestamp;
    }

    private static String readResponse(HttpURLConnection connection, int responseCode) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        responseCode == HTTP_OK ? connection.getInputStream() : connection.getErrorStream(),
                        StandardCharsets.UTF_8
                )
        )) {
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            return response.toString();
        }
    }

    private static String getRequiredEnv(String name) {
        String value = System.getenv(name);

        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }

        return value.trim();
    }
}