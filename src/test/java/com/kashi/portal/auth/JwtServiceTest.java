package com.kashi.portal.auth;

import com.kashi.portal.support.TestJwt;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(TestJwt.SECRET, 3_600_000L);

    @Test
    void tokenContainsTheEmailItWasCreatedFor() {
        String token = jwtService.generateToken("asha@example.com");

        assertEquals("asha@example.com", jwtService.extractEmail(token));
    }

    @Test
    void expirationIsReportedInSeconds() {
        assertEquals(3600L, jwtService.getExpirationSeconds());
    }

    @Test
    void expiredTokenIsRejected() {
        String expired = TestJwt.expiredToken("asha@example.com");

        assertThrows(ExpiredJwtException.class, () -> jwtService.extractEmail(expired));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String forged = TestJwt.tokenSignedWithOtherKey("asha@example.com");

        assertThrows(JwtException.class, () -> jwtService.extractEmail(forged));
    }

    @Test
    void tokenWithChangedPayloadIsRejected() {
        String[] parts = jwtService.generateToken("user@example.com").split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"admin@example.com\"}".getBytes(StandardCharsets.UTF_8));
        String tampered = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThrows(JwtException.class, () -> jwtService.extractEmail(tampered));
    }

    @Test
    void garbageTokenIsRejected() {
        assertThrows(JwtException.class, () -> jwtService.extractEmail("not-a-jwt"));
    }

    @Test
    void startupFailsWhenSecretIsMissing() {
        assertThrows(IllegalStateException.class, () -> new JwtService("", 3_600_000L));
    }

    @Test
    void startupFailsWhenSecretIsShorterThan32Bytes() {
        assertThrows(IllegalStateException.class, () -> new JwtService("too-short-secret", 3_600_000L));
    }

    @Test
    void startupFailsWhenSecretIsTheEnvExamplePlaceholder() {
        String placeholder = "replace_with_a_random_secret_at_least_32_bytes_long";

        assertThrows(IllegalStateException.class, () -> new JwtService(placeholder, 3_600_000L));
    }

    @Test
    void startupFailsWhenExpirationIsNotPositive() {
        assertThrows(IllegalStateException.class, () -> new JwtService(TestJwt.SECRET, 0L));
    }
}
