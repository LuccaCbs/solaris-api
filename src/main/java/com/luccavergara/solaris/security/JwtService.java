package com.luccavergara.solaris.security;

import com.luccavergara.solaris.entity.OrganizationMemberRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Service
public class JwtService {

    public static final String CLAIM_ORG_ID = "orgId";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_STORE_ID = "storeId";
    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_PURPOSE = "purpose";
    public static final String PURPOSE_ORG_BILLING = "ORG_BILLING";

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Optional<Long> extractOrganizationId(String token) {
        return extractLongClaim(token, CLAIM_ORG_ID);
    }

    public Optional<OrganizationMemberRole> extractOrganizationRole(String token) {
        return Optional.ofNullable(extractClaim(token, claims -> claims.get(CLAIM_ROLE, String.class)))
                .map(OrganizationMemberRole::valueOf);
    }

    public Optional<Long> extractStoreId(String token) {
        return extractLongClaim(token, CLAIM_STORE_ID);
    }

    private Optional<Long> extractLongClaim(String token, String claimName) {
        return Optional.ofNullable(extractClaim(token, claims -> {
            Object value = claims.get(claimName);
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value instanceof String string && !string.isBlank()) {
                return Long.parseLong(string.trim());
            }
            return null;
        }));
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails
    ) {
        return generateToken(extraClaims, userDetails, jwtExpiration);
    }

    public String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expirationMs
    ) {
        return Jwts
                .builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSignInKey())
                .compact();
    }

    public boolean isTokenValid(
            String token,
            UserDetails userDetails
    ) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(
            String token,
            Function<Claims, T> claimsResolver
    ) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims parseClaims(String token) {
        return extractAllClaims(token);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
