package com.detrasoft.framework.api.controllers.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.data.domain.Page;

import java.io.IOException;

@JsonComponent
public class PageJsonSerializer extends JsonSerializer<Page<?>> {

    @Override
    public void serialize(Page page, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();

        gen.writeObjectField("content", page.getContent());
        gen.writeNumberField("totalElements", page.getTotalElements());
        gen.writeNumberField("totalPages", page.getTotalPages());
        gen.writeNumberField("number", page.getNumber());
        gen.writeNumberField("size", page.getSize());
        gen.writeNumberField("numberOfElements", page.getNumberOfElements());
        gen.writeBooleanField("first", page.isFirst());
        gen.writeBooleanField("last", page.isLast());
        gen.writeBooleanField("empty", page.isEmpty());

        gen.writeEndObject();
    }
}
