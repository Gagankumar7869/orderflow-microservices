package com.gagan.order.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;

@Component
@Profile("!local")
public class JwtTokenValidator {

    private final SecretKey signingKey;
    private final String expectedIssuer;
    private final String expectedAudience;

    public JwtTokenValidator(
            @Value("${security.jwt.secret:}") String secret,
            @Value("${security.jwt.issuer:}") String expectedIssuer,
            @Value("${security.jwt.audience:}") String expectedAudience) {

        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException(
                    "Fail-closed security error: security.jwt.secret (JWT_SECRET) must be configured for non-local profiles."
            );
        }
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expectedIssuer = expectedIssuer;
        this.expectedAudience = expectedAudience;
    }

    public OrderUserPrincipal validateAndParse(String token) {
        JwtParserBuilder parserBuilder = Jwts.parser().verifyWith(signingKey);

        if (StringUtils.hasText(expectedIssuer)) {
            parserBuilder.requireIssuer(expectedIssuer);
        }
        if (StringUtils.hasText(expectedAudience)) {
            parserBuilder.requireAudience(expectedAudience);
        }

        Claims claims = parserBuilder.build()
                .parseSignedClaims(token)
                .getPayload();

        Number userIdClaim = claims.get("userId", Number.class);
        String roleClaim = claims.get("role", String.class);
        String email = claims.getSubject();

        if (userIdClaim == null || !StringUtils.hasText(roleClaim)) {
            throw new JwtException("Missing required userId or role claim in JWT");
        }

        return new OrderUserPrincipal(userIdClaim.longValue(), email, roleClaim);
    }
}