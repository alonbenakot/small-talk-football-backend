package com.smalltalk.SmallTalkFootball.config;

import com.smalltalk.SmallTalkFootball.domain.User;
import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtUtil {

    private final String signatureAlgorithm = SignatureAlgorithm.HS256.getJcaName();
    private final String encodedSecretKey = "this+is+my+key+and+it+must+be+at+least+256+bits+long";
    private final Key decodedSecretKey = new SecretKeySpec(Base64.getDecoder().decode(encodedSecretKey), this.signatureAlgorithm);

    @Value("${jwt.expiration:3600}")
    private long jwtExpirationInSec;

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        return createToken(claims, user.getEmail());
    }

    private String createToken(Map<String, Object> claims, String subject) {

        Instant now = Instant.now();

        return Jwts.builder().setClaims(claims)

                .setSubject(subject)

                .setIssuedAt(Date.from(now))

                .setExpiration(Date.from(now.plus(jwtExpirationInSec, ChronoUnit.SECONDS)))

                .signWith(decodedSecretKey)

                .compact();
    }

    private Claims extractAllClaims(String token) throws ExpiredJwtException {
        JwtParser jwtParser = Jwts.parserBuilder().setSigningKey(decodedSecretKey).build();
        return jwtParser.parseClaimsJws(token).getBody();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * returns the JWT subject - in our case the email address
     */
    public String extractUserEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public boolean isTokenValidated(String token, User user) {
        if (user != null) {
            String userName = extractUserEmail(token);
            String userId = extractUserId(token);
            return userName.equals(user.getEmail()) && userId.equals(user.getId()) && !isTokenExpired(token);
        }
        return false;
    }

}