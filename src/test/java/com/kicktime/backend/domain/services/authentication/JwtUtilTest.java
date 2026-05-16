package com.kicktime.backend.domain.services.authentication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
    }

    @Test
    void generateToken_returnsNonNullToken() {
        String token = jwtUtil.generateToken("user@kicktime.com", "PLAYER", 1L);
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void extractEmail_returnsCorrectEmail() {
        String token = jwtUtil.generateToken("user@kicktime.com", "PLAYER", 1L);
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("user@kicktime.com");
    }

    @Test
    void extractRole_returnsCorrectRole() {
        String token = jwtUtil.generateToken("user@kicktime.com", "CAPTAIN", 2L);
        assertThat(jwtUtil.extractRole(token)).isEqualTo("CAPTAIN");
    }

    @Test
    void extractUserId_returnsCorrectUserId() {
        String token = jwtUtil.generateToken("user@kicktime.com", "PLAYER", 42L);
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(42L);
    }

    @Test
    void isTokenValid_withValidToken_returnsTrue() {
        String token = jwtUtil.generateToken("user@kicktime.com", "PLAYER", 1L);
        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_withInvalidToken_returnsFalse() {
        assertThat(jwtUtil.isTokenValid("token.invalido.total")).isFalse();
    }

    @Test
    void isTokenValid_withTamperedToken_returnsFalse() {
        String token = jwtUtil.generateToken("user@kicktime.com", "PLAYER", 1L);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtUtil.isTokenValid(tampered)).isFalse();
    }

    @Test
    void isTokenValid_withEmptyString_returnsFalse() {
        assertThat(jwtUtil.isTokenValid("")).isFalse();
    }

    @Test
    void generateToken_differentUsersProduceDifferentTokens() {
        String token1 = jwtUtil.generateToken("user1@kicktime.com", "PLAYER", 1L);
        String token2 = jwtUtil.generateToken("user2@kicktime.com", "PLAYER", 2L);
        assertThat(token1).isNotEqualTo(token2);
    }
}