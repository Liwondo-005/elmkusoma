package tz.elmkusoma.config.security;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class OrganizationContextHolder {

    private OrganizationContext context;

    public void setContext(OrganizationContext context) {
        this.context = context;
    }

    public OrganizationContext getContext() {
        return context;
    }

    public void clear() {
        this.context = null;
    }

    public boolean hasContext() {
        return context != null;
    }
}