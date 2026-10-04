package tz.elmkusoma.config.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    /**
     * Issuer claim pinned on every token minted by this service.
     *
     * NOTE on `aud`: core/media/realtime form a single first-party family sharing
     * one HMAC secret (jwt.secret) with issuance centralised here. A per-service
     * audience would require per-service issuance/validation wiring; until that
     * exists an `aud` claim would be decoration, not enforcement. Future work:
     * introduce per-service audiences once each service mints/verifies its own
     * tokens.
     */
    public static final String ISSUER = "elmkusoma";

    /** Expected JWS header alg. Tokens with any other alg (incl. "none") are rejected. */
    public static final String EXPECTED_ALG = Jwts.SIG.HS256.getId();

    /**
     * Global (tenant-unscoped) Redis key prefix for the logout jti denylist.
     * Keys are `logout:denylist:jti:&lt;jti&gt;` with TTL = remaining access-token
     * lifetime. Deliberately NOT tenant-scoped: JwtAuthenticationFilter runs before
     * the organization context is resolved, so a tenant-prefixed write at logout
     * time would never match the filter's read. jti is globally unique.
     */
    public static final String LOGOUT_JTI_DENYLIST_PREFIX = "logout:denylist:jti:";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return generateToken(userDetails.getUsername(), accessTokenExpirationMs);
    }

    public String generateAccessToken(String email) {
        return generateToken(email, accessTokenExpirationMs);
    }

    public String generateAccessTokenWithClaims(String email, UUID userId, String role, UUID institutionId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

        var builder = Jwts.builder()
                .subject(email)
                .issuer(ISSUER)
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiryDate);

        if (userId != null) {
            builder.claim("userId", userId.toString());
        }
        if (role != null) {
            builder.claim("role", role);
        }
        if (institutionId != null) {
            builder.claim("institutionId", institutionId.toString());
        }

        // Algorithm pinned explicitly: with a 32-byte secret jjwt would infer
        // HS256 anyway, but pinning documents the contract and pairs with the
        // header-alg check on every parse path (alg-confusion defence).
        return builder.signWith(getSigningKey(), Jwts.SIG.HS256).compact();
    }

    public String generateRefreshToken(String email) {
        return generateToken(email, refreshTokenExpirationMs);
    }

    public String generateToken(String email, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)
                .issuer(ISSUER)
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String getEmailFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }

    public String getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("userId", String.class);
    }

    public String getRoleFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("role", String.class);
    }

    public String getInstitutionIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("institutionId", String.class);
    }

    /**
     * Lenient email extraction for the refresh flow ONLY. Accepts pre-hardening
     * 7d refresh tokens that carry no iss (alg is still pinned to HS256; a
     * present iss must match). All other callers use the strict getters above.
     */
    public String getEmailFromRefreshToken(String token) {
        return getLenientClaimsFromToken(token).getSubject();
    }

    /**
     * jti of any signature-valid token, iss-agnostic: pre-hardening tokens carry
     * no jti (null) and simply expire naturally. Used by the logout denylist
     * write and the filter's denylist read. Never throws.
     */
    public String getJtiFromToken(String token) {
        try {
            return getLenientClaimsFromToken(token).getId();
        } catch (JwtException | IllegalArgumentException | SecurityException ex) {
            log.debug("Could not extract jti from token: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Expiry of any signature-valid token, iss-agnostic. Used to size the logout
     * denylist TTL. Never throws; null when unreadable.
     */
    public Date getExpirationFromToken(String token) {
        try {
            return getLenientClaimsFromToken(token).getExpiration();
        } catch (JwtException | IllegalArgumentException | SecurityException ex) {
            log.debug("Could not extract expiration from token: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Strict access-token claims: signature + expiry + alg HS256 + iss required.
     * Backs validateToken and every get*FromToken path (filter, handshake,
     * org-context and request-attribute filters).
     */
    private Claims getClaimsFromToken(String token) {
        return parseAndVerify(token, true);
    }

    /**
     * Lenient refresh-token claims: signature + expiry + alg HS256, iss optional
     * but enforced when present.
     */
    private Claims getLenientClaimsFromToken(String token) {
        return parseAndVerify(token, false);
    }

    private Claims parseAndVerify(String token, boolean requireIssuer) {
        // parseSignedClaims already rejects unsigned/plaintext tokens (alg "none").
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
        requireExpectedAlg(jws);
        requireExpectedIssuer(jws.getPayload().getIssuer(), requireIssuer);
        return jws.getPayload();
    }

    private void requireExpectedAlg(Jws<Claims> jws) {
        String alg = jws.getHeader().getAlgorithm();
        if (!EXPECTED_ALG.equals(alg)) {
            throw new SecurityException("Unexpected JWT alg: " + alg);
        }
    }

    private void requireExpectedIssuer(String issuer, boolean requirePresent) {
        if (issuer == null) {
            if (requirePresent) {
                throw new SecurityException("Missing JWT issuer");
            }
            return;
        }
        if (!ISSUER.equals(issuer)) {
            throw new SecurityException("Unexpected JWT issuer: " + issuer);
        }
    }

    /**
     * Strict access-token validation: signature + expiry + alg HS256 + iss
     * REQUIRED. Wired to JwtAuthenticationFilter and every other access-token
     * check.
     *
     * COMPAT: access tokens live 1h, so strictness converges within an hour
     * without forced logout — a client holding a pre-hardening access token
     * refreshes once (lenient path below) and receives iss+jti tokens.
     */
    public boolean validateToken(String token) {
        try {
            getClaimsFromToken(token);
            return true;
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Lenient refresh-token validation: accepts pre-hardening 7d refresh tokens
     * that carry no iss (alg must still be HS256; a present iss must match).
     * Refresh rotation transparently upgrades tokens: each replacement pair
     * minted by this provider always carries iss+jti.
     */
    public boolean validateRefreshToken(String token) {
        try {
            getLenientClaimsFromToken(token);
            return true;
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }
}
