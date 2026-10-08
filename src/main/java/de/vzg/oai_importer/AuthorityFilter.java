package de.vzg.oai_importer;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Restricts configured sources, targets and jobs to the entries the current user is allowed to see.
 * The {@link de.vzg.oai_importer.mycore.MyCoReAuthenticationProvider} grants an authority
 * {@code <prefix>-<id>} for every entry that belongs to the application the user logged in with.
 */
public final class AuthorityFilter {

    private AuthorityFilter() {
    }

    /**
     * Keeps only the entries for which the user holds the authority {@code <prefix>-<key>}.
     *
     * @param entries the configured entries, may be null
     * @param prefix the authority prefix, e.g. {@code source}, {@code target} or {@code job}
     * @param authentication the current user
     * @return the visible entries sorted by key, never null
     */
    public static <T> Map<String, T> filter(Map<String, T> entries, String prefix, Authentication authentication) {
        TreeMap<String, T> visible = new TreeMap<>();
        if (entries == null) {
            return visible;
        }

        Set<String> ids = getIds(prefix, authentication);
        entries.forEach((key, value) -> {
            if (ids.contains(key)) {
                visible.put(key, value);
            }
        });
        return visible;
    }

    /**
     * Returns the ids of all authorities {@code <prefix>-<id>} the user holds.
     *
     * @param prefix the authority prefix, e.g. {@code target}
     * @param authentication the current user
     * @return the ids, never null
     */
    public static Set<String> getIds(String prefix, Authentication authentication) {
        if (authentication == null) {
            return Collections.emptySet();
        }

        String authorityPrefix = prefix + "-";
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(authority -> authority != null && authority.startsWith(authorityPrefix))
            .map(authority -> authority.substring(authorityPrefix.length()))
            .collect(Collectors.toSet());
    }
}
