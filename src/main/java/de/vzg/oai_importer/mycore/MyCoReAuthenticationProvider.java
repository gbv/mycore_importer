package de.vzg.oai_importer.mycore;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import de.vzg.oai_importer.ImportJobConfiguration;
import de.vzg.oai_importer.ImporterConfiguration;
import de.vzg.oai_importer.mapping.MappingService;
import lombok.extern.log4j.Log4j2;

/**
 * Authenticates users against the MyCoRe instance of a target. The username has the form {@code <user>@<target-id>},
 * the target id selects the MyCoRe instance the credentials are checked against.
 * <p>
 * The roles of the MyCoRe user decide which targets are accessible. Roles are only valid for targets which use the
 * same MyCoRe instance (same url) as the target the user logged in with:
 * <ul>
 * <li>{@code admin} grants everything below for all targets of the instance and allows to manage mapping groups</li>
 * <li>{@code importer} grants the sources, jobs and mapping groups of all targets of the instance</li>
 * <li>{@code importer-<target-id>} grants the sources, jobs and mapping groups of the named target</li>
 * <li>{@code mapper} grants the mapping groups used by the jobs of all targets of the instance</li>
 * <li>{@code mapper-<target-id>} grants the mapping groups used by the jobs of the named target</li>
 * </ul>
 * A mapping group is used by a job, if its name is a value of the importer config of the job.
 * The MyCoRe instance has to pass these roles in the JWT, so they have to be listed in {@code MCR.Rest.JWT.Roles}.
 */
@Log4j2
@Component
public class MyCoReAuthenticationProvider implements AuthenticationProvider {

    public static final String ROLE_ADMIN = "admin";

    public static final String ROLE_IMPORTER = "importer";

    public static final String ROLE_MAPPER = "mapper";

    @Autowired
    private ImporterConfiguration configuration;

    @Autowired
    private MyCoReRestAPIService myCoReRestAPIService;

    @Autowired
    private MappingService mappingService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String name = authentication.getName();
        String password = authentication.getCredentials().toString();

        String[] split = name.split("@");
        if (split.length != 2) {
            throw new BadCredentialsException("Invalid username: " + name);
        }

        String user = split[0];
        String realm = split[1];
        MyCoReTargetConfiguration targetConfig = getTargets().get(realm);
        if (targetConfig == null) {
            throw new BadCredentialsException("Realm not found: " + realm);
        }

        try {
            MyCoReRestAPIService.UserInfo userInfo = myCoReRestAPIService.authenticate(targetConfig, user, password);
            List<GrantedAuthority> authorities = createAuthorities(realm, userInfo.roles());
            return new UsernamePasswordAuthenticationToken(userInfo.id(), password, authorities);
        } catch (IOException | URISyntaxException e) {
            throw new AuthenticationException("Error while authenticating against MyCoRe", e) {
            };
        }
    }

    /**
     * Creates the authorities for a user who logged in with the given realm and holds the given MyCoRe roles.
     *
     * @param loginRealm the target id the user logged in with
     * @param roles the MyCoRe roles of the user, may be null
     * @return the authorities, never null
     */
    List<GrantedAuthority> createAuthorities(String loginRealm, Collection<String> roles) {
        Set<String> authorities = new LinkedHashSet<>();
        if (roles != null && roles.contains(ROLE_ADMIN)) {
            authorities.add("mapping-admin");
        }

        Set<String> importerRealms = getAccessibleRealms(loginRealm, roles, ROLE_IMPORTER);
        if (!importerRealms.isEmpty()) {
            authorities.add("source");
            authorities.add("target");
            authorities.add("job");
            authorities.add("mapping");
        }

        importerRealms.forEach(realm -> {
            authorities.add("source-" + realm);
            authorities.add("target-" + realm);
            authorities.add("job-" + realm);
            getJobsWithRealm(realm).forEach(jobID -> {
                authorities.add("source-" + configuration.getJobs().get(jobID).getSourceConfigId());
                authorities.add("job-" + jobID);
            });
            mappingService.getGroupsByTarget(realm).forEach(group -> authorities.add("mapping-" + group.getId()));
        });

        Set<String> mapperRealms = getAccessibleRealms(loginRealm, roles, ROLE_MAPPER);
        if (!mapperRealms.isEmpty()) {
            authorities.add("mapping");
        }
        mappingService.getGroupsByNames(getMappingGroupNames(mapperRealms))
            .forEach(group -> authorities.add("mapping-" + group.getId()));

        log.debug("Granted importer realms {} and mapper realms {} for login realm {} and roles {}", importerRealms,
            mapperRealms, loginRealm, roles);
        return authorities.stream().<GrantedAuthority>map(SimpleGrantedAuthority::new).toList();
    }

    /**
     * Returns the targets a user may access with the given role. The role itself and {@code admin} grant all targets,
     * {@code <role>-<target-id>} grants a single target. Only targets which use the same MyCoRe instance as the login
     * realm are considered, because the roles of the user are only valid for the instance the user authenticated
     * against.
     *
     * @param loginRealm the target id the user logged in with
     * @param roles the MyCoRe roles of the user, may be null
     * @param role the role, e.g. {@code importer} or {@code mapper}
     * @return the accessible target ids, sorted, never null
     */
    Set<String> getAccessibleRealms(String loginRealm, Collection<String> roles, String role) {
        Set<String> realms = new TreeSet<>();
        MyCoReTargetConfiguration loginTarget = getTargets().get(loginRealm);
        String loginUrl = loginTarget == null ? null : normalizeUrl(loginTarget.getUrl());
        if (roles == null || loginUrl == null || loginUrl.isEmpty()) {
            return realms;
        }

        boolean allRealms = roles.contains(ROLE_ADMIN) || roles.contains(role);
        getTargets().forEach((realm, target) -> {
            if (loginUrl.equals(normalizeUrl(target.getUrl()))
                && (allRealms || roles.contains(role + "-" + realm))) {
                realms.add(realm);
            }
        });
        return realms;
    }

    /**
     * Returns the ids of all jobs which import into the given target.
     *
     * @param realm the target id
     * @return the job ids, never null
     */
    public List<String> getJobsWithRealm(String realm) {
        Map<String, ImportJobConfiguration> jobs = configuration.getJobs();
        if (jobs == null) {
            return List.of();
        }
        return jobs.entrySet().stream()
            .filter(e -> Objects.equals(e.getValue().getTargetConfigId(), realm))
            .map(Map.Entry::getKey)
            .toList();
    }

    /**
     * Returns the values of the importer config of all jobs which import into one of the given targets. Mapping groups
     * are referenced by their name in the importer config, e.g. {@code importer-config.genre=<group name>}.
     *
     * @param realms the target ids
     * @return the values, never null
     */
    private Set<String> getMappingGroupNames(Set<String> realms) {
        Set<String> names = new TreeSet<>();
        realms.forEach(realm -> getJobsWithRealm(realm).forEach(jobID -> {
            Map<String, String> importerConfig = configuration.getJobs().get(jobID).getImporterConfig();
            if (importerConfig != null) {
                importerConfig.values().stream()
                    .filter(value -> value != null && !value.isBlank())
                    .forEach(names::add);
            }
        }));
        return names;
    }

    private Map<String, MyCoReTargetConfiguration> getTargets() {
        Map<String, MyCoReTargetConfiguration> targets = configuration.getTargets();
        return targets == null ? Map.of() : targets;
    }

    private static String normalizeUrl(String url) {
        if (url == null) {
            return null;
        }
        String normalized = url.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
