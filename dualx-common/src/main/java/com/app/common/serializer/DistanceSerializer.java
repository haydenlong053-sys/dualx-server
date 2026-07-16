package com.app.common.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 经纬度数字序列化，保留6位小数
 */
public class DistanceSerializer extends JsonSerializer<Double> {

    @Override
    public void serialize(Double value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (value == null) {
            jsonGenerator.writeString("0");
            return;
        }
        jsonGenerator.writeString(BigDecimal.valueOf(value).setScale(2, RoundingMode.DOWN).stripTrailingZeros().toPlainString());
    }
}
