package com.detrasoft.framework.api.controllers;

import com.detrasoft.framework.api.controllers.jackson.ResponseView;
import com.detrasoft.framework.api.dto.GenericDTO;
import com.detrasoft.framework.api.dto.converters.GenericEntityDTOConverter;
import com.detrasoft.framework.crud.entities.GenericEntity;
import com.detrasoft.framework.crud.services.crud.GenericCRUDService;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public abstract class GenericCRUDController<DTO extends GenericDTO> {

    protected GenericCRUDService service;

    protected GenericEntityDTOConverter<?, DTO> converter;

    public GenericCRUDController(GenericCRUDService service, GenericEntityDTOConverter<?, DTO> converter) {
        this.service = service;
        this.converter = converter;
    }

    @JsonView(ResponseView.findAll.class)
    @GetMapping
    public ResponseEntity<Page<DTO>> findAll(Pageable pageable) {
        Page<?> list = getAllPaged(pageable);
        Page<DTO> resultList = new PageImpl<DTO>(
                list.getContent().stream()
                        .map(obj -> converter.toDto(obj)).toList(), pageable, list.getTotalElements());
        return ResponseEntity.ok().body(resultList);
    }

    protected Page<? extends GenericEntity> getAllPaged(Pageable pageable) {
        return service.findAllPaged(pageable);
    }


    @JsonView(ResponseView.findById.class)
    @GetMapping(value = "/{id}")
    public ResponseEntity<DTO> findById(@PathVariable String id) {
        GenericEntity dto = getOne(id);
        return ResponseEntity.ok().body(converter.toDto(dto));
    }

    protected GenericEntity getOne(String id) {
        return service.findById(UUID.fromString(id));
    }


    @JsonView(ResponseView.post.class)
    @PostMapping
    public ResponseEntity<DTO> insert(@RequestBody @Valid DTO dto) {
        var newObj = service.insert(converter.toEntity(dto));
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(newObj.getId()).toUri();
        return ResponseEntity.created(uri).body(converter.toDto(newObj));
    }

    @JsonView(ResponseView.put.class)
    @PutMapping(value = "/{id}")
    public ResponseEntity<DTO> update(@PathVariable UUID id, @RequestBody @Valid DTO dto) {
        dto.setId(id);
        var newObj = service.update(id, converter.toEntity(dto));
        return ResponseEntity.ok().body(converter.toDto(newObj));
    }

    @JsonView(ResponseView.patch.class)
    @PatchMapping(value = "/{id}")
    public ResponseEntity<DTO> patch(@PathVariable UUID id, @RequestBody Map<String, Object> dto, HttpServletRequest request) {
        var objSaved = service.findById(id);
        if (objSaved != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                var newObj = objectMapper.convertValue(dto, objSaved.getClass());
                var finalNewDto = newObj;
                dto.forEach((nameProp, valueProp) -> {
                    Field field = ReflectionUtils.findField(objSaved.getClass(), nameProp);
                    Objects.requireNonNull(field).setAccessible(true);
                    Object value = ReflectionUtils.getField(field, finalNewDto);
                    ReflectionUtils.setField(field, objSaved, value);
                });
                newObj = service.update(id, objSaved);
                return ResponseEntity.ok().body(converter.toDto(finalNewDto));
            } catch (IllegalArgumentException ex) {
                throw new HttpMessageNotReadableException("Error on parse", new ServletServerHttpRequest(request));
            }
        }
        return ResponseEntity.badRequest().build();
    }

    @JsonView(ResponseView.delete.class)
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
