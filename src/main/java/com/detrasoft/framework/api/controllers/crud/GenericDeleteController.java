package com.detrasoft.framework.api.controllers.crud;

import com.detrasoft.framework.api.controllers.jackson.ResponseView;
import com.detrasoft.framework.api.dto.GenericDTO;
import com.detrasoft.framework.core.notification.ResponseNotification;
import com.detrasoft.framework.core.resource.Translator;
import com.detrasoft.framework.crud.services.crud.GenericCRUDService;
import com.detrasoft.framework.enums.CodeMessages;
import com.fasterxml.jackson.annotation.JsonView;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.Instant;
import java.util.UUID;

@SuppressWarnings({"rawtypes", "unchecked"})
public class GenericDeleteController<DTO extends GenericDTO> {

    protected GenericCRUDService service;

    public GenericDeleteController(GenericCRUDService service) {
        this.service = service;
    }

    @JsonView(ResponseView.delete.class)
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<ResponseNotification> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok().body(
                ResponseNotification.builder()
                        .timestamp(Instant.now())
                        .title(Translator.getTranslatedText(CodeMessages.SUCCESS))
                        .detail(Translator.getTranslatedText(CodeMessages.SUCCESS_OPERATION))
                        .messages(service.getMessages())
                        .status(HttpStatus.OK.value())
                        .build());
    }
}