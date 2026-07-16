package com.app.common.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class CoinNumSerializer extends JsonSerializer<BigDecimal> {

    private static final BigDecimal MILLION = new BigDecimal("1000000");
    private static final BigDecimal THOUSAND = new BigDecimal("1000");
    private static final BigDecimal SCALE6 = new BigDecimal("0.000001");
    private static final BigDecimal SCALE3 = new BigDecimal("0.001");

    @Override
    public void serialize(BigDecimal value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (value == null) {
            jsonGenerator.writeNumber("0");
            return;
        }
        BigDecimal abs = value.abs();
        if (abs.compareTo(MILLION) >= 0) {
            jsonGenerator.writeString(value.setScale(0, RoundingMode.DOWN).toPlainString());
        } else if (abs.compareTo(THOUSAND) >= 0) {
            jsonGenerator.writeString(value.setScale(2, RoundingMode.DOWN).toPlainString());
        } else if (abs.compareTo(SCALE6) < 0) {
            jsonGenerator.writeString(value.setScale(8, RoundingMode.DOWN).stripTrailingZeros().toPlainString());
        } else if (abs.compareTo(SCALE3) < 0) {
            jsonGenerator.writeString(value.setScale(6, RoundingMode.DOWN).stripTrailingZeros().toPlainString());
        } else {
            jsonGenerator.writeString(value.setScale(4, RoundingMode.DOWN).stripTrailingZeros().toPlainString());
        }
    }
}
