package com.kicktime.backend.domain.services;

import com.kicktime.backend.domain.model.User;
import com.kicktime.backend.domain.model.dto.request.LoginRequestDTO;
import com.kicktime.backend.domain.model.dto.request.RegisterRequestDTO;
import com.kicktime.backend.domain.model.dto.response.RegisterResponseDTO;
import com.kicktime.backend.domain.model.enums.UserRole;
import com.kicktime.backend.domain.services.authentication.AuthService;
import com.kicktime.backend.domain.services.authentication.JwtUtil;
import com.kicktime.backend.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private RegisterRequestDTO registerRequest;
    private LoginRequestDTO loginRequest;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequestDTO();
        registerRequest.setName("Julian Torres");
        registerRequest.setEmail("julian@kicktime.com");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("julian@kicktime.com");
        loginRequest.setPassword("password123");

        user = User.builder()
                .id(1L)
                .name("Julian Torres")
                .email("julian@kicktime.com")
                .password("$2a$encoded_password")
                .role(UserRole.PLAYER)
                .build();
    }

    // ─── register ─────────────────────────────────────────────────────────────

    @Test
    void register_success_returnsCorrectResponse() {
        when(userRepository.existsByEmail("julian@kicktime.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$encoded_password");

        RegisterResponseDTO response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Julian Torres");
        assertThat(response.getEmail()).isEqualTo("julian@kicktime.com");
        assertThat(response.getRole()).isEqualTo(UserRole.PLAYER);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_emailAlreadyRegistered_throwsException() {
        when(userRepository.existsByEmail("julian@kicktime.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_passwordIsEncoded_beforeSaving() {
        when(userRepository.existsByEmail("julian@kicktime.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$encoded_password");

        authService.register(registerRequest);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        assertThat(captor.getValue().getPassword()).isEqualTo("$2a$encoded_password");
        assertThat(captor.getValue().getPassword()).isNotEqualTo("password123");
    }

    @Test
    void register_userSavedWithPlayerRole() {
        when(userRepository.existsByEmail("julian@kicktime.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$encoded_password");

        authService.register(registerRequest);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.PLAYER);
    }

    @Test
    void register_doesNotGenerateToken() {
        when(userRepository.existsByEmail("julian@kicktime.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$encoded_password");

        authService.register(registerRequest);

        verify(jwtUtil, never()).generateToken(any(), any(), any());
    }

    // ─── login ────────────────────────────────────────────────────────────────

    @Test
    void login_success_returnsToken() {
        when(userRepository.findByEmail("julian@kicktime.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "$2a$encoded_password")).thenReturn(true);
        when(jwtUtil.generateToken("julian@kicktime.com", "PLAYER", 1L))
                .thenReturn("mocked.jwt.token");

        String token = authService.login(loginRequest);

        assertThat(token).isEqualTo("mocked.jwt.token");
    }

    @Test
    void login_userNotFound_throwsException() {
        when(userRepository.findByEmail("julian@kicktime.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");

        verify(jwtUtil, never()).generateToken(any(), any(), any());
    }

    @Test
    void login_incorrectPassword_throwsException() {
        when(userRepository.findByEmail("julian@kicktime.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "$2a$encoded_password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid credentials");

        verify(jwtUtil, never()).generateToken(any(), any(), any());
    }

    @Test
    void login_tokenGeneratedWithCorrectParameters() {
        when(userRepository.findByEmail("julian@kicktime.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "$2a$encoded_password")).thenReturn(true);
        when(jwtUtil.generateToken("julian@kicktime.com", "PLAYER", 1L))
                .thenReturn("mocked.jwt.token");

        authService.login(loginRequest);

        verify(jwtUtil).generateToken("julian@kicktime.com", "PLAYER", 1L);
    }
}