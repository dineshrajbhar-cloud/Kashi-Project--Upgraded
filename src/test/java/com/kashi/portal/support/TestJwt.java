package com.kashi.portal.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/** Test-only secret and helpers that build tokens JwtService would reject. */
public final class TestJwt {

    public static final String SECRET = "unit-test-secret-key-with-more-than-32-bytes";

    private TestJwt() {
    }

    public static String expiredToken(String email) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date(now - 7_200_000))
                .expiration(new Date(now - 3_600_000))
                .signWith(key(SECRET), Jwts.SIG.HS256)
                .compact();
    }

    public static String tokenSignedWithOtherKey(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(key("a-completely-different-secret-key-32-bytes!"), Jwts.SIG.HS256)
                .compact();
    }

    private static SecretKey key(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
