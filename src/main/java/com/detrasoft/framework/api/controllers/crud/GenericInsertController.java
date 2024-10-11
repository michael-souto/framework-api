package com.detrasoft.framework.api.controllers.crud;

import com.detrasoft.framework.api.dto.GenericDTO;
import com.detrasoft.framework.api.dto.converters.GenericEntityDTOConverter;
import com.detrasoft.framework.core.notification.ResponseNotification;
import com.detrasoft.framework.core.resource.Translator;
import com.detrasoft.framework.crud.services.crud.GenericInsertService;
import com.detrasoft.framework.enums.CodeMessages;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;

public abstract class GenericInsertController<DTO extends GenericDTO> {

    protected GenericInsertService service;

    protected GenericEntityDTOConverter<?, DTO> converter;

    public GenericInsertController(GenericInsertService service, GenericEntityDTOConverter<?, DTO> converter) {
        this.service = service;
        this.converter = converter;
    }

    @PostMapping
    public ResponseEntity<ResponseNotification> insert(@RequestBody @Valid DTO dto, HttpServletRequest request) {
        var newObj = service.insert(converter.toEntity(dto));
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(newObj.getId()).toUri();
        
        return ResponseEntity.created(uri).body(
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
