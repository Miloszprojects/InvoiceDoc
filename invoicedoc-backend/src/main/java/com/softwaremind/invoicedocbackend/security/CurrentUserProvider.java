package com.softwaremind.invoicedocbackend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    public CurrentUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new IllegalStateException("No authenticated user in context");
        }

        return new CurrentUser(
                principal.getUserId(),
                principal.getOrganizationId(),
                principal.getRole()
        );
    }
}
