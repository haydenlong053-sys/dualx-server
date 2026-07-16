package com.app.common.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class BigCnySerializer extends JsonSerializer<BigDecimal> {

    private static final BigDecimal WAN = new BigDecimal("10000");
    private static final BigDecimal MILLION = new BigDecimal("1000000");

    @Override
    public void serialize(BigDecimal bigDecimal, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (bigDecimal == null) {
            jsonGenerator.writeString("0");
            return;
        }
        if (bigDecimal.compareTo(MILLION) >= 0) {
            jsonGenerator.writeString(bigDecimal.divide(WAN, 0, RoundingMode.DOWN).stripTrailingZeros().toPlainString() + "万");
            return;
        }
        if(bigDecimal.compareTo(WAN) >= 0){
            jsonGenerator.writeString(bigDecimal.setScale(0, RoundingMode.DOWN).stripTrailingZeros().toPlainString());
            return;
        }
        jsonGenerator.writeString(bigDecimal.setScale(2, RoundingMode.DOWN).stripTrailingZeros().toPlainString());
    }
}
