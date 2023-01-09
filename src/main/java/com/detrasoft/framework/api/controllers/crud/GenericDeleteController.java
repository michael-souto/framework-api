package com.detrasoft.framework.api.controllers.crud;

import com.detrasoft.framework.api.dto.GenericDTO;
import com.detrasoft.framework.crud.services.crud.GenericCRUDService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;

public class GenericDeleteController <DTO extends GenericDTO> {

    protected GenericCRUDService<?> service;

    public GenericDeleteController(GenericCRUDService<?> service) {
        this.service = service;
    }

    @DeleteMapping(value = "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
