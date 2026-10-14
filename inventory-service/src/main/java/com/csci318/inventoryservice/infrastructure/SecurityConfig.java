package com.csci318.inventoryservice.infrastructure;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

/**
 * Stateless JWT resource server (Architecture Decision 11). Paths are relative to the /api context path.
 * Public: GET /inventory/{productId} and the H2 console. POST /inventory/{productId}/notifications needs a
 * token (the owner check on the body's userId is done in the controller). /users/{userId}/** (I2 stock
 * alerts) only for the token's own user, checked before any lookup; ADMIN does not bypass it. Every other
 * inventory endpoint (I3 PUT, I1 REST) needs the ADMIN role.
 */
@Configuration
public class SecurityConfig {

    private static final String ROLE_CLAIM = "role";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationEntryPoint authenticationEntryPoint,
                                                   AccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // The H2 console is rendered in frames.
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        // Error pages for responses that were already authorised (e.g. 404, 405).
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(PathRequest.toH2Console()).permitAll()
                        .requestMatchers(antMatcher(HttpMethod.GET, "/inventory/{productId}")).permitAll()
                        .requestMatchers(antMatcher(HttpMethod.POST, "/inventory/{productId}/notifications"))
                        .authenticated()
                        .requestMatchers(antMatcher("/inventory/**")).hasRole("ADMIN")
                        .requestMatchers(antMatcher("/users/{userId}/**")).access(ownUser())
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }

    /** Grants access only when the token's subject (user id) equals the {userId} path variable. */
    private static AuthorizationManager<RequestAuthorizationContext> ownUser() {
        return (authentication, context) -> new AuthorizationDecision(
                authentication.get() instanceof JwtAuthenticationToken token
                        && token.getName().equals(context.getVariables().get("userId")));
    }

    /** Maps the role claim (CUSTOMER / ADMIN) to the authority ROLE_CUSTOMER / ROLE_ADMIN. */
    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(ROLE_CLAIM);
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
