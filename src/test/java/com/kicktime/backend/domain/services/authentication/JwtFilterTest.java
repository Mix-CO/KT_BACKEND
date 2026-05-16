package com.kicktime.backend.domain.services.authentication;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_noAuthHeader_continuesFilterChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).isTokenValid(any());
    }

    @Test
    void doFilterInternal_authHeaderWithoutBearer_continuesFilterChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic sometoken");

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).isTokenValid(any());
    }

    @Test
    void doFilterInternal_invalidToken_returns401() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid.token");
        when(request.getHeader("Origin")).thenReturn("http://localhost:5173");
        when(jwtUtil.isTokenValid("invalid.token")).thenReturn(false);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_invalidToken_noOriginHeader_stillReturns401() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid.token");
        when(request.getHeader("Origin")).thenReturn(null);
        when(jwtUtil.isTokenValid("invalid.token")).thenReturn(false);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response, never()).setHeader(eq("Access-Control-Allow-Origin"), any());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_validToken_setsAuthenticationAndContinues() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.token");
        when(jwtUtil.isTokenValid("valid.token")).thenReturn(true);
        when(jwtUtil.extractEmail("valid.token")).thenReturn("user@kicktime.com");
        when(jwtUtil.extractRole("valid.token")).thenReturn("PLAYER");

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("user@kicktime.com");
    }

    @Test
    void doFilterInternal_validToken_setsCorrectRole() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.token");
        when(jwtUtil.isTokenValid("valid.token")).thenReturn(true);
        when(jwtUtil.extractEmail("valid.token")).thenReturn("captain@kicktime.com");
        when(jwtUtil.extractRole("valid.token")).thenReturn("CAPTAIN");

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_CAPTAIN"));
    }
}
