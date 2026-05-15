package com.kicktime.backend.api;

import com.kicktime.backend.domain.model.dto.request.LoginRequestDTO;
import com.kicktime.backend.domain.model.dto.request.RegisterRequestDTO;
import com.kicktime.backend.domain.model.dto.response.LoginResponseDTO;
import com.kicktime.backend.domain.model.dto.response.RegisterResponseDTO;
import com.kicktime.backend.domain.model.enums.UserRole;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("AuthController - Pruebas de Integración")
class AuthControllerTest {

    @Autowired
    private AuthController authController;

    @Nested
    @DisplayName("register")
    class RegisterTests {

        @Test
        @DisplayName("Debe retornar 200 OK con datos del usuario registrado")
        void register_ValidRequest_Returns200() {
            RegisterRequestDTO request = new RegisterRequestDTO();
            request.setName("Usuario Test");
            request.setEmail("test_register_" + System.currentTimeMillis() + "@kicktime.com");
            request.setPassword("password123");

            ResponseEntity<RegisterResponseDTO> response = authController.register(request);

            assertNotNull(response);
            assertNotNull(response.getBody());
            assertEquals(200, response.getStatusCode().value());
            assertEquals("Usuario Test", response.getBody().getName());
            assertEquals(UserRole.PLAYER, response.getBody().getRole());
        }

        @Test
        @DisplayName("Debe asignar rol PLAYER automáticamente al registrar")
        void register_ShouldAssignPlayerRoleAutomatically() {
            RegisterRequestDTO request = new RegisterRequestDTO();
            request.setName("Jugador Nuevo");
            request.setEmail("player_role_" + System.currentTimeMillis() + "@kicktime.com");
            request.setPassword("password123");

            ResponseEntity<RegisterResponseDTO> response = authController.register(request);

            assertNotNull(response.getBody());
            assertEquals(UserRole.PLAYER, response.getBody().getRole());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el email ya está registrado")
        void register_EmailAlreadyExists_ThrowsException() {
            String email = "duplicate_" + System.currentTimeMillis() + "@kicktime.com";

            RegisterRequestDTO first = new RegisterRequestDTO();
            first.setName("Usuario Uno");
            first.setEmail(email);
            first.setPassword("password123");
            authController.register(first);

            RegisterRequestDTO second = new RegisterRequestDTO();
            second.setName("Usuario Dos");
            second.setEmail(email);
            second.setPassword("otrapassword");

            assertThrows(RuntimeException.class,
                    () -> authController.register(second));
        }

        @Test
        @DisplayName("Debe retornar email correcto en la respuesta")
        void register_ShouldReturnCorrectEmail() {
            String email = "email_check_" + System.currentTimeMillis() + "@kicktime.com";

            RegisterRequestDTO request = new RegisterRequestDTO();
            request.setName("Check Email");
            request.setEmail(email);
            request.setPassword("password123");

            ResponseEntity<RegisterResponseDTO> response = authController.register(request);

            assertNotNull(response.getBody());
            assertEquals(email, response.getBody().getEmail());
        }
    }

    @Nested
    @DisplayName("login")
    class LoginTests {

        @Test
        @DisplayName("Debe retornar 200 OK con token JWT")
        void login_ValidCredentials_Returns200WithToken() {
            String email = "login_test_" + System.currentTimeMillis() + "@kicktime.com";

            RegisterRequestDTO register = new RegisterRequestDTO();
            register.setName("Login User");
            register.setEmail(email);
            register.setPassword("password123");
            authController.register(register);

            LoginRequestDTO login = new LoginRequestDTO();
            login.setEmail(email);
            login.setPassword("password123");

            ResponseEntity<LoginResponseDTO> response = authController.login(login);

            assertNotNull(response);
            assertNotNull(response.getBody());
            assertEquals(200, response.getStatusCode().value());
            assertNotNull(response.getBody().getToken());
            assertFalse(response.getBody().getToken().isEmpty());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el usuario no existe")
        void login_UserNotFound_ThrowsException() {
            LoginRequestDTO request = new LoginRequestDTO();
            request.setEmail("noexiste_" + System.currentTimeMillis() + "@kicktime.com");
            request.setPassword("password123");

            assertThrows(RuntimeException.class,
                    () -> authController.login(request));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando la contraseña es incorrecta")
        void login_InvalidPassword_ThrowsException() {
            String email = "wrong_pass_" + System.currentTimeMillis() + "@kicktime.com";

            RegisterRequestDTO register = new RegisterRequestDTO();
            register.setName("Wrong Pass User");
            register.setEmail(email);
            register.setPassword("correctpassword");
            authController.register(register);

            LoginRequestDTO login = new LoginRequestDTO();
            login.setEmail(email);
            login.setPassword("wrongpassword");

            assertThrows(RuntimeException.class,
                    () -> authController.login(login));
        }

        @Test
        @DisplayName("Debe retornar tokens diferentes para usuarios diferentes")
        void login_DifferentUsers_ReturnDifferentTokens() {
            long timestamp = System.currentTimeMillis();

            RegisterRequestDTO r1 = new RegisterRequestDTO();
            r1.setName("User One");
            r1.setEmail("user1_" + timestamp + "@kicktime.com");
            r1.setPassword("password123");
            authController.register(r1);

            RegisterRequestDTO r2 = new RegisterRequestDTO();
            r2.setName("User Two");
            r2.setEmail("user2_" + timestamp + "@kicktime.com");
            r2.setPassword("password123");
            authController.register(r2);

            LoginRequestDTO l1 = new LoginRequestDTO();
            l1.setEmail(r1.getEmail());
            l1.setPassword("password123");

            LoginRequestDTO l2 = new LoginRequestDTO();
            l2.setEmail(r2.getEmail());
            l2.setPassword("password123");

            ResponseEntity<LoginResponseDTO> resp1 = authController.login(l1);
            ResponseEntity<LoginResponseDTO> resp2 = authController.login(l2);

            assertNotNull(resp1.getBody());
            assertNotNull(resp2.getBody());
            assertNotEquals(resp1.getBody().getToken(), resp2.getBody().getToken());
        }
    }
}