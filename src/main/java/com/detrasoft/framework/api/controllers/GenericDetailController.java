package com.detrasoft.framework.api.controllers;

import com.detrasoft.framework.api.dto.GenericDTO;
import com.detrasoft.framework.api.dto.converters.GenericEntityDTOConverter;
import com.detrasoft.framework.crud.entities.GenericEntity;
import com.detrasoft.framework.crud.services.crud.GenericCRUDService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;

public abstract class GenericDetailController<DTO extends GenericDTO> {
    
    protected GenericCRUDService service;
    protected GenericEntityDTOConverter<?, DTO> converter;

    protected abstract void setIdSubDetailInDTO(UUID idDetail, UUID idSubDetail, DTO dto);
    public GenericDetailController(GenericCRUDService<?> service, GenericEntityDTOConverter<?, DTO> converter) {
        this.service = service;
        this.converter = converter;
    }

    @PostMapping
    public ResponseEntity<DTO> insert(@PathVariable(value = "idDetail") UUID idDetail,
                                      @RequestBody @Valid DTO dto) {
        setIdSubDetailInDTO(idDetail, null, dto);
        var obj = service.insert(converter.toEntity(dto));
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{idSubDetail}").buildAndExpand(dto.getId())
                .toUri();
        return ResponseEntity.created(uri).body(converter.toDto(obj));
    }

    @PutMapping(value = "/{idSubDetail}")
    public ResponseEntity<DTO> update(@PathVariable(value = "idDetail") UUID idDetail,
                                      @PathVariable(value = "idSubDetail") UUID idSubDetail,
                                                 @RequestBody @Valid DTO dto) {
        setIdSubDetailInDTO(idDetail, idSubDetail, dto);
        var obj = service.update(idSubDetail, converter.toEntity(dto));
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{idSubDetail}").buildAndExpand(dto.getId())
                .toUri();
        return ResponseEntity.created(uri).body(converter.toDto(obj));
    }

    @GetMapping(value = "/{idSubDetail}")
    public ResponseEntity<DTO> findById(@PathVariable UUID idSubDetail) {
        var obj = findByIdSubDetail(idSubDetail);
        return ResponseEntity.ok(converter.toDto(obj));
    }

    protected GenericEntity findByIdSubDetail(UUID idSubDetail) {
        return service.findById(idSubDetail);
    }

    @GetMapping
    public ResponseEntity<Page<DTO>> findAll(@PathVariable(value = "idDetail") UUID idDetail, Pageable pageable) {
        Page<?> list = findAllByIdDetail(idDetail, pageable);
        Page<DTO> resultList = new PageImpl<DTO>(
                list.getContent().stream()
                        .map(obj -> converter.toDto(obj)).toList(), pageable, list.getTotalElements());
        return ResponseEntity.ok().body(resultList);
    }

    protected Page<? extends GenericEntity> findAllByIdDetail(UUID idDetail, Pageable pageable) {
        return  service.findAllPaged(pageable);
    }

    @DeleteMapping(value = "/{idSubDetail}")
    public ResponseEntity<Void> deleteById(@PathVariable UUID idSubDetail) {
        service.delete(idSubDetail);
        return ResponseEntity.noContent().build();
    }
}
