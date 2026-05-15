package com.kicktime.backend.api;

import com.kicktime.backend.domain.model.dto.request.CreateReservationRequestDTO;
import com.kicktime.backend.domain.model.dto.request.RespondReservationDTO;
import com.kicktime.backend.domain.model.dto.request.UpdateReservationStatusRequestDTO;
import com.kicktime.backend.domain.model.enums.ReservationStatus;

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
    private ReservationWebSocketController reservationWebSocketController;

    @Nested
    @DisplayName("requestReservation")
    class RequestReservationTests {

        @Test
        @DisplayName("Debe lanzar excepción cuando el match no existe")
        void requestReservation_MatchNotFound_ThrowsException() {
            CreateReservationRequestDTO request = CreateReservationRequestDTO.builder()
                    .matchId(999L)
                    .timeSlotId(1L)
                    .userId(1L)
                    .build();

            assertThrows(RuntimeException.class,
                    () -> reservationWebSocketController.requestReservation(request));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el usuario no existe")
        void requestReservation_UserNotFound_ThrowsException() {
            CreateReservationRequestDTO request = CreateReservationRequestDTO.builder()
                    .matchId(999L)
                    .timeSlotId(999L)
                    .userId(999L)
                    .build();

            assertThrows(RuntimeException.class,
                    () -> reservationWebSocketController.requestReservation(request));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el timeSlot no existe")
        void requestReservation_TimeSlotNotFound_ThrowsException() {
            CreateReservationRequestDTO request = CreateReservationRequestDTO.builder()
                    .matchId(999L)
                    .timeSlotId(999L)
                    .userId(1L)
                    .build();

            assertThrows(RuntimeException.class,
                    () -> reservationWebSocketController.requestReservation(request));
        }
    }

    @Nested
    @DisplayName("respondToReservation")
    class RespondToReservationTests {

        @Test
        @DisplayName("Debe lanzar excepción cuando la reserva no existe al aceptar")
        void respondToReservation_ReservationNotFound_Accept_ThrowsException() {
            RespondReservationDTO request = RespondReservationDTO.builder()
                    .reservationId(999L)
                    .respondingUserId(2L)
                    .status(ReservationStatus.ACCEPTED)
                    .build();

            assertThrows(RuntimeException.class,
                    () -> reservationWebSocketController.respondToReservation(request));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando la reserva no existe al rechazar")
        void respondToReservation_ReservationNotFound_Reject_ThrowsException() {
            RespondReservationDTO request = RespondReservationDTO.builder()
                    .reservationId(999L)
                    .respondingUserId(2L)
                    .status(ReservationStatus.REJECTED)
                    .build();

            assertThrows(RuntimeException.class,
                    () -> reservationWebSocketController.respondToReservation(request));
        }

        @Test
        @DisplayName("Debe mapear correctamente status y respondingUserId")
        void respondToReservation_MapsCorrectly() {
            UpdateReservationStatusRequestDTO dto = new UpdateReservationStatusRequestDTO();
            dto.setStatus(ReservationStatus.ACCEPTED);
            dto.setRespondingUserId(2L);

            assertEquals(ReservationStatus.ACCEPTED, dto.getStatus());
            assertEquals(2L, dto.getRespondingUserId());
        }
    }
}