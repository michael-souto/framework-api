package com.detrasoft.framework.api.dto;

import org.springframework.hateoas.RepresentationModel;

public abstract class GenericRepresentationModelDTO<T extends RepresentationModel<? extends T>>
        extends RepresentationModel<T> implements GenericDTO {
}
