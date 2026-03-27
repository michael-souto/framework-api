package com.detrasoft.framework.api.dto;

import java.util.UUID;

public class DefaultDescriptionDTO implements GenericDTO {
    private UUID id;
    private String description;
    
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}
