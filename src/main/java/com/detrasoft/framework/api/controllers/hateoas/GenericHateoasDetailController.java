package com.detrasoft.framework.api.controllers.hateoas;

import com.detrasoft.framework.api.controllers.jackson.ResponseView;
import com.detrasoft.framework.api.dto.GenericRepresentationModelDTO;
import com.detrasoft.framework.api.dto.converters.GenericRepresentationModelDTOAssembler;
import com.detrasoft.framework.crud.entities.GenericEntity;
import com.detrasoft.framework.crud.services.crud.GenericCRUDService;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.net.URI;

public abstract class GenericHateoasDetailController<DTO extends GenericRepresentationModelDTO<? extends DTO>> {

    protected GenericCRUDService service;
    protected GenericRepresentationModelDTOAssembler<GenericEntity, DTO> assembler;

    @Autowired
    protected PagedResourcesAssembler<DTO> pagedAssembler;

    protected abstract void setIdSubDetailInDTO(Long idDetail, Long idSubDetail, DTO dto);
    public GenericHateoasDetailController(GenericCRUDService<?> service,
                                          GenericRepresentationModelDTOAssembler assembler) {
        this.service = service;
        this.assembler = assembler;
    }

    @JsonView(ResponseView.post.class)
    @PostMapping
    public ResponseEntity<DTO> insert(@PathVariable(value = "idDetail") Long idDetail,
                                      @RequestBody @Valid DTO dto) {
        setIdSubDetailInDTO(idDetail, null, dto);
        var obj = service.insert(assembler.toEntity(dto));
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{idSubDetail}").buildAndExpand(dto.getId())
                .toUri();
        return ResponseEntity.created(uri).body(assembler.toModel(obj, idDetail));
    }

    @JsonView(ResponseView.put.class)
    @PutMapping(value = "/{idSubDetail}")
    public ResponseEntity<DTO> update(@PathVariable(value = "idDetail") Long idDetail,
                                      @PathVariable(value = "idSubDetail") Long idSubDetail,
                                      @RequestBody @Valid DTO dto) {
        dto.setId(idSubDetail);
        setIdSubDetailInDTO(idDetail, idSubDetail, dto);
        var obj = service.update(idSubDetail, assembler.toEntity(dto));
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{idSubDetail}").buildAndExpand(dto.getId())
                .toUri();
        return ResponseEntity.created(uri).body(assembler.toModel(obj, idDetail));
    }

    @JsonView(ResponseView.findById.class)
    @GetMapping(value = "/{idSubDetail}")
    public ResponseEntity<DTO> findById(@PathVariable(value = "idDetail") Long idDetail,
                                        @PathVariable Long idSubDetail) {
        var obj = findByIdSubDetail(idSubDetail);
        return ResponseEntity.ok(assembler.toModel(obj,idDetail));
    }

    protected GenericEntity findByIdSubDetail(Long idSubDetail) {
        return service.findById(idSubDetail);
    }

    @JsonView(ResponseView.findAll.class)
    @GetMapping
    public ResponseEntity<PagedModel<DTO>> findAll(@PathVariable(value = "idDetail") Long idDetail, Pageable pageable) {
        Page<? extends GenericEntity> list = findAllByIdDetail(idDetail, pageable);
        var paged = new PageImpl<DTO>(
                list.getContent().stream()
                        .map(obj -> assembler.toModel(obj, idDetail)).toList(), pageable, list.getTotalElements());
        return ResponseEntity.ok().body(pagedAssembler.toModel((Page) list, (RepresentationModelAssembler) assembler));
    }

    protected Page<? extends GenericEntity> findAllByIdDetail(Long idDetail, Pageable pageable) {
        return  service.findAllPaged(pageable);
    }

    @JsonView(ResponseView.delete.class)
    @DeleteMapping(value = "/{idSubDetail}")
    public ResponseEntity<Void> deleteById(@PathVariable Long idSubDetail) {
        service.delete(idSubDetail);
        return ResponseEntity.noContent().build();
    }

    @SuppressWarnings("unchecked")
    private Class<?> getGenericClass() {
        ParameterizedType type = (ParameterizedType) getClass().getGenericSuperclass();
        return ((Class<DTO>) (type).getActualTypeArguments()[0]);
    }

    @GetMapping(value = "/schema")
    public ResponseEntity<Object> schema() {
        try {
            return ResponseEntity.ok(getGenericClass().getDeclaredConstructor().newInstance());
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                 | NoSuchMethodException | SecurityException e) {
            return null;
        }
    }
}
