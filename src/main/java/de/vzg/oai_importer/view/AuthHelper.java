package de.vzg.oai_importer.view;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Helper for security checks inside jte templates.
 * It replaces the Thymeleaf attribute {@code sec:authorize} of thymeleaf-extras-springsecurity.
 */
public final class AuthHelper {

    private AuthHelper() {
    }

    /**
     * Checks whether the current user is authenticated without anonymous or remember me authentication.
     *
     * @return true if the current user is fully authenticated
     */
    public static boolean isFullyAuthenticated() {
        Authentication authentication = getAuthentication();
        return authentication != null && !(authentication instanceof RememberMeAuthenticationToken);
    }

    /**
     * Checks whether the current user has at least one of the given authorities.
     *
     * @param authorities the authorities to check
     * @return true if the current user has one of the authorities
     */
    public static boolean hasAnyAuthority(String... authorities) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return false;
        }

        Set<String> grantedAuthorities = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());

        return Arrays.stream(authorities).anyMatch(grantedAuthorities::contains);
    }

    private static Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication;
    }
}
