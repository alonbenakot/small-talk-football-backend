package com.smalltalk.SmallTalkFootball.system.utils;

import com.smalltalk.SmallTalkFootball.entities.User;
import io.jsonwebtoken.*;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil {

    private static String signatureAlgorithm = SignatureAlgorithm.HS256.getJcaName();
    private static String encodedSecretKey = "this+is+the+keffefey+and+it+must+be+at+least+256+bits+long";
    private static Key decodedSecretKey = new SecretKeySpec(Base64.getDecoder().decode(encodedSecretKey),
            JwtUtil.signatureAlgorithm);

    public static String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userPassword", user.getPassword());
        claims.put("userId", user.getId());
        return createToken(claims, user.getEmail());
    }

    private static String createToken(Map<String, Object> claims, String subject) {

        Instant now = Instant.now();

        return Jwts.builder().setClaims(claims)

                .setSubject(subject)

                .setIssuedAt(Date.from(now))

                .setExpiration(Date.from(now.plus(5, ChronoUnit.HOURS)))

                .signWith(JwtUtil.decodedSecretKey)

                .compact();
    }

    private Claims extractAllClaims(String token) throws ExpiredJwtException {
        JwtParser jwtParser = Jwts.parserBuilder().setSigningKey(this.decodedSecretKey).build();
        return jwtParser.parseClaimsJws(token).getBody();
    }

    /**
     * returns the JWT subject - in our case the email address
     */
    public String extractUserEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    private boolean isTokenExpired(String token) {
        try {
            extractAllClaims(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    public boolean validateToken(String token, String email) {
        final String username = extractUserEmail(token);
        return (username.equals(email) && !isTokenExpired(token));
    }

}