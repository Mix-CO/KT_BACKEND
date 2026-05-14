package com.kicktime.backend.api;

import com.kicktime.backend.domain.model.dto.request.LoginRequestDTO;
import com.kicktime.backend.domain.model.dto.request.RegisterRequestDTO;
import com.kicktime.backend.domain.model.dto.response.RegisterResponseDTO;
import com.kicktime.backend.domain.model.enums.UserRole;
import com.kicktime.backend.domain.services.authentication.AuthService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("AuthController - Pruebas de Integración")
class AuthControllerTest {

    @Autowired
    private AuthService authService;

    // ================================================================
    // register
    // ================================================================

    @Nested
    @DisplayName("register")
    class RegisterTests {

        @Test
        @DisplayName("Debe registrar usuario exitosamente con datos válidos")
        void register_ValidRequest_ReturnsRegisterResponseDTO() {
            RegisterRequestDTO request = new RegisterRequestDTO();
            request.setName("Usuario Test");
            request.setEmail("test_register_" + System.currentTimeMillis() + "@kicktime.com");
            request.setPassword("password123");

            RegisterResponseDTO result = authService.register(request);

            assertNotNull(result);
            assertNotNull(result.getId());
            assertEquals("Usuario Test", result.getName());
            assertEquals(UserRole.PLAYER, result.getRole());
        }

        @Test
        @DisplayName("Debe asignar rol PLAYER automáticamente al registrar")
        void register_ShouldAssignPlayerRoleAutomatically() {
            RegisterRequestDTO request = new RegisterRequestDTO();
            request.setName("Jugador Nuevo");
            request.setEmail("player_role_" + System.currentTimeMillis() + "@kicktime.com");
            request.setPassword("password123");

            RegisterResponseDTO result = authService.register(request);

            assertEquals(UserRole.PLAYER, result.getRole());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el email ya está registrado")
        void register_EmailAlreadyExists_ThrowsException() {
            String email = "duplicate_" + System.currentTimeMillis() + "@kicktime.com";

            RegisterRequestDTO first = new RegisterRequestDTO();
            first.setName("Usuario Uno");
            first.setEmail(email);
            first.setPassword("password123");
            authService.register(first);

            RegisterRequestDTO second = new RegisterRequestDTO();
            second.setName("Usuario Dos");
            second.setEmail(email);
            second.setPassword("otrapassword");

            Exception exception = assertThrows(RuntimeException.class,
                    () -> authService.register(second));

            assertTrue(exception.getMessage().contains("Email already registered"));
        }

        @Test
        @DisplayName("Debe retornar email correcto en la respuesta")
        void register_ShouldReturnCorrectEmail() {
            String email = "email_check_" + System.currentTimeMillis() + "@kicktime.com";

            RegisterRequestDTO request = new RegisterRequestDTO();
            request.setName("Check Email");
            request.setEmail(email);
            request.setPassword("password123");

            RegisterResponseDTO result = authService.register(request);

            assertEquals(email, result.getEmail());
        }
    }

    // ================================================================
    // login
    // ================================================================

    @Nested
    @DisplayName("login")
    class LoginTests {

        @Test
        @DisplayName("Debe retornar token JWT al hacer login exitosamente")
        void login_ValidCredentials_ReturnsToken() {
            String email = "login_test_" + System.currentTimeMillis() + "@kicktime.com";

            RegisterRequestDTO register = new RegisterRequestDTO();
            register.setName("Login User");
            register.setEmail(email);
            register.setPassword("password123");
            authService.register(register);

            LoginRequestDTO login = new LoginRequestDTO();
            login.setEmail(email);
            login.setPassword("password123");

            String token = authService.login(login);

            assertNotNull(token);
            assertFalse(token.isEmpty());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el usuario no existe")
        void login_UserNotFound_ThrowsException() {
            LoginRequestDTO request = new LoginRequestDTO();
            request.setEmail("noexiste_" + System.currentTimeMillis() + "@kicktime.com");
            request.setPassword("password123");

            Exception exception = assertThrows(RuntimeException.class,
                    () -> authService.login(request));

            assertTrue(exception.getMessage().contains("User not found"));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando la contraseña es incorrecta")
        void login_InvalidPassword_ThrowsException() {
            String email = "wrong_pass_" + System.currentTimeMillis() + "@kicktime.com";

            RegisterRequestDTO register = new RegisterRequestDTO();
            register.setName("Wrong Pass User");
            register.setEmail(email);
            register.setPassword("correctpassword");
            authService.register(register);

            LoginRequestDTO login = new LoginRequestDTO();
            login.setEmail(email);
            login.setPassword("wrongpassword");

            Exception exception = assertThrows(RuntimeException.class,
                    () -> authService.login(login));

            assertTrue(exception.getMessage().contains("Invalid credentials"));
        }

        @Test
        @DisplayName("Debe retornar tokens diferentes para usuarios diferentes")
        void login_DifferentUsers_ReturnDifferentTokens() {
            long timestamp = System.currentTimeMillis();

            RegisterRequestDTO r1 = new RegisterRequestDTO();
            r1.setName("User One");
            r1.setEmail("user1_" + timestamp + "@kicktime.com");
            r1.setPassword("password123");
            authService.register(r1);

            RegisterRequestDTO r2 = new RegisterRequestDTO();
            r2.setName("User Two");
            r2.setEmail("user2_" + timestamp + "@kicktime.com");
            r2.setPassword("password123");
            authService.register(r2);

            LoginRequestDTO l1 = new LoginRequestDTO();
            l1.setEmail(r1.getEmail());
            l1.setPassword("password123");

            LoginRequestDTO l2 = new LoginRequestDTO();
            l2.setEmail(r2.getEmail());
            l2.setPassword("password123");

            String token1 = authService.login(l1);
            String token2 = authService.login(l2);

            assertNotEquals(token1, token2);
        }
    }
}