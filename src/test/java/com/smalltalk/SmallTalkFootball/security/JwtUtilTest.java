package com.smalltalk.SmallTalkFootball.security;

import com.smalltalk.SmallTalkFootball.domain.User;
import com.smalltalk.SmallTalkFootball.testsupport.TestFixtures;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        expireAfterHours(720);
    }

    private void expireAfterHours(long hours) {
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationInHours", hours);
    }

    @Test
    void roundTripsTheUserEmailAndId() {
        User user = TestFixtures.member("ada@example.com");

        String token = jwtUtil.generateToken(user);

        assertThat(jwtUtil.extractUserEmail(token)).isEqualTo("ada@example.com");
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(user.getId());
    }

    @Test
    void setsAnExpiryTheConfiguredNumberOfHoursAhead() {
        expireAfterHours(2);

        Date expiration = jwtUtil.extractExpiration(jwtUtil.generateToken(TestFixtures.member("ada@example.com")));

        long hoursAhead = (expiration.getTime() - System.currentTimeMillis()) / 3_600_000;
        assertThat(hoursAhead).isBetween(1L, 2L);
    }

    @Test
    void validatesATokenAgainstItsOwnUser() {
        User user = TestFixtures.member("ada@example.com");

        assertThat(jwtUtil.isTokenValidated(jwtUtil.generateToken(user), user)).isTrue();
    }

    @Test
    void rejectsANullUser() {
        String token = jwtUtil.generateToken(TestFixtures.member("ada@example.com"));

        assertThat(jwtUtil.isTokenValidated(token, null)).isFalse();
    }

    @Test
    void rejectsATokenIssuedForADifferentEmail() {
        String token = jwtUtil.generateToken(TestFixtures.member("ada@example.com"));

        assertThat(jwtUtil.isTokenValidated(token, TestFixtures.member("grace@example.com"))).isFalse();
    }

    @Test
    void rejectsATokenWhoseUserIdNoLongerMatches() {
        User user = TestFixtures.member("ada@example.com");
        String token = jwtUtil.generateToken(user);

        User sameEmailDifferentId = TestFixtures.member("ada@example.com");
        sameEmailDifferentId.setId("some-other-id");

        assertThat(jwtUtil.isTokenValidated(token, sameEmailDifferentId)).isFalse();
    }

    /*
     * jjwt raises ExpiredJwtException while parsing, so validation throws rather than
     * returning false. JwtAuthFilter catches everything and answers 401, so the caller-facing
     * behaviour is the same - but any new caller has to catch it.
     */
    @Test
    void throwsRatherThanReturningFalseForAnExpiredToken() {
        expireAfterHours(-1);
        User user = TestFixtures.member("ada@example.com");
        String expired = jwtUtil.generateToken(user);

        assertThatThrownBy(() -> jwtUtil.isTokenValidated(expired, user))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void rejectsATokenSignedWithAnotherKey() {
        // Signed by a different issuer: same shape, wrong signature.
        String foreign = "eyJhbGciOiJIUzI1NiJ9"
                + ".eyJzdWIiOiJhZGFAZXhhbXBsZS5jb20iLCJ1c2VySWQiOiJ1c2VyLTEifQ"
                + ".2SkQ3lQ0Q0dQ0Q0dQ0Q0dQ0Q0dQ0Q0dQ0Q0dQ0Q0dQ";

        assertThatThrownBy(() -> jwtUtil.extractUserEmail(foreign))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void rejectsATokenThatIsNotAJwtAtAll() {
        assertThatThrownBy(() -> jwtUtil.extractUserEmail("clearly-not-a-token"))
                .isInstanceOf(MalformedJwtException.class);
    }
}
