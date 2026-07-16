package com.app.common.util;


import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 图片校验工具类
 */
public class ImgUtil {

    public static boolean isImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        String contentType = file.getContentType();
        /*return contentType != null && contentType.startsWith("image/");*/
        return contentType != null && (contentType.startsWith("image/") || contentType.startsWith("application/octet-stream"));
    }

    public static boolean isImageByExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        // 定义允许的图片格式
        List<String> imageExtensions = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp");
        // 获取文件名并提取后缀
        String fileName = file.getOriginalFilename();
        if (fileName != null) {
            String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
            return imageExtensions.contains(fileExtension);
        }

        return false;
    }

    public static boolean isRealImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            return image != null;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean isValidImage(MultipartFile file) {
        return isImageFile(file) && isImageByExtension(file) && isRealImage(file);
    }
}
