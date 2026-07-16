package com.app.common.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 百分数序列化，乘以100 后追加百分号
 */
public class PercentageSerializer extends JsonSerializer<BigDecimal> {
    @Override
    public void serialize(BigDecimal bigDecimal, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (bigDecimal == null) {
            jsonGenerator.writeNumber("0.00 %");
            return;
        }
        jsonGenerator.writeString(
            bigDecimal.multiply(BigDecimal.TEN.pow(2))
                .setScale(2, RoundingMode.DOWN).toPlainString() + " %"
        );
    }
}
