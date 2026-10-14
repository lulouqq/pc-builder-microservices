package com.csci318.recommendationservice;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Creates Bearer tokens like Account Service does (HS256, claims sub/role/iat/exp), signed with the test secret
 * from application-test.yml.
 */
public final class TestTokens {

    public static final String SECRET = "test-only-jwt-secret-not-for-production-use";

    private TestTokens() {
    }

    public static String bearer(String userId) {
        return bearer(userId, SECRET);
    }

    public static String bearer(String userId, String secret) {
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId)
                .claim("role", "CUSTOMER")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build();
        String token = new NimbusJwtEncoder(new ImmutableSecret<>(key))
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return "Bearer " + token;
    }
}
