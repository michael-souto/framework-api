package com.detrasoft.framework.api.controllers.crud;

import com.detrasoft.framework.api.controllers.jackson.ResponseView;
import com.detrasoft.framework.api.dto.GenericDTO;
import com.detrasoft.framework.api.dto.converters.GenericEntityDTOConverter;
import com.detrasoft.framework.core.notification.ResponseNotification;
import com.detrasoft.framework.core.resource.Translator;
import com.detrasoft.framework.crud.services.crud.GenericCRUDService;
import com.detrasoft.framework.enums.CodeMessages;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.lang.reflect.Field;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings({"rawtypes", "unchecked"})
public class GenericUpdateController<DTO extends GenericDTO> {

    protected GenericCRUDService service;
    protected GenericEntityDTOConverter<?, DTO> converter;

    public GenericUpdateController(GenericCRUDService service, GenericEntityDTOConverter<?, DTO> converter) {
        this.service = service;
        this.converter = converter;
    }

    @JsonView(ResponseView.put.class)
    @PutMapping(value = "/{id}")
    public ResponseEntity<ResponseNotification> update(@PathVariable UUID id, @RequestBody @Valid DTO dto) {
        dto.setId(id);
        var objSaved = service.update(id, converter.toEntity(dto));
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();

        return ResponseEntity.ok().body(
                ResponseNotification.builder()
                        .timestamp(Instant.now())
                        .title(Translator.getTranslatedText(CodeMessages.SUCCESS))
                        .detail(Translator.getTranslatedText(CodeMessages.SUCCESS_OPERATION))
                        .messages(service.getMessages())
                        .path(uri.toString())
                        .status(HttpStatus.ACCEPTED.value())
                        .data(converter.toDto(objSaved))
                        .build());
    }

    @JsonView(ResponseView.put.class)
    @PutMapping(value = "/list")
    public ResponseEntity<ResponseNotification> updateList(@RequestBody List<DTO> listDto) {
        var entityList = new ArrayList<>();
        for (DTO dto : listDto) {
            var result = service.update(dto.getId(), converter.toEntity(dto));
            entityList.add(result);
        }
        List<DTO> resultList = entityList.stream().map(x -> converter.toDto(x)).toList();
        return ResponseEntity.ok().body(
                ResponseNotification.builder()
                        .timestamp(Instant.now())
                        .title(Translator.getTranslatedText(CodeMessages.SUCCESS))
                        .detail(Translator.getTranslatedText(CodeMessages.SUCCESS_OPERATION))
                        .messages(service.getMessages())
                        .status(HttpStatus.CREATED.value())
                        .data(resultList)
                        .build());
    }

    @JsonView(ResponseView.patch.class)
    @PatchMapping(value = "/{id}")
    public ResponseEntity<ResponseNotification> patch(@PathVariable UUID id, @RequestBody Map<String, Object> dto,
            HttpServletRequest request) {
        var objSaved = service.findById(id);
        if (objSaved != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());
                objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

                var newObj = objectMapper.convertValue(dto, objSaved.getClass());
                var finalNewDto = newObj;
                dto.forEach((nameProp, valueProp) -> {
                    Field field = ReflectionUtils.findField(objSaved.getClass(), nameProp);
                    Objects.requireNonNull(field).setAccessible(true);
                    Object value = ReflectionUtils.getField(field, finalNewDto);
                    ReflectionUtils.setField(field, objSaved, value);
                });
                newObj = service.update(id, objSaved);
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
                                .build());

            } catch (IllegalArgumentException ex) {
                throw new HttpMessageNotReadableException("Error on parse", new ServletServerHttpRequest(request));
            }
        }
        return ResponseEntity.badRequest().build();
    }

    @JsonView(ResponseView.patch.class)
    @PatchMapping(value = "/list")
    public ResponseEntity<ResponseNotification> patchMultiple(@RequestBody List<Map<String, Object>> dtoList,
            HttpServletRequest request) {
        List<Object> updatedObjects = new ArrayList<>();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            for (Map<String, Object> dto : dtoList) {
                if (!dto.containsKey("id")) {
                    return ResponseEntity.badRequest().body(
                            ResponseNotification.builder()
                                    .timestamp(Instant.now())
                                    .title(Translator.getTranslatedText("NO_ID_IN_REQUEST_BODY"))
                                    .detail("Missing 'id' in request body")
                                    .status(HttpStatus.BAD_REQUEST.value())
                                    .path(request.getRequestURI())
                                    .build());
                }

                UUID id = UUID.fromString(dto.get("id").toString());
                var entity = service.findById(id);

                if (entity != null) {
                    var newObj = objectMapper.convertValue(dto, entity.getClass());

                    dto.forEach((nameProp, valueProp) -> {
                        Field field = ReflectionUtils.findField(entity.getClass(), nameProp);
                        if (field != null) {
                            field.setAccessible(true);
                            Object value = ReflectionUtils.getField(field, newObj);
                            ReflectionUtils.setField(field, entity, value);
                        }
                    });

                    var updatedEntity = service.update(entity);
                    updatedObjects.add(converter.toDto(updatedEntity));
                }
            }

            URI uri = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
            return ResponseEntity.ok().body(
                    ResponseNotification.builder()
                            .timestamp(Instant.now())
                            .title(Translator.getTranslatedText(CodeMessages.SUCCESS))
                            .detail(Translator.getTranslatedText(CodeMessages.SUCCESS_OPERATION))
                            .messages(service.getMessages())
                            .path(uri.toString())
                            .status(HttpStatus.CREATED.value())
                            .data(updatedObjects)
                            .build());

        } catch (IllegalArgumentException ex) {
            throw new HttpMessageNotReadableException("Error on parse", new ServletServerHttpRequest(request));
        }
    }
}