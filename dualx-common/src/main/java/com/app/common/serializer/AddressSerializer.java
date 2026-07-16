package com.app.common.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class AddressSerializer extends JsonSerializer<String> {
    @Override
    public void serialize(String value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (value == null) {
            jsonGenerator.writeNumber("0x00****0000");
            return;
        }
        if (value.length() < 20) {
            jsonGenerator.writeString(value);
            return;
        }
        //(\\d{4})\\d{10}(\\w{4})
        jsonGenerator.writeString(value.replaceAll("(\\w{4})\\w{34}(\\w{4})", "$1****$2"));
    }
}
