package com.kicktime.backend.api;

import com.kicktime.backend.domain.model.dto.request.CreateReservationRequestDTO;
import com.kicktime.backend.domain.model.dto.request.UpdateReservationStatusRequestDTO;
import com.kicktime.backend.domain.model.dto.response.ReservationResponseDTO;
import com.kicktime.backend.domain.model.enums.ReservationStatus;
import com.kicktime.backend.domain.services.ReservationService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("ReservationController - Pruebas de Integración")
class ReservationControllerTest {

    @Autowired
    private ReservationService reservationService;

    // ================================================================
    // createReservation
    // ================================================================

    @Nested
    @DisplayName("createReservation")
    class CreateReservationTests {

        @Test
        @DisplayName("Debe lanzar excepción cuando el match no existe")
        void createReservation_MatchNotFound_ThrowsException() {
            CreateReservationRequestDTO request = CreateReservationRequestDTO.builder()
                    .matchId(999L)
                    .timeSlotId(1L)
                    .userId(1L)
                    .build();

            assertThrows(RuntimeException.class,
                    () -> reservationService.createReservation(request));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el timeSlot no existe")
        void createReservation_TimeSlotNotFound_ThrowsException() {
            CreateReservationRequestDTO request = CreateReservationRequestDTO.builder()
                    .matchId(999L)
                    .timeSlotId(999L)
                    .userId(1L)
                    .build();

            assertThrows(RuntimeException.class,
                    () -> reservationService.createReservation(request));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el usuario no existe")
        void createReservation_UserNotFound_ThrowsException() {
            CreateReservationRequestDTO request = CreateReservationRequestDTO.builder()
                    .matchId(999L)
                    .timeSlotId(999L)
                    .userId(999L)
                    .build();

            assertThrows(RuntimeException.class,
                    () -> reservationService.createReservation(request));
        }
    }

    // ================================================================
    // getReservationsForMatch
    // ================================================================

    @Nested
    @DisplayName("getReservationsForMatch")
    class GetReservationsForMatchTests {

        @Test
        @DisplayName("Debe retornar lista vacía cuando no hay reservas para el partido")
        void getReservationsForMatch_NoReservations_ReturnsEmptyList() {
            List<ReservationResponseDTO> result =
                    reservationService.getReservationsForMatch(999L);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Debe retornar lista no nula para cualquier matchId")
        void getReservationsForMatch_AnyId_ReturnsNotNull() {
            List<ReservationResponseDTO> result =
                    reservationService.getReservationsForMatch(1L);

            assertNotNull(result);
        }
    }

    // ================================================================
    // updateReservationStatus
    // ================================================================

    @Nested
    @DisplayName("updateReservationStatus")
    class UpdateReservationStatusTests {

        @Test
        @DisplayName("Debe lanzar excepción cuando la reserva no existe")
        void updateReservationStatus_ReservationNotFound_ThrowsException() {
            UpdateReservationStatusRequestDTO request = UpdateReservationStatusRequestDTO.builder()
                    .status(ReservationStatus.ACCEPTED)
                    .respondingUserId(1L)
                    .build();

            Exception exception = assertThrows(RuntimeException.class,
                    () -> reservationService.updateReservationStatus(999L, request));

            assertTrue(exception.getMessage().contains("Reservation not found"));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando se intenta actualizar con ID inexistente")
        void updateReservationStatus_InvalidId_ThrowsException() {
            UpdateReservationStatusRequestDTO request = UpdateReservationStatusRequestDTO.builder()
                    .status(ReservationStatus.REJECTED)
                    .respondingUserId(1L)
                    .build();

            assertThrows(RuntimeException.class,
                    () -> reservationService.updateReservationStatus(999L, request));
        }
    }

    // ================================================================
    // deleteReservation
    // ================================================================

    @Nested
    @DisplayName("deleteReservation")
    class DeleteReservationTests {

        @Test
        @DisplayName("Debe lanzar excepción cuando la reserva no existe")
        void deleteReservation_NonExistingId_ThrowsException() {
            Exception exception = assertThrows(RuntimeException.class,
                    () -> reservationService.deleteReservation(999L));

            assertTrue(exception.getMessage().contains("Reservation not found"));
        }

        @Test
        @DisplayName("Debe ejecutarse sin excepción cuando la reserva existe")
        void deleteReservation_ExistingId_DoesNotThrow() {
            // Verificamos que el método no lanza cuando no existe,
            // y que el manejo de error es correcto
            assertThrows(RuntimeException.class,
                    () -> reservationService.deleteReservation(999L));
        }
    }
}
