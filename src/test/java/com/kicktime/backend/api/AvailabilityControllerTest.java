package com.kicktime.backend.api;

import com.kicktime.backend.domain.model.dto.request.CreateAvailabilityRequestDTO;
import com.kicktime.backend.domain.model.dto.response.AvailabilityResponseDTO;
import com.kicktime.backend.domain.services.AvailabilityService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("AvailabilityController - Pruebas de Integración")
class AvailabilityControllerTest {

    @Autowired
    private AvailabilityService availabilityService;

    // ================================================================
    // createAvailability
    // ================================================================

    @Nested
    @DisplayName("createAvailability")
    class CreateAvailabilityTests {

        @Test
        @DisplayName("Debe lanzar excepción cuando el usuario no existe")
        void createAvailability_UserNotFound_ThrowsException() {
            CreateAvailabilityRequestDTO request = CreateAvailabilityRequestDTO.builder()
                    .userId(999L)
                    .timeSlotId(1L)
                    .build();

            Exception exception = assertThrows(RuntimeException.class,
                    () -> availabilityService.createAvailability(request));

            assertTrue(exception.getMessage().contains("User not found"));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el timeSlot no existe")
        void createAvailability_TimeSlotNotFound_ThrowsException() {
            CreateAvailabilityRequestDTO request = CreateAvailabilityRequestDTO.builder()
                    .userId(999L)
                    .timeSlotId(999L)
                    .build();

            assertThrows(RuntimeException.class,
                    () -> availabilityService.createAvailability(request));
        }
    }

    // ================================================================
    // getUserAvailability
    // ================================================================

    @Nested
    @DisplayName("getUserAvailability")
    class GetUserAvailabilityTests {

        @Test
        @DisplayName("Debe retornar lista vacía cuando el usuario no tiene disponibilidad")
        void getUserAvailability_NoAvailability_ReturnsEmptyList() {
            List<AvailabilityResponseDTO> result =
                    availabilityService.getUserAvailability(999L);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Debe retornar lista no nula para cualquier userId")
        void getUserAvailability_AnyUserId_ReturnsNotNull() {
            List<AvailabilityResponseDTO> result =
                    availabilityService.getUserAvailability(1L);

            assertNotNull(result);
        }
    }

    // ================================================================
    // deleteAvailability
    // ================================================================

    @Nested
    @DisplayName("deleteAvailability")
    class DeleteAvailabilityTests {

        @Test
        @DisplayName("Debe ejecutarse sin excepción cuando el id no existe")
        void deleteAvailability_NonExistingId_DoesNotThrow() {
            assertDoesNotThrow(() -> availabilityService.deleteAvailability(999L));
        }

        @Test
        @DisplayName("Debe ejecutarse sin excepción con id cero")
        void deleteAvailability_ZeroId_DoesNotThrow() {
            assertDoesNotThrow(() -> availabilityService.deleteAvailability(0L));
        }
    }
}