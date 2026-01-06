package com.ocmseservice.apiservice.workflows.spcp.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility class for JWT token operations
 */
@Component
@Slf4j
public class JwtTokenUtil {

    private static final String TOKEN_ISSUER = "OCMSE";

    @Value("${jwt.secret.key:defaultSecretKeyForDevelopmentPurposesOnly}")
    private String jwtSecret;

    @Value("${jwt.token.expiration:3600}")
    private int expiration;

    @Value("${jwt.refresh.token.expiration:86400}")
    private int refreshExpiration;

    /**
     * Extract expiration date from token
     * 
     * @param token JWT token
     * @return expiration date
     */
    public Date extractExpiration(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * Check if token is expired
     * 
     * @param token JWT token
     * @return true if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException ex) {
            log.warn("Token expired: {}", ex.getMessage());
            return true;
        }
    }

    /**
     * Get claim from token
     * 
     * @param token JWT token
     * @param claimsResolver function to resolve claims
     * @return claim value
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Get all claims from token
     * 
     * @param token JWT token
     * @return all claims
     */
    public Claims getAllClaimsFromToken(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Generate JWT token
     * 
     * @param claims claims to include in token
     * @param subject subject of token (usually username)
     * @return JWT token
     */
    public String generateToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setIssuer(TOKEN_ISSUER)
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generate refresh token
     * 
     * @param subject subject of token (usually username)
     * @return refresh token
     */
    public String generateRefreshToken(String subject) {
        return Jwts.builder()
                .setIssuer(TOKEN_ISSUER)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpiration * 1000))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Get claims from expired token
     * 
     * @param token expired JWT token
     * @return claims from expired token
     */
    public Claims getClaimsFromExpiredToken(String token) {
        try {
            return Jwts
                    .parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException ex) {
            return ex.getClaims();
        }
    }

    /**
     * Validate token
     * 
     * @param token JWT token
     * @return true if token is valid
     */
    public boolean validateToken(String token) {
        try {
            Jwts
                    .parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
}
