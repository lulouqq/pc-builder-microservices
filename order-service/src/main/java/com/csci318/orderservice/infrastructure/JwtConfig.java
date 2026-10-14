package com.csci318.orderservice.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * HS256 JWT validation with the secret shared by all services (security.jwt.secret / JWT_SECRET).
 * Tokens are issued by Account Service; this service only validates them.
 */
@Configuration
public class JwtConfig {

    static final int MIN_SECRET_BYTES = 32;

    /** Fails at startup when the secret is missing or shorter than 256 bits (required for HS256). */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${security.jwt.secret}") String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "security.jwt.secret (JWT_SECRET) must be at least " + MIN_SECRET_BYTES + " bytes long");
        }
        return NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
