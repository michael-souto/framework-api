package com.detrasoft.framework.api.controllers;

import com.detrasoft.framework.api.dto.GenericDTO;
import com.detrasoft.framework.api.dto.converters.GenericEntityDTOConverter;
import com.detrasoft.framework.crud.entities.GenericEntity;
import com.detrasoft.framework.crud.services.crud.GenericCRUDService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;

public abstract class GenericDetailController<DTO extends GenericDTO> {
    
    protected GenericCRUDService service;
    protected GenericEntityDTOConverter<?, DTO> converter;

    protected abstract void setIdSubDetailInDTO(Long idDetail, Long idSubDetail, DTO dto);
    public GenericDetailController(GenericCRUDService<?> service, GenericEntityDTOConverter<?, DTO> converter) {
        this.service = service;
        this.converter = converter;
    }

    @PostMapping
    public ResponseEntity<DTO> insert(@PathVariable(value = "idDetail") Long idDetail,
                                      @RequestBody @Valid DTO dto) {
        setIdSubDetailInDTO(idDetail, null, dto);
        var obj = service.insert(converter.toEntity(dto));
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{idSubDetail}").buildAndExpand(dto.getId())
                .toUri();
        return ResponseEntity.created(uri).body(converter.toDto(obj));
    }

    @PutMapping(value = "/{idSubDetail}")
    public ResponseEntity<DTO> update(@PathVariable(value = "idDetail") Long idDetail,
                                      @PathVariable(value = "idSubDetail") Long idSubDetail,
                                                 @RequestBody @Valid DTO dto) {
        setIdSubDetailInDTO(idDetail, idSubDetail, dto);
        var obj = service.update(idSubDetail, converter.toEntity(dto));
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{idSubDetail}").buildAndExpand(dto.getId())
                .toUri();
        return ResponseEntity.created(uri).body(converter.toDto(obj));
    }

    @GetMapping(value = "/{idSubDetail}")
    public ResponseEntity<DTO> findById(@PathVariable Long idSubDetail) {
        var obj = findByIdSubDetail(idSubDetail);
        return ResponseEntity.ok(converter.toDto(obj));
    }

    protected GenericEntity findByIdSubDetail(Long idSubDetail) {
        return service.findById(idSubDetail);
    }

    @GetMapping
    public ResponseEntity<Page<DTO>> findAll(@PathVariable(value = "idDetail") Long idDetail, Pageable pageable) {
        Page<?> list = findAllByIdDetail(idDetail, pageable);
        Page<DTO> resultList = new PageImpl<DTO>(
                list.getContent().stream()
                        .map(obj -> converter.toDto(obj)).toList(), pageable, list.getTotalElements());
        return ResponseEntity.ok().body(resultList);
    }

    protected Page<?> findAllByIdDetail(Long idDetail, Pageable pageable) {
        return  service.findAllPaged(pageable);
    }

    @DeleteMapping(value = "/{idSubDetail}")
    public ResponseEntity<Void> deleteById(@PathVariable Long idSubDetail) {
        service.delete(idSubDetail);
        return ResponseEntity.noContent().build();
    }
}
