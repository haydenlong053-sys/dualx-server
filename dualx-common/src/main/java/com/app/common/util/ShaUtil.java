package com.app.common.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ShaUtil {

    public static BigInteger sha256ToInt(String text) {
        String res = sha256(text);
        return res == null ? BigInteger.ZERO : new BigInteger(res, 16);
    }

    public static String sha256(String text) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(text.getBytes(StandardCharsets.UTF_8));
            return byte2hex(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256计算失败", e);
        }
        return null;
    }

    public static byte[] hmacSha256(String text, String key){
        try {
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            return mac.doFinal(text.getBytes());
        } catch (NoSuchAlgorithmException e) {
            log.error("HMAC-SHA256算法不可用", e);
        } catch (InvalidKeyException e) {
            log.error("HMAC-SHA256密钥无效", e);
        }
        return null;
    }

    public static String hmacSha256Hex(String text, String key) {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            return byte2hex(mac.doFinal(text.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            log.error("HMAC-SHA256算法不可用", e);
        } catch (InvalidKeyException e) {
            log.error("HMAC-SHA256密钥无效", e);
        }
        return null;
    }

    private static String byte2hex(byte[] b) {
        StringBuilder hs = new StringBuilder();
        String stmp;
        for (int n = 0; b != null && n < b.length; n++) {
            stmp = Integer.toHexString(b[n] & 0XFF);
            if (stmp.length() == 1)
                hs.append('0');
            hs.append(stmp);
        }
        return hs.toString().toLowerCase();
    }
}
