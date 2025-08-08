package com.detrasoft.framework.api.dto.converters;

import com.detrasoft.framework.api.dto.GenericDTO;
import com.detrasoft.framework.crud.entities.GenericEntity;
import org.springframework.beans.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;

@SuppressWarnings({"rawtypes", "unchecked"})
public class GenericEntityDTOConverter<Entity extends GenericEntity, DTO extends GenericDTO> {

    public Entity toEntity(DTO dto) {
        try {
            var entity = (Entity) ((Class) ((ParameterizedType) this.getClass().
                getGenericSuperclass()).getActualTypeArguments()[0]).getDeclaredConstructor().newInstance();
            copyDtoToEntity(dto, entity);
            return entity;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public DTO toDto(Object obj) {
        try {
            var dto = (DTO) ((Class) ((ParameterizedType) this.getClass().
                    getGenericSuperclass()).getActualTypeArguments()[1]).getDeclaredConstructor().newInstance();
            copyEntityToDto(obj, dto);
            return dto;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
    
    protected void copyDtoToEntity(DTO dto, Entity entity) {
        BeanUtils.copyProperties(dto, entity);
    }

    protected void copyEntityToDto(Object obj, DTO dto) {
        BeanUtils.copyProperties(obj, dto);
    }
}
