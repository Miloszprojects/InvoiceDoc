package com.softwaremind.invoicedocbackend.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    // min. 32 znaki pod HS256 (256 bitów)
    private static final String SECRET_1 = "SuperTajnySekretDoTestow1234567890";
    private static final String SECRET_2 = "InnySekretDoTestowJwtService987654321";

    private static final long EXPIRATION_SECONDS = 3600L; // 1h

    private JwtService createServiceWithSecret(String secret, long expirationSeconds) {
        return new JwtService(secret, expirationSeconds);
    }

    @Test
    @DisplayName("generateToken should create a JWT that can be parsed and contains expected claims")
    void generateTokenShouldCreateParsableJwtWithExpectedClaims() {
        // given
        JwtService service = createServiceWithSecret(SECRET_1, EXPIRATION_SECONDS);

        String username = "john.doe";
        Long userId = 42L;
        Long orgId = 7L;
        String role = "ADMIN";

        // when
        String token = service.generateToken(username, userId, orgId, role);
        Jws<Claims> jws = service.parseToken(token);
        Claims claims = jws.getBody();

        // then
        assertAll(
                () -> assertThat(token).isNotNull().isNotEmpty(),
                () -> assertThat(claims.getSubject()).isEqualTo(username),
                () -> assertThat(claims.get("uid", Long.class)).isEqualTo(userId),
                () -> assertThat(claims.get("org", Long.class)).isEqualTo(orgId),
                () -> assertThat(claims.get("role", String.class)).isEqualTo(role),
                () -> assertThat(claims.getIssuedAt()).isNotNull(),
                () -> assertThat(claims.getExpiration()).isNotNull()
        );
    }

    @Test
    @DisplayName("generated token should have expiration roughly now + expirationSeconds")
    void generatedTokenShouldHaveProperExpiration() {
        // given
        long expirationSeconds = 120L; // 2 min
        JwtService service = createServiceWithSecret(SECRET_1, expirationSeconds);

        String username = "user";
        Long userId = 1L;
        Long orgId = 2L;
        String role = "USER";

        long beforeMillis = System.currentTimeMillis();

        // when
        String token = service.generateToken(username, userId, orgId, role);
        long afterMillis = System.currentTimeMillis();

        Jws<Claims> jws = service.parseToken(token);
        Claims claims = jws.getBody();

        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();

        // then
        long expectedDeltaMillis = expirationSeconds * 1000L;
        long actualDeltaMillis = expiration.getTime() - issuedAt.getTime();

        assertAll(
                () -> assertThat(issuedAt.getTime())
                        .isBetween(beforeMillis - 1000L, afterMillis + 1000L),
                () -> assertThat(actualDeltaMillis)
                        .isBetween(expectedDeltaMillis - 1000L, expectedDeltaMillis + 1000L)
        );
    }

    @Test
    @DisplayName("parseToken should throw JwtException when token is invalid")
    void parseTokenShouldThrowWhenTokenInvalid() {
        // given
        JwtService service = createServiceWithSecret(SECRET_1, EXPIRATION_SECONDS);

        String invalidToken = "this.is.not.a.valid.jwt";

        // when / then
        assertThrows(JwtException.class, () -> service.parseToken(invalidToken));
    }

    @Test
    @DisplayName("parseToken should fail when token was signed with different secret")
    void parseTokenShouldFailWhenSignedWithDifferentSecret() {
        // given
        JwtService serviceWithSecret1 = createServiceWithSecret(SECRET_1, EXPIRATION_SECONDS);
        JwtService serviceWithSecret2 = createServiceWithSecret(SECRET_2, EXPIRATION_SECONDS);

        String tokenFromSecret1 = serviceWithSecret1.generateToken("user", 1L, 2L, "USER");

        // when / then
        assertThrows(JwtException.class, () -> serviceWithSecret2.parseToken(tokenFromSecret1));
    }

    @Test
    @DisplayName("generateToken should include all custom claims even with null orgId")
    void generateTokenShouldIncludeClaimsWithNullOrgId() {
        // given
        JwtService service = createServiceWithSecret(SECRET_1, EXPIRATION_SECONDS);

        String username = "no-org-user";
        Long userId = 99L;
        Long orgId = null;
        String role = "OWNER";

        // when
        String token = service.generateToken(username, userId, orgId, role);
        Jws<Claims> jws = service.parseToken(token);
        Claims claims = jws.getBody();

        // then
        assertAll(
                () -> assertThat(claims.getSubject()).isEqualTo(username),
                () -> assertThat(claims.get("uid", Long.class)).isEqualTo(userId),
                () -> assertThat(claims.get("org")).isNull(),
                () -> assertThat(claims.get("role", String.class)).isEqualTo(role)
        );
    }
}
