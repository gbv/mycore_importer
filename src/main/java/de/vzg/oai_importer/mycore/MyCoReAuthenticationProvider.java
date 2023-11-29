package de.vzg.oai_importer.mycore;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import de.vzg.oai_importer.ImporterConfiguration;
import de.vzg.oai_importer.mapping.MappingService;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class MyCoReAuthenticationProvider implements AuthenticationProvider {

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
        if (split.length == 2) {
            String user = split[0];
            String realm = split[1];

            if(configuration.getTargets().containsKey(realm)){
                MyCoReTargetConfiguration targetConfig = configuration.getTargets().get(realm);
                try {
                    MyCoReRestAPIService.UserInfo authenticate = myCoReRestAPIService.authenticate(targetConfig, user, password);
                    List<GrantedAuthority> authorities = new ArrayList<>();
                    if(authenticate.roles().contains("admin")){
                        authorities.add(() -> "mapping-admin");
                    }
                    if(authenticate.roles().contains("importer") || authenticate.roles().contains("admin")){
                        authorities.add(() -> "source");
                        authorities.add(() -> "source-"+realm);
                        getJobsWithRealm(realm).forEach(jobID -> {
                            String sourceConfigId = configuration.getJobs().get(jobID).getSourceConfigId();
                            authorities.add(() -> "source-"+sourceConfigId);
                        });

                        authorities.add(() -> "target");
                        authorities.add(() -> "target-"+realm);

                        authorities.add(() -> "job");
                        authorities.add(() -> "job-"+realm);
                        getJobsWithRealm(realm).forEach(jobID -> {
                            authorities.add(() -> "job-"+jobID);
                        });

                        authorities.add(() -> "mapping");
                        mappingService.getGroupsByTarget(realm).forEach(group -> {
                            authorities.add(() -> "mapping-"+group.getId());
                        });
                    }
                    return new UsernamePasswordAuthenticationToken(authenticate.id(), password, authorities);
                } catch (IOException|URISyntaxException e) {
                    throw new AuthenticationException("Error while authenticating against MyCoRe", e) {
                    };
                }
            } else {
                throw new BadCredentialsException("Realm not found: " + realm);
            }
        } else {
            throw new BadCredentialsException("Invalid username: " + name);
        }
    }

    public List<String> getJobsWithRealm(String realm){
        return configuration.getJobs().entrySet().stream()
                .filter(e -> e.getValue().getTargetConfigId().equals(realm))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }



    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
