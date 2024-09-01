package com.detrasoft.framework.api.controllers.crud;

import com.detrasoft.framework.api.dto.GenericDTO;
import com.detrasoft.framework.api.dto.converters.GenericEntityDTOConverter;
import com.detrasoft.framework.crud.services.crud.GenericUpdateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.validation.Valid;
import java.util.UUID;

public class GenericUpdateController<DTO extends GenericDTO> {

    protected GenericUpdateService service;

    protected GenericEntityDTOConverter<?, DTO> converter;

    public GenericUpdateController(GenericUpdateService service, GenericEntityDTOConverter<?, DTO> converter) {
        this.service = service;
        this.converter = converter;
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<DTO> update(@PathVariable UUID id, @RequestBody @Valid DTO dto) {
        dto.setId(id);
        var newObj = service.update(id, converter.toEntity(dto));
        return ResponseEntity.ok().body(converter.toDto(newObj));
    }
}
