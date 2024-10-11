package com.detrasoft.framework.api.controllers.crud;

import com.detrasoft.framework.api.dto.GenericDTO;
import com.detrasoft.framework.api.dto.converters.GenericEntityDTOConverter;
import com.detrasoft.framework.core.notification.ResponseNotification;
import com.detrasoft.framework.core.resource.Translator;
import com.detrasoft.framework.crud.services.crud.GenericUpdateService;
import com.detrasoft.framework.enums.CodeMessages;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public class GenericUpdateController<DTO extends GenericDTO> {

    protected GenericUpdateService service;

    protected GenericEntityDTOConverter<?, DTO> converter;

    public GenericUpdateController(GenericUpdateService service, GenericEntityDTOConverter<?, DTO> converter) {
        this.service = service;
        this.converter = converter;
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<ResponseNotification> update(@PathVariable UUID id, @RequestBody @Valid DTO dto) {
        dto.setId(id);
        var newObj = service.update(id, converter.toEntity(dto));
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();

        return ResponseEntity.ok().body(
            ResponseNotification.builder()
                .timestamp(Instant.now())
                .title(Translator.getTranslatedText(CodeMessages.SUCCESS))
                .detail(Translator.getTranslatedText(CodeMessages.SUCCESS_OPERATION))
                .messages(service.getMessages())
                .path(uri.toString())
                .status(HttpStatus.CREATED.value())
                .data(converter.toDto(newObj))
            .build()
        );
    }
}
