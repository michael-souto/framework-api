package com.detrasoft.framework.api.controllers.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.hateoas.PagedModel;

import java.io.IOException;

@JsonComponent
public class PagedModelSerializer extends JsonSerializer<PagedModel<?>> {

    @Override
    public void serialize(PagedModel<?> page, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeObjectField("content", page.getContent());
        gen.writeObjectField("links", page.getLinks());

        if (page.getMetadata() != null) {
            gen.writeNumberField("totalElements", page.getMetadata().getTotalElements());
            gen.writeNumberField("totalPages", page.getMetadata().getTotalPages());
            gen.writeNumberField("number", page.getMetadata().getNumber());
            gen.writeNumberField("size", page.getMetadata().getSize());
        }

        gen.writeEndObject();
    }
}
