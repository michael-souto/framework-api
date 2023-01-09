package com.detrasoft.framework.api.controllers.crud;

import com.detrasoft.framework.api.controllers.jackson.ResponseView;
import com.detrasoft.framework.api.dto.GenericDTO;
import com.detrasoft.framework.api.dto.converters.GenericEntityDTOConverter;
import com.detrasoft.framework.crud.entities.GenericEntity;
import com.detrasoft.framework.crud.services.crud.GenericCRUDService;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

public class GenericGetController<DTO extends GenericDTO> {

    protected GenericCRUDService<?> service;
    protected GenericEntityDTOConverter<?, DTO> converter;

    public GenericGetController(GenericCRUDService<?> service, GenericEntityDTOConverter<?, DTO> converter) {
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

    protected Page<?> getAllPaged(Pageable pageable) {
        return service.findAllPaged(pageable);
    }

    @JsonView(ResponseView.findById.class)
    @GetMapping(value = "/{id}")
    public ResponseEntity<DTO> findById(@PathVariable String id) {
        GenericEntity dto = getOne(id);
        return ResponseEntity.ok().body(converter.toDto(dto));
    }

    protected GenericEntity getOne(String id) {
        return service.findById(Long.parseLong(id));
    }

}
