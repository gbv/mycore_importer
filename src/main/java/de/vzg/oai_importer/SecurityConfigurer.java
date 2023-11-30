package de.vzg.oai_importer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import de.vzg.oai_importer.mycore.MyCoReAuthenticationProvider;

@EnableWebSecurity
@EnableGlobalMethodSecurity(
        securedEnabled = true,
        jsr250Enabled = true,
        prePostEnabled = true)
@Configuration
public class SecurityConfigurer {

    @Autowired
    private MyCoReAuthenticationProvider myCoReAuthenticationProvider;

    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder
            = http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.authenticationProvider(myCoReAuthenticationProvider);


        return authenticationManagerBuilder.build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers(AntPathRequestMatcher.antMatcher("/webjars/**"),
                                        AntPathRequestMatcher.antMatcher("/")).permitAll() // Erlaubt allen Nutzern den Zugriff auf WebJars und auf die Startseite
                                .anyRequest().authenticated() // Alle anderen Anfragen müssen authentifiziert werden
                )
                .formLogin(formLogin ->
                        formLogin.loginPage("/login").permitAll() // Konfiguration für das Formular-Login
                ).logout(logout ->
                        logout.logoutUrl("/logout").permitAll() // Konfiguration für das Logout
                );

        return http.build();
    }
}
