package com.kicktime.backend.api;

import com.kicktime.backend.domain.model.dto.request.CreateAvailabilityRequestDTO;
import com.kicktime.backend.domain.model.dto.response.AvailabilityResponseDTO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("AvailabilityController - Pruebas de Integración")
class AvailabilityControllerTest {

    @Autowired
    private AvailabilityController availabilityController;

    @Nested
    @DisplayName("createAvailability")
    class CreateAvailabilityTests {

        @Autowired
        private AvailabilityController availabilityController;

        @Autowired
        private com.kicktime.backend.repository.UserRepository userRepository;

        @Autowired
        private com.kicktime.backend.repository.TimeSlotRepository timeSlotRepository;

        @Autowired
        private com.kicktime.backend.repository.AvailabilityRepository availabilityRepository;

        @Test
        @DisplayName("Debe retornar 201 CREATED cuando los datos son válidos")
        void createAvailability_ValidRequest_Returns201() {
            com.kicktime.backend.domain.model.User user =
                    com.kicktime.backend.domain.model.User.builder()
                            .name("Usuario Availability Test")
                            .email("availability_" + System.currentTimeMillis() + "@kicktime.com")
                            .password("encoded")
                            .role(com.kicktime.backend.domain.model.enums.UserRole.PLAYER)
                            .build();
            com.kicktime.backend.domain.model.User savedUser = userRepository.save(user);

            com.kicktime.backend.domain.model.TimeSlot timeSlot =
                    new com.kicktime.backend.domain.model.TimeSlot();
            timeSlot.setStatus(com.kicktime.backend.domain.model.enums.TimeSlotStatus.AVAILABLE);
            com.kicktime.backend.domain.model.TimeSlot savedSlot = timeSlotRepository.save(timeSlot);

            Long availabilityId = null;
            try {
                CreateAvailabilityRequestDTO request = CreateAvailabilityRequestDTO.builder()
                        .userId(savedUser.getId())
                        .timeSlotId(savedSlot.getId())
                        .build();

                ResponseEntity<AvailabilityResponseDTO> response =
                        availabilityController.createAvailability(request);

                assertNotNull(response);
                assertEquals(201, response.getStatusCode().value());
                assertNotNull(response.getBody());
                availabilityId = response.getBody().getId();
            } finally {
                // Borrar en orden correcto: primero availability, luego user y timeslot
                if (availabilityId != null) {
                    availabilityRepository.deleteById(availabilityId);
                }
                userRepository.deleteById(savedUser.getId());
                timeSlotRepository.deleteById(savedSlot.getId());
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el usuario no existe")
        void createAvailability_UserNotFound_ThrowsException() {
            CreateAvailabilityRequestDTO request = CreateAvailabilityRequestDTO.builder()
                    .userId(999L)
                    .timeSlotId(1L)
                    .build();

            assertThrows(RuntimeException.class,
                    () -> availabilityController.createAvailability(request));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el timeSlot no existe")
        void createAvailability_TimeSlotNotFound_ThrowsException() {
            CreateAvailabilityRequestDTO request = CreateAvailabilityRequestDTO.builder()
                    .userId(999L)
                    .timeSlotId(999L)
                    .build();

            assertThrows(RuntimeException.class,
                    () -> availabilityController.createAvailability(request));
        }
    }

    @Nested
    @DisplayName("getUserAvailability")
    class GetUserAvailabilityTests {

        @Test
        @DisplayName("Debe retornar 200 OK con lista vacía cuando no hay disponibilidad")
        void getUserAvailability_NoAvailability_Returns200() {
            ResponseEntity<List<AvailabilityResponseDTO>> response =
                    availabilityController.getUserAvailability(999L);

            assertNotNull(response);
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isEmpty());
        }

        @Test
        @DisplayName("Debe retornar 200 OK con lista no nula")
        void getUserAvailability_AnyUserId_Returns200() {
            ResponseEntity<List<AvailabilityResponseDTO>> response =
                    availabilityController.getUserAvailability(1L);

            assertNotNull(response);
            assertNotNull(response.getBody());
            assertEquals(200, response.getStatusCode().value());
        }
    }

    @Nested
    @DisplayName("deleteAvailability")
    class DeleteAvailabilityTests {

        @Test
        @DisplayName("Debe retornar 204 NO CONTENT cuando el id no existe")
        void deleteAvailability_NonExistingId_Returns204() {
            ResponseEntity<Void> response =
                    availabilityController.deleteAvailability(999L);

            assertNotNull(response);
            assertEquals(204, response.getStatusCode().value());
        }

        @Test
        @DisplayName("Debe retornar 204 NO CONTENT con id cero")
        void deleteAvailability_ZeroId_Returns204() {
            ResponseEntity<Void> response =
                    availabilityController.deleteAvailability(0L);

            assertNotNull(response);
            assertEquals(204, response.getStatusCode().value());
        }
    }
}