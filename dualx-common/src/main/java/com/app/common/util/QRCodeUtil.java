package com.app.common.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Hashtable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QRCodeUtil {

    /**
     * 生成二维码并转换为 Base64 字符串
     *
     * @param content 要生成二维码的内容
     * @param width   二维码宽度
     * @param height  二维码高度
     * @return Base64 编码的二维码字符串
     */
    public static String generateQRCodeBase64(String content, int width, int height) {
        try {
            // 设置二维码参数
            Hashtable<EncodeHintType, Object> hints = new Hashtable<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8"); // 指定字符编码
            hints.put(EncodeHintType.MARGIN, 1); // 二维码白边大小

            // 生成二维码
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

            // 创建 BufferedImage
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    image.setRGB(x, y, bitMatrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
                }
            }

            // 转换为 Base64 字符串
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);

        } catch (WriterException | IOException e) {
            log.error("生成二维码失败", e);
        }
        return null;
    }
}
