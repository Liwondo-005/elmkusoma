package tz.elmkusoma.common;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuditListener {

    @PrePersist
    public void prePersist(BaseEntity entity) {
        String currentUser = getCurrentUser();
        entity.setCreatedBy(currentUser);
        entity.setUpdatedBy(currentUser);
    }

    @PreUpdate
    public void preUpdate(BaseEntity entity) {
        entity.setUpdatedBy(getCurrentUser());
    }

    private String getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "system";
    }
}
