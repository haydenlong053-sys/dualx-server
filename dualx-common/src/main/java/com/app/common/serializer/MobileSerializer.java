package com.app.common.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class MobileSerializer extends JsonSerializer<String> {
    @Override
    public void serialize(String value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (value == null) {
            jsonGenerator.writeNumber("000****0000");
            return;
        }
        if (value.length() < 11) {
            jsonGenerator.writeString(value);
            return;
        }
        //(\\d{3})\\d{4}(\\w{4})
        jsonGenerator.writeString(value.replaceAll("(\\w{3})\\w{4}(\\w{4})", "$1****$2"));
    }
}
