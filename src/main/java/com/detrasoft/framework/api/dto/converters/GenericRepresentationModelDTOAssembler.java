package com.detrasoft.framework.api.dto.converters;

import com.detrasoft.framework.api.dto.GenericRepresentationModelDTO;
import com.detrasoft.framework.crud.entities.GenericEntity;
import org.springframework.beans.BeanUtils;
import org.springframework.hateoas.*;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@SuppressWarnings({"rawtypes", "unchecked", "null"})
public class GenericRepresentationModelDTOAssembler<Entity, DTO extends GenericRepresentationModelDTO<? extends DTO>>
        extends RepresentationModelAssemblerSupport<Entity, DTO>
        implements RepresentationModelAssembler<Entity, DTO> {

    public GenericRepresentationModelDTOAssembler(Class<?> controllerClass, Class<DTO> resourceType) {
        super(controllerClass, resourceType);
    }

    @Override
    public DTO toModel(Entity entity) {
        try {
            var dto = instantiateModel(entity);
            copyEntityToDto(entity, dto);
            return dto;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public DTO toModel(Entity entity, boolean withLinkController) {
        try {
            var dto = createModelWithId(((GenericEntity)entity).getId(), entity);
            copyEntityToDto(entity, dto);
            if (withLinkController) {
                dto.add(linkTo(getControllerClass()).withRel(IanaLinkRelations.COLLECTION_VALUE));

                TemplateVariables pageVariables = new TemplateVariables(
                        new TemplateVariable("page", TemplateVariable.VariableType.REQUEST_PARAM),
                        new TemplateVariable("size", TemplateVariable.VariableType.REQUEST_PARAM),
                        new TemplateVariable("sort", TemplateVariable.VariableType.REQUEST_PARAM));

                String url = linkTo(getControllerClass()).toUri().toString();

                dto.add(Link.of(UriTemplate.of(url, pageVariables), LinkRelation.of(IanaLinkRelations.COLLECTION_VALUE)));

            }
            return dto;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public DTO toModel(Entity entity, Object uriParans) {
        try {
            var dto = createModelWithId(((GenericEntity)entity).getId(), entity, uriParans);
            copyEntityToDto(entity, dto);
            dto.add(linkTo(getControllerClass(), uriParans).withRel(IanaLinkRelations.COLLECTION_VALUE));

            TemplateVariables pageVariables = new TemplateVariables(
                    new TemplateVariable("page", TemplateVariable.VariableType.REQUEST_PARAM),
                    new TemplateVariable("size", TemplateVariable.VariableType.REQUEST_PARAM),
                    new TemplateVariable("sort", TemplateVariable.VariableType.REQUEST_PARAM));

            String url = linkTo(getControllerClass(), uriParans).toUri().toString();

            dto.add(Link.of(UriTemplate.of(url, pageVariables), LinkRelation.of(IanaLinkRelations.COLLECTION_VALUE)));

            return dto;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void copyEntityToDto(Entity obj, DTO dto) {
        BeanUtils.copyProperties(obj, dto);
    }

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

    protected void copyDtoToEntity(DTO dto, Entity entity) {
        BeanUtils.copyProperties(dto, entity);
    }
}
