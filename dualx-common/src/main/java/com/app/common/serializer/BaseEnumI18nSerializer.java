package com.app.common.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.app.common.enums.BaseEnum;
import com.app.common.util.MessageUtil;

import java.io.IOException;

public class BaseEnumI18nSerializer extends JsonSerializer<BaseEnum> {

    @Override
    public void serialize(BaseEnum baseEnum, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (baseEnum == null) {
            jsonGenerator.writeNull();
            return;
        }
        String i18nMsg = MessageUtil.get(baseEnum.i18nKey());
        jsonGenerator.writeString(i18nMsg);
    }
}
