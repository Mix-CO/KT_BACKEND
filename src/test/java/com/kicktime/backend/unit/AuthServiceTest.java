package com.kicktime.backend.unit;

import com.kicktime.backend.domain.model.User;
import com.kicktime.backend.domain.model.dto.request.LoginRequestDTO;
import com.kicktime.backend.domain.model.dto.request.RegisterRequestDTO;
import com.kicktime.backend.domain.model.enums.UserRole;
import com.kicktime.backend.domain.services.authentication.AuthService;
import com.kicktime.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private AuthService authService;

    @BeforeEach
    void setup() {
  //      userRepository = Mockito.mock(UserRepository.class);
  //      authService = new AuthService(userRepository);
    }

    @Test
    void shouldRegisterUserSuccessfully() {

        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setName("Julian");
        request.setEmail("julian@test.com");
        request.setPassword("123456");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);

        var response = authService.register(request);

        assertEquals("Julian", response.getName());
        assertEquals("julian@test.com", response.getEmail());
        assertEquals(UserRole.PLAYER, response.getRole());

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void shouldThrowExceptionIfEmailAlreadyExists() {

        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setName("Julian");
        request.setEmail("julian@test.com");
        request.setPassword("123456");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.register(request)
        );

        assertEquals("Email already registered", exception.getMessage());

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldLoginSuccessfully() {

        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("julian@test.com");
        request.setPassword("123456");

        String encodedPassword = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
                .encode("123456");

        User user = User.builder()
                .id(1L)
                .name("Julian")
                .email("julian@test.com")
                .password(encodedPassword)
                .role(UserRole.PLAYER)
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));

        String token = authService.login(request);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void shouldThrowExceptionIfUserNotFound() {

        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("notfound@test.com");
        request.setPassword("123456");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.login(request)
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionIfPasswordIsInvalid() {

        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("julian@test.com");
        request.setPassword("wrongpassword");

        String encodedPassword = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
                .encode("123456");

        User user = User.builder()
                .email("julian@test.com")
                .password(encodedPassword)
                .role(UserRole.PLAYER)
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.login(request)
        );

        assertEquals("Invalid credentials", exception.getMessage());
    }
}
