package com.app.common.util;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

public final class Md5Util {

    public static String md5(String str) {
        if (str == null) {
            return null;
        }
        return DigestUtils.md5DigestAsHex(str.getBytes(StandardCharsets.UTF_8));
    }

    public static String encryptPassword(String plain) {
        return plain != null ? md5("Meta" + plain + "Bird") : "";
    }
}
