package com.detrasoft.framework.api.controllers.jackson;

import org.springframework.boot.jackson.JacksonComponent;
import org.springframework.data.domain.Page;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * Serializa qualquer {@link Page} (PageImpl, SliceImpl paginado etc.) num JSON
 * estável e enxuto:
 * {@code content, totalElements, totalPages, number, size, numberOfElements, first, last, empty}.
 * <p>
 * Precisa ser um {@link ValueSerializer} do Jackson 3: o Spring Boot 4 só
 * registra via
 * {@link JacksonComponent} serializers de {@code tools.jackson}; um
 * {@code JsonSerializer} do
 * Jackson 2 é ignorado em silêncio e o Page cai na serialização por reflexão do
 * bean, que
 * expõe {@code pageable}/{@code sort}, dispara o WARN do Spring Data e quebra
 * no native image
 * (MissingReflectionRegistrationError em
 * {@code AbstractPageRequest.getOffset()}).
 * <p>
 * Só chama os getters da interface (sem reflexão) e escreve o {@code content}
 * pelo próprio
 * {@link SerializationContext}, preservando o {@code @JsonView} ativo do
 * endpoint.
 */
@JacksonComponent
public class PageJsonSerializer extends ValueSerializer<Page<?>> {

    @Override
    public void serialize(Page<?> page, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        gen.writeStartObject(page);

        gen.writeName("content");
        ctxt.writeValue(gen, page.getContent());

        gen.writeNumberProperty("totalElements", page.getTotalElements());
        gen.writeNumberProperty("totalPages", page.getTotalPages());
        gen.writeNumberProperty("number", page.getNumber());
        gen.writeNumberProperty("size", page.getSize());
        gen.writeNumberProperty("numberOfElements", page.getNumberOfElements());
        gen.writeBooleanProperty("first", page.isFirst());
        gen.writeBooleanProperty("last", page.isLast());
        gen.writeBooleanProperty("empty", page.isEmpty());

        gen.writeEndObject();
    }

    @Override
    public Class<?> handledType() {
        return Page.class;
    }
}
