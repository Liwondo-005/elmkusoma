package tz.elmkusoma.common;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class AuditListener {

    @PrePersist
    public void prePersist(BaseEntity entity) {
        // Audit trail will be populated by service layer
        // where security context is available
    }

    @PreUpdate
    public void preUpdate(BaseEntity entity) {
        // Audit trail will be populated by service layer
        // where security context is available
    }
}
