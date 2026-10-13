package com.csci318.accountservice.infrastructure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtConfigTest {

    @Test
    void secretShorterThan256BitsIsRejected() {
        assertThatThrownBy(() -> JwtConfig.secretKey("too-short"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
        assertThatThrownBy(() -> JwtConfig.secretKey(null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void secretOf256BitsIsAccepted() {
        assertThat(JwtConfig.secretKey("a".repeat(32)).getAlgorithm()).isEqualTo("HmacSHA256");
    }
}
