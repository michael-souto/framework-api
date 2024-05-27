package com.detrasoft.framework.api.controllers.hateoas;

import com.detrasoft.framework.api.controllers.jackson.ResponseView;
import com.detrasoft.framework.api.dto.GenericRepresentationModelDTO;
import com.detrasoft.framework.api.dto.converters.GenericRepresentationModelDTOAssembler;
import com.detrasoft.framework.crud.entities.GenericEntity;
import com.detrasoft.framework.crud.services.crud.GenericCRUDService;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.lang.reflect.ParameterizedType;
import java.util.UUID;

public class GenericHateoasCRUDController<DTO extends GenericRepresentationModelDTO<? extends DTO>> {

    protected GenericCRUDService service;
    protected GenericRepresentationModelDTOAssembler<GenericEntity, DTO> assembler;

    @Autowired
    protected PagedResourcesAssembler<DTO> pagedAssembler;

    public GenericHateoasCRUDController(GenericCRUDService service,
                                        GenericRepresentationModelDTOAssembler assembler) {
        this.service = service;
        this.assembler = assembler;
    }

    @JsonView(ResponseView.findAll.class)
    @GetMapping
    public ResponseEntity<PagedModel<DTO>> findAll(Pageable pageable) {
        Page<? extends GenericEntity> list = getAllPaged(pageable);
        var paged = new PageImpl<DTO>(
                list.getContent().stream()
                        .map(obj -> assembler.toModel(obj, true)).toList(), pageable, list.getTotalElements());
        return ResponseEntity.ok().body(pagedAssembler.toModel((Page) list, (RepresentationModelAssembler) assembler));
    }
    protected Page<GenericEntity> getAllPaged(Pageable pageable) {
        return service.findAllPaged(pageable);
    }

    @JsonView(ResponseView.findById.class)
    @GetMapping(value = "/{id}")
    public ResponseEntity<DTO> findById(@PathVariable String id) {
        var dto = getOne(id);
        return ResponseEntity.ok().body(assembler.toModel(dto,true));
    }

    protected GenericEntity getOne(String id) {
        return service.findById(UUID.fromString(id));
    }

    @JsonView(ResponseView.post.class)
    @PostMapping
    public ResponseEntity<DTO> insert(@RequestBody @Valid DTO dto) {
        var newObj = service.insert(assembler.toEntity(dto));
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(newObj.getId()).toUri();
        return ResponseEntity.created(uri).body(assembler.toModel(newObj,true));
    }

    @JsonView(ResponseView.put.class)
    @PutMapping(value = "/{id}")
    public ResponseEntity<DTO> update(@PathVariable UUID id, @RequestBody @Valid DTO dto) {
        dto.setId(id);
        var newObj = service.update(id, assembler.toEntity(dto));
        return ResponseEntity.ok().body(assembler.toModel(newObj, true));
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
                return ResponseEntity.ok().body(assembler.toModel(finalNewDto));
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
