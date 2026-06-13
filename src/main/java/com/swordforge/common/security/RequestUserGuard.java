package com.swordforge.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class RequestUserGuard {

    private final boolean securityEnabled;

    public RequestUserGuard(@Value("${app.security.enabled:true}") boolean securityEnabled) {
        this.securityEnabled = securityEnabled;
    }

    public void requireSelfOrAdmin(String userId) {
        if (!securityEnabled) {
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertAuthenticated(authentication);
        if (isAdmin(authentication) || authentication.getName().equals(userId)) {
            return;
        }
        throw new AccessDeniedException("cannot access another user's data");
    }

    public void requireAdmin() {
        if (!securityEnabled) {
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertAuthenticated(authentication);
        if (isAdmin(authentication)) {
            return;
        }
        throw new AccessDeniedException("admin role required");
    }

    private void assertAuthenticated(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("authentication required");
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
