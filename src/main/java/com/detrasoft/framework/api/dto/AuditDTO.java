package com.detrasoft.framework.api.dto;

import lombok.Data;

import javax.persistence.Embeddable;
import java.time.Instant;

@Data
@Embeddable
public class AuditDTO {
    private Instant createdAt;
    private Instant updatedAt;
    private String userCreated;
    private String userUpdated;
}
