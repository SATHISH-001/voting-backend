package com.voting.security;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {
    @Value("${jwt.secret}") private String secret;
    @Value("${jwt.expiration}") private long expiration;

    private SecretKey key() { return Keys.hmacShaKeyFor(secret.getBytes()); }

    public String generateToken(String email, String role, Long userId) {
        return Jwts.builder().subject(email)
            .claim("role", role).claim("userId", userId)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(key()).compact();
    }

    public Claims claims(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    }

    public String extractEmail(String token)  { return claims(token).getSubject(); }
    public Long   extractUserId(String token) { return claims(token).get("userId", Long.class); }

    public boolean isValid(String token) {
        try { return !claims(token).getExpiration().before(new Date()); }
        catch (JwtException e) { return false; }
    }
}
