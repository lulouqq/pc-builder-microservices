package com.csci318.accountservice.infrastructure;

import com.csci318.accountservice.domain.Role;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Issues the access token returned by POST /auth/login: claims sub (user id), role, iat, exp; valid for 1 hour.
 */
@Component
public class JwtTokenService {

    public static final String ROLE_CLAIM = "role";
    public static final Duration TOKEN_LIFETIME = Duration.ofHours(1);

    private final JwtEncoder jwtEncoder;

    public JwtTokenService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public IssuedToken issue(String userId, Role role) {
        // JWT timestamps have second precision.
        Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plus(TOKEN_LIFETIME);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId)
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }
}
