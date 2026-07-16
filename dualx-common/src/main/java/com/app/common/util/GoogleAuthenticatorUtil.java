package com.app.common.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;


@Service
public class GoogleAuthenticatorUtil {
    private final com.warrenstrange.googleauth.GoogleAuthenticator googleAuthenticator = new com.warrenstrange.googleauth.GoogleAuthenticator();
 
    public String generateSecretKey() {
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        return key.getKey();
    }

    /**
     * 生成 Google Authenticator 可扫描的二维码（Base64 图片）
     */
    public String generateQRCodeWithoutAccount(String secretKey) {
        try {
            // 生成简化的OTP URI（不包含accountName）
            String otpAuthUri = String.format("otpauth://totp/%s?secret=%s&issuer=%s",
                URLEncoder.encode("RWA", String.valueOf(StandardCharsets.UTF_8)),
                secretKey,
                URLEncoder.encode("RWA", String.valueOf(StandardCharsets.UTF_8)));

            // 生成QR码
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(otpAuthUri, BarcodeFormat.QR_CODE, 200, 200);

            // 转换为Base64图片
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            return "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());

        } catch (Exception e) {
            throw new RuntimeException("生成二维码失败", e);
        }
    }
 
    public boolean verifyCode(String secretKey, int verificationCode) {
        return googleAuthenticator.authorize(secretKey, verificationCode);
    }

}