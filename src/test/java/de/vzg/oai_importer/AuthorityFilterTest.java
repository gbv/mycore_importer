package de.vzg.oai_importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class AuthorityFilterTest {

    private Authentication user(String... authorities) {
        return new UsernamePasswordAuthenticationToken("user", "password",
            List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList());
    }

    @Test
    public void keepsOnlyEntriesWithMatchingAuthority() {
        Map<String, String> jobs = Map.of("import-dfi", "a", "import-other", "b", "import-zenodo", "c");

        Map<String, String> visible = AuthorityFilter.filter(jobs, "job",
            user("job", "job-dfi", "job-import-dfi", "job-import-zenodo", "source-import-other"));

        assertEquals(List.of("import-dfi", "import-zenodo"), List.copyOf(visible.keySet()));
    }

    @Test
    public void extractsIdsOfPrefix() {
        assertEquals(Set.of("dfi"),
            AuthorityFilter.getIds("target", user("target", "target-dfi", "job-target-other", "mapping-7")));
    }

    @Test
    public void showsNothingWithoutAuthentication() {
        assertTrue(AuthorityFilter.filter(Map.of("dfi", "a"), "target", null).isEmpty());
    }

    @Test
    public void handlesMissingConfiguration() {
        assertTrue(AuthorityFilter.filter(null, "source", user("source-gvk")).isEmpty());
    }
}
