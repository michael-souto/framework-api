package com.detrasoft.framework.api.utils;

import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.detrasoft.framework.crud.entities.GenericEntity;

public class GenericSpecs {
    public static Specification<GenericEntity> byIds(List<UUID> ids) {
        return (root, query, builder) -> builder.in(root.get("id")).value(ids);
    };
}
