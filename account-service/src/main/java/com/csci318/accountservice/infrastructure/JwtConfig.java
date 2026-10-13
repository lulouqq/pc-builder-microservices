package com.csci318.accountservice.infrastructure;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * HS256 JWT signing and validation with the secret shared by all services (security.jwt.secret / JWT_SECRET).
 */
@Configuration
public class JwtConfig {

    static final int MIN_SECRET_BYTES = 32;

    @Bean
    public SecretKey jwtSecretKey(@Value("${security.jwt.secret}") String secret) {
        return secretKey(secret);
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    /** Fails at startup when the secret is missing or shorter than 256 bits (required for HS256). */
    static SecretKey secretKey(String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "security.jwt.secret (JWT_SECRET) must be at least " + MIN_SECRET_BYTES + " bytes long");
        }
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
