package com.detrasoft.framework.api.controllers;

import com.detrasoft.framework.api.controllers.jackson.ResponseView;
import com.detrasoft.framework.api.dto.GenericDTO;
import com.detrasoft.framework.api.dto.converters.GenericEntityDTOConverter;
import com.detrasoft.framework.core.notification.ResponseNotification;
import com.detrasoft.framework.core.resource.Translator;
import com.detrasoft.framework.crud.entities.GenericEntity;
import com.detrasoft.framework.crud.services.crud.GenericCRUDService;
import com.detrasoft.framework.enums.CodeMessages;
import com.fasterxml.jackson.annotation.JsonView;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class GenericDetailController<DTO extends GenericDTO> {

    protected GenericCRUDService service;
    protected GenericEntityDTOConverter<?, DTO> converter;

    protected abstract void setIdSubDetailInDTO(UUID idDetail, UUID idSubDetail, DTO dto);

    public GenericDetailController(GenericCRUDService<?> service, GenericEntityDTOConverter<?, DTO> converter) {
        this.service = service;
        this.converter = converter;
    }

    @JsonView(ResponseView.post.class)
    @PostMapping
    public ResponseEntity<ResponseNotification> insert(@PathVariable(value = "idDetail") UUID idDetail,
            @RequestBody @Valid DTO dto) {
        setIdSubDetailInDTO(idDetail, null, dto);
        var newObj = service.insert(converter.toEntity(dto));
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{idSubDetail}").buildAndExpand(dto.getId())
                .toUri();
        return ResponseEntity.created(uri).body(
                ResponseNotification.builder()
                        .timestamp(Instant.now())
                        .title(Translator.getTranslatedText(CodeMessages.SUCCESS))
                        .detail(Translator.getTranslatedText(CodeMessages.SUCCESS_OPERATION))
                        .messages(service.getMessages())
                        .path(uri.toString())
                        .status(HttpStatus.CREATED.value())
                        .data(converter.toDto(newObj))
                        .build());
    }

    @JsonView(ResponseView.post.class)
    @PostMapping(value = "/list")
    public ResponseEntity<ResponseNotification> insertList(@RequestBody List<DTO> listDto) {
        var entityList = new ArrayList<>();
        for (DTO dto : listDto) {
            var result = service.insert(converter.toEntity(dto));
            entityList.add(result);
        }
        List<DTO> resultList = entityList.stream().map(x -> converter.toDto(x)).toList();
        return ResponseEntity.created(null).body(
                ResponseNotification.builder()
                        .timestamp(Instant.now())
                        .title(Translator.getTranslatedText(CodeMessages.SUCCESS))
                        .detail(Translator.getTranslatedText(CodeMessages.SUCCESS_OPERATION))
                        .messages(service.getMessages())
                        .status(HttpStatus.CREATED.value())
                        .data(resultList)
                        .build());
    }

    @PutMapping(value = "/{idSubDetail}")
    public ResponseEntity<ResponseNotification> update(@PathVariable(value = "idDetail") UUID idDetail,
            @PathVariable(value = "idSubDetail") UUID idSubDetail,
            @RequestBody @Valid DTO dto) {
        setIdSubDetailInDTO(idDetail, idSubDetail, dto);
        var objSaved = service.update(idSubDetail, converter.toEntity(dto));
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{idSubDetail}").buildAndExpand(dto.getId())
                .toUri();
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

    @JsonView(ResponseView.findById.class)
    @GetMapping(value = "/{idSubDetail}")
    public ResponseEntity<DTO> findById(@PathVariable UUID idSubDetail) {
        var obj = findByIdSubDetail(idSubDetail);
        return ResponseEntity.ok(converter.toDto(obj));
    }

    protected GenericEntity findByIdSubDetail(UUID idSubDetail) {
        return service.findById(idSubDetail);
    }

    @JsonView(ResponseView.findAll.class)
    @GetMapping
    public ResponseEntity<Page<DTO>> findAll(@PathVariable(value = "idDetail") UUID idDetail, Pageable pageable) {
        Page<?> list = findAllByIdDetail(idDetail, pageable);
        Page<DTO> resultList = new PageImpl<DTO>(
                list.getContent().stream()
                        .map(obj -> converter.toDto(obj)).toList(),
                pageable, list.getTotalElements());
        return ResponseEntity.ok().body(resultList);
    }

    protected Page<? extends GenericEntity> findAllByIdDetail(UUID idDetail, Pageable pageable) {
        return service.findAllPaged(pageable);
    }

    @JsonView(ResponseView.delete.class)
    @DeleteMapping(value = "/{idSubDetail}")
    public ResponseEntity<ResponseNotification> deleteById(@PathVariable UUID idSubDetail) {
        service.delete(idSubDetail);
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
