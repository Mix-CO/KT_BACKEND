package com.kicktime.backend.api;

import com.kicktime.backend.domain.model.dto.request.CreateReservationRequestDTO;
import com.kicktime.backend.domain.model.dto.request.RespondReservationDTO;
import com.kicktime.backend.domain.model.dto.request.UpdateReservationStatusRequestDTO;
import com.kicktime.backend.domain.model.enums.ReservationStatus;
import com.kicktime.backend.domain.services.ReservationService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("ReservationWebSocketController - Pruebas de Integración")
class ReservationWebSocketControllerTest {

    @Autowired
    private ReservationService reservationService;

    // ================================================================
    // requestReservation (/app/reservation/request)
    // ================================================================

    @Nested
    @DisplayName("requestReservation")
    class RequestReservationTests {

        @Test
        @DisplayName("Debe lanzar excepción cuando el match no existe al solicitar reserva")
        void requestReservation_MatchNotFound_ThrowsException() {
            CreateReservationRequestDTO request = CreateReservationRequestDTO.builder()
                    .matchId(999L)
                    .timeSlotId(1L)
                    .userId(1L)
                    .build();

            Exception exception = assertThrows(RuntimeException.class,
                    () -> reservationService.createReservation(request));

            assertTrue(exception.getMessage().contains("Match not found"));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el usuario no existe al solicitar reserva")
        void requestReservation_UserNotFound_ThrowsException() {
            CreateReservationRequestDTO request = CreateReservationRequestDTO.builder()
                    .matchId(999L)
                    .timeSlotId(999L)
                    .userId(999L)
                    .build();

            assertThrows(RuntimeException.class,
                    () -> reservationService.createReservation(request));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el timeSlot no existe al solicitar reserva")
        void requestReservation_TimeSlotNotFound_ThrowsException() {
            CreateReservationRequestDTO request = CreateReservationRequestDTO.builder()
                    .matchId(999L)
                    .timeSlotId(999L)
                    .userId(1L)
                    .build();

            assertThrows(RuntimeException.class,
                    () -> reservationService.createReservation(request));
        }
    }

    // ================================================================
    // respondToReservation (/app/reservation/respond)
    // ================================================================

    @Nested
    @DisplayName("respondToReservation")
    class RespondToReservationTests {

        @Test
        @DisplayName("Debe lanzar excepción cuando la reserva no existe al aceptar")
        void respondToReservation_ReservationNotFound_Accept_ThrowsException() {
            RespondReservationDTO respond = RespondReservationDTO.builder()
                    .reservationId(999L)
                    .respondingUserId(2L)
                    .status(ReservationStatus.ACCEPTED)
                    .build();

            UpdateReservationStatusRequestDTO dto = new UpdateReservationStatusRequestDTO();
            dto.setStatus(respond.getStatus());
            dto.setRespondingUserId(respond.getRespondingUserId());

            Exception exception = assertThrows(RuntimeException.class,
                    () -> reservationService.updateReservationStatus(
                            respond.getReservationId(), dto));

            assertTrue(exception.getMessage().contains("Reservation not found"));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando la reserva no existe al rechazar")
        void respondToReservation_ReservationNotFound_Reject_ThrowsException() {
            RespondReservationDTO respond = RespondReservationDTO.builder()
                    .reservationId(999L)
                    .respondingUserId(2L)
                    .status(ReservationStatus.REJECTED)
                    .build();

            UpdateReservationStatusRequestDTO dto = new UpdateReservationStatusRequestDTO();
            dto.setStatus(respond.getStatus());
            dto.setRespondingUserId(respond.getRespondingUserId());

            assertThrows(RuntimeException.class,
                    () -> reservationService.updateReservationStatus(
                            respond.getReservationId(), dto));
        }

        @Test
        @DisplayName("Debe mapear correctamente el RespondReservationDTO al UpdateReservationStatusRequestDTO")
        void respondToReservation_MapsCorrectly() {
            RespondReservationDTO respond = RespondReservationDTO.builder()
                    .reservationId(999L)
                    .respondingUserId(2L)
                    .status(ReservationStatus.ACCEPTED)
                    .build();

            UpdateReservationStatusRequestDTO dto = new UpdateReservationStatusRequestDTO();
            dto.setStatus(respond.getStatus());
            dto.setRespondingUserId(respond.getRespondingUserId());

            assertEquals(ReservationStatus.ACCEPTED, dto.getStatus());
            assertEquals(2L, dto.getRespondingUserId());
        }
    }
}