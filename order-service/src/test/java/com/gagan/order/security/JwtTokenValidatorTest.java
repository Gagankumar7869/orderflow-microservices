package com.gagan.order.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenValidatorTest {

    private static final String TEST_SECRET =
            "b3JkZXJmbG93LWlkZW50aXR5LXNlcnZpY2Utc3VwZXItc2VjcmV0LWtleS0yMDI2LWdhZ2Fu";
    private static final String DIFFERENT_SECRET =
            "YW5vdGhlci1kaWZmZXJlbnQtc2VjcmV0LWtleS1mb3ItdGVzdGluZy10YW1wZXJpbmctMTIz";

    private final SecretKey signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));

    @Test
    void shouldFailClosedWhenSecretIsBlank() {
        assertThatThrownBy(() -> new JwtTokenValidator("", "", ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Fail-closed security error");
    }

    @Test
    void shouldValidateValidTokenAndExtractPrincipal() {
        JwtTokenValidator validator = new JwtTokenValidator(TEST_SECRET, "", "");

        String token = Jwts.builder()
                .subject("gagan@orderflow.com")
                .claims(Map.of("userId", 101L, "role", "CUSTOMER"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(signingKey)
                .compact();

        OrderUserPrincipal principal = validator.validateAndParse(token);

        assertThat(principal.userId()).isEqualTo(101L);
        assertThat(principal.email()).isEqualTo("gagan@orderflow.com");
        assertThat(principal.role()).isEqualTo("CUSTOMER");
    }

    @Test
    void shouldRejectExpiredToken() {
        JwtTokenValidator validator = new JwtTokenValidator(TEST_SECRET, "", "");

        String expiredToken = Jwts.builder()
                .subject("gagan@orderflow.com")
                .claims(Map.of("userId", 101L, "role", "CUSTOMER"))
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(signingKey)
                .compact();

        assertThatThrownBy(() -> validator.validateAndParse(expiredToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTamperedSignature() {
        JwtTokenValidator validator = new JwtTokenValidator(TEST_SECRET, "", "");
        SecretKey wrongKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(DIFFERENT_SECRET));

        String tamperedToken = Jwts.builder()
                .subject("gagan@orderflow.com")
                .claims(Map.of("userId", 101L, "role", "ADMIN"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(wrongKey)
                .compact();

        assertThatThrownBy(() -> validator.validateAndParse(tamperedToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenMissingUserIdOrRole() {
        JwtTokenValidator validator = new JwtTokenValidator(TEST_SECRET, "", "");

        String missingRoleToken = Jwts.builder()
                .subject("gagan@orderflow.com")
                .claims(Map.of("userId", 101L))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(signingKey)
                .compact();

        assertThatThrownBy(() -> validator.validateAndParse(missingRoleToken))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Missing required userId or role claim");
    }

    @Test
    void shouldEnforceIssuerAndAudienceWhenConfigured() {
        JwtTokenValidator validator = new JwtTokenValidator(
                TEST_SECRET, "orderflow-identity", "orderflow-services");

        String validToken = Jwts.builder()
                .subject("gagan@orderflow.com")
                .issuer("orderflow-identity")
                .audience().add("orderflow-services").and()
                .claims(Map.of("userId", 101L, "role", "CUSTOMER"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(signingKey)
                .compact();

        OrderUserPrincipal principal = validator.validateAndParse(validToken);
        assertThat(principal.userId()).isEqualTo(101L);

        String wrongIssuerToken = Jwts.builder()
                .subject("gagan@orderflow.com")
                .issuer("unknown-issuer")
                .audience().add("orderflow-services").and()
                .claims(Map.of("userId", 101L, "role", "CUSTOMER"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(signingKey)
                .compact();

        assertThatThrownBy(() -> validator.validateAndParse(wrongIssuerToken))
                .isInstanceOf(JwtException.class);
    }
}