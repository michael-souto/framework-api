package com.detrasoft.framework.api.dto;

public interface AuditedGenericDTO extends GenericDTO {

    AuditDTO getAudit();
    void setAudit(AuditDTO auditDTO) ;

//    Instant getCreatedAt() ;
//    void setCreatedAt(Instant createdAt) ;
//    Instant getUpdatedAt() ;
//    void setUpdatedAt(Instant updatedAt) ;
//    String getUserCreated() ;
//    void setUserCreated(String userCreated) ;
//    String getUserUpdated() ;
//    void setUserUpdated(String userUpdated);
}
