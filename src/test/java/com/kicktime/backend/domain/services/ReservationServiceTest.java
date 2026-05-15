package com.kicktime.backend.domain.services;

import com.kicktime.backend.domain.model.*;
import com.kicktime.backend.domain.model.dto.request.CreateReservationRequestDTO;
import com.kicktime.backend.domain.model.dto.request.UpdateReservationStatusRequestDTO;
import com.kicktime.backend.domain.model.dto.response.ReservationResponseDTO;
import com.kicktime.backend.domain.model.enums.MatchStatus;
import com.kicktime.backend.domain.model.enums.ReservationStatus;
import com.kicktime.backend.domain.model.enums.TimeSlotStatus;
import com.kicktime.backend.repository.*;
import com.kicktime.backend.util.mappers.ReservationMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationService - Pruebas Unitarias")
class ReservationServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private MatchRepository matchRepository;
    @Mock private TimeSlotRepository timeSlotRepository;
    @Mock private UserRepository userRepository;
    @Mock private ReservationMapper reservationMapper;
    @Mock private ReservationWebSocketService reservationWebSocketService;

    @InjectMocks
    private ReservationService reservationService;

    // ----------------------------------------------------------------
    // Datos de prueba reutilizables
    // ----------------------------------------------------------------

    private User homeCaptain;
    private User awayCaptain;
    private Team homeTeam;
    private Team awayTeam;
    private Match match;
    private TimeSlot timeSlot;
    private Reservation reservation;
    private ReservationResponseDTO reservationResponseDTO;
    private CreateReservationRequestDTO createRequest;

    @BeforeEach
    void setUp() {
        homeCaptain = User.builder()
                .id(1L)
                .name("Capitán Local")
                .email("home@kicktime.com")
                .build();

        awayCaptain = User.builder()
                .id(2L)
                .name("Capitán Visitante")
                .email("away@kicktime.com")
                .build();

        homeTeam = Team.builder()
                .id(1L)
                .name("Equipo Local")
                .captain(homeCaptain)
                .build();

        awayTeam = Team.builder()
                .id(2L)
                .name("Equipo Visitante")
                .captain(awayCaptain)
                .build();

        timeSlot = TimeSlot.builder()
                .id(1L)
                .status(TimeSlotStatus.AVAILABLE)
                .build();

        match = Match.builder()
                .id(1L)
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .status(MatchStatus.SCHEDULED)
                .build();

        reservation = Reservation.builder()
                .id(1L)
                .match(match)
                .timeSlot(timeSlot)
                .proposedBy(homeCaptain)
                .status(ReservationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        reservationResponseDTO = ReservationResponseDTO.builder()
                .id(1L)
                .matchId(1L)
                .timeSlotId(1L)
                .proposedByUserId(1L)
                .proposedByName("Capitán Local")
                .status(ReservationStatus.PENDING)
                .build();

        createRequest = CreateReservationRequestDTO.builder()
                .matchId(1L)
                .timeSlotId(1L)
                .userId(1L)
                .build();
    }

    // ================================================================
    // createReservation
    // ================================================================

    @Nested
    @DisplayName("createReservation")
    class CreateReservationTests {

        @Test
        @DisplayName("Debe crear reserva exitosamente cuando el capitán local la solicita")
        void createReservation_HomeCaptain_ReturnsReservationResponseDTO() {
            when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
            when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(timeSlot));
            when(userRepository.findById(1L)).thenReturn(Optional.of(homeCaptain));
            when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);
            when(reservationMapper.toDTO(reservation)).thenReturn(reservationResponseDTO);
            doNothing().when(reservationWebSocketService).lockTimeSlot(anyLong(), anyLong());

            ReservationResponseDTO result = reservationService.createReservation(createRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getStatus()).isEqualTo(ReservationStatus.PENDING);
            verify(reservationRepository).save(any(Reservation.class));
            verify(reservationWebSocketService).lockTimeSlot(anyLong(), anyLong());
        }

        @Test
        @DisplayName("Debe crear reserva exitosamente cuando el capitán visitante la solicita")
        void createReservation_AwayCaptain_ReturnsReservationResponseDTO() {
            CreateReservationRequestDTO awayRequest = CreateReservationRequestDTO.builder()
                    .matchId(1L)
                    .timeSlotId(1L)
                    .userId(2L)
                    .build();

            Reservation awayReservation = Reservation.builder()
                    .id(2L)
                    .match(match)
                    .timeSlot(timeSlot)
                    .proposedBy(awayCaptain)
                    .status(ReservationStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            ReservationResponseDTO awayResponse = ReservationResponseDTO.builder()
                    .id(2L)
                    .matchId(1L)
                    .timeSlotId(1L)
                    .proposedByUserId(2L)
                    .status(ReservationStatus.PENDING)
                    .build();

            when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
            when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(timeSlot));
            when(userRepository.findById(2L)).thenReturn(Optional.of(awayCaptain));
            when(reservationRepository.save(any(Reservation.class))).thenReturn(awayReservation);
            when(reservationMapper.toDTO(awayReservation)).thenReturn(awayResponse);
            doNothing().when(reservationWebSocketService).lockTimeSlot(anyLong(), anyLong());

            ReservationResponseDTO result = reservationService.createReservation(awayRequest);

            assertThat(result).isNotNull();
            assertThat(result.getProposedByUserId()).isEqualTo(2L);
            verify(reservationRepository).save(any(Reservation.class));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el match no existe")
        void createReservation_MatchNotFound_ThrowsException() {
            when(matchRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.createReservation(createRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Match not found");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el timeSlot no existe")
        void createReservation_TimeSlotNotFound_ThrowsException() {
            when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
            when(timeSlotRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.createReservation(createRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("TimeSlot not found");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el usuario no existe")
        void createReservation_UserNotFound_ThrowsException() {
            when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
            when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(timeSlot));
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.createReservation(createRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el usuario no es capitán de ningún equipo")
        void createReservation_UserNotCaptain_ThrowsException() {
            User randomUser = User.builder()
                    .id(99L)
                    .name("Usuario Cualquiera")
                    .email("random@kicktime.com")
                    .build();

            CreateReservationRequestDTO randomRequest = CreateReservationRequestDTO.builder()
                    .matchId(1L)
                    .timeSlotId(1L)
                    .userId(99L)
                    .build();

            when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
            when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(timeSlot));
            when(userRepository.findById(99L)).thenReturn(Optional.of(randomUser));

            assertThatThrownBy(() -> reservationService.createReservation(randomRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Only team captains can create reservations");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el timeSlot ya está reservado")
        void createReservation_TimeSlotNotAvailable_ThrowsException() {
            TimeSlot reservedSlot = TimeSlot.builder()
                    .id(1L)
                    .status(TimeSlotStatus.RESERVED)
                    .build();

            when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
            when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(reservedSlot));
            when(userRepository.findById(1L)).thenReturn(Optional.of(homeCaptain));

            assertThatThrownBy(() -> reservationService.createReservation(createRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("TimeSlot is already reserved");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe asignar estado PENDING automáticamente al crear")
        void createReservation_ShouldAssignPendingStatusAutomatically() {
            when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
            when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(timeSlot));
            when(userRepository.findById(1L)).thenReturn(Optional.of(homeCaptain));
            when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
                Reservation saved = invocation.getArgument(0);
                assertThat(saved.getStatus()).isEqualTo(ReservationStatus.PENDING);
                return reservation;
            });
            when(reservationMapper.toDTO(any())).thenReturn(reservationResponseDTO);
            doNothing().when(reservationWebSocketService).lockTimeSlot(anyLong(), anyLong());

            reservationService.createReservation(createRequest);

            verify(reservationRepository).save(any(Reservation.class));
        }
    }

    // ================================================================
    // getReservationsForMatch
    // ================================================================

    @Nested
    @DisplayName("getReservationsForMatch")
    class GetReservationsForMatchTests {

        @Test
        @DisplayName("Debe retornar lista de reservas para un partido")
        void getReservationsForMatch_WithData_ReturnsList() {
            when(reservationRepository.findByMatchId(1L)).thenReturn(List.of(reservation));
            when(reservationMapper.toDTO(reservation)).thenReturn(reservationResponseDTO);

            List<ReservationResponseDTO> result = reservationService.getReservationsForMatch(1L);

            assertThat(result).isNotNull().hasSize(1);
            assertThat(result.get(0).getMatchId()).isEqualTo(1L);
            verify(reservationRepository).findByMatchId(1L);
        }

        @Test
        @DisplayName("Debe retornar lista vacía cuando no hay reservas para el partido")
        void getReservationsForMatch_NoData_ReturnsEmptyList() {
            when(reservationRepository.findByMatchId(1L)).thenReturn(List.of());

            List<ReservationResponseDTO> result = reservationService.getReservationsForMatch(1L);

            assertThat(result).isNotNull().isEmpty();
            verify(reservationRepository).findByMatchId(1L);
        }
    }

    // ================================================================
    // updateReservationStatus
    // ================================================================

    @Nested
    @DisplayName("updateReservationStatus")
    class UpdateReservationStatusTests {

        @Test
        @DisplayName("Debe aceptar la reserva cuando el capitán rival confirma")
        void updateReservationStatus_Accepted_ConfirmsMatchAndRejectsOthers() {
            UpdateReservationStatusRequestDTO request = UpdateReservationStatusRequestDTO.builder()
                    .status(ReservationStatus.ACCEPTED)
                    .respondingUserId(2L) // awayCaptain responde la reserva del homeCaptain
                    .build();

            Reservation otherReservation = Reservation.builder()
                    .id(2L)
                    .match(match)
                    .timeSlot(timeSlot)
                    .proposedBy(awayCaptain)
                    .status(ReservationStatus.PENDING)
                    .build();

            ReservationResponseDTO acceptedResponse = ReservationResponseDTO.builder()
                    .id(1L)
                    .status(ReservationStatus.ACCEPTED)
                    .build();

            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
            when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);
            when(reservationRepository.findByMatchId(1L)).thenReturn(List.of(reservation, otherReservation));
            when(reservationMapper.toDTO(reservation)).thenReturn(acceptedResponse);
            doNothing().when(reservationWebSocketService).confirmReservation(anyLong(), anyLong());

            ReservationResponseDTO result = reservationService.updateReservationStatus(1L, request);

            assertThat(result.getStatus()).isEqualTo(ReservationStatus.ACCEPTED);
            // Verifica que se confirmó por WebSocket
            verify(reservationWebSocketService).confirmReservation(anyLong(), anyLong());
            // Verifica que se guardó el match con estado CONFIRMED
            verify(matchRepository).save(any(Match.class));
            // Verifica que se rechazó la otra reserva
            verify(reservationRepository, atLeast(2)).save(any(Reservation.class));
        }

        @Test
        @DisplayName("Debe rechazar la reserva y liberar el timeSlot por WebSocket")
        void updateReservationStatus_Rejected_ReleasesTimeSlot() {
            UpdateReservationStatusRequestDTO request = UpdateReservationStatusRequestDTO.builder()
                    .status(ReservationStatus.REJECTED)
                    .respondingUserId(2L)
                    .build();

            ReservationResponseDTO rejectedResponse = ReservationResponseDTO.builder()
                    .id(1L)
                    .status(ReservationStatus.REJECTED)
                    .build();

            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
            when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);
            when(reservationMapper.toDTO(reservation)).thenReturn(rejectedResponse);
            doNothing().when(reservationWebSocketService).releaseTimeSlot(anyLong(), anyLong());

            ReservationResponseDTO result = reservationService.updateReservationStatus(1L, request);

            assertThat(result.getStatus()).isEqualTo(ReservationStatus.REJECTED);
            verify(reservationWebSocketService).releaseTimeSlot(anyLong(), anyLong());
            verify(matchRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando la reserva no existe")
        void updateReservationStatus_ReservationNotFound_ThrowsException() {
            UpdateReservationStatusRequestDTO request = UpdateReservationStatusRequestDTO.builder()
                    .status(ReservationStatus.ACCEPTED)
                    .respondingUserId(2L)
                    .build();

            when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.updateReservationStatus(99L, request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Reservation not found");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando quien responde no es el capitán rival")
        void updateReservationStatus_NotRivalCaptain_ThrowsException() {
            UpdateReservationStatusRequestDTO request = UpdateReservationStatusRequestDTO.builder()
                    .status(ReservationStatus.ACCEPTED)
                    .respondingUserId(99L) // usuario que no es capitán rival
                    .build();

            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

            assertThatThrownBy(() -> reservationService.updateReservationStatus(1L, request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Only the rival captain can confirm or reject this reservation");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("No debe llamar WebSocket ni guardar match cuando el estado no es ACCEPTED ni REJECTED")
        void updateReservationStatus_PendingStatus_NoWebSocketCall() {
            UpdateReservationStatusRequestDTO request = UpdateReservationStatusRequestDTO.builder()
                    .status(ReservationStatus.EXPIRED)
                    .respondingUserId(2L)
                    .build();

            ReservationResponseDTO expiredResponse = ReservationResponseDTO.builder()
                    .id(1L)
                    .status(ReservationStatus.EXPIRED)
                    .build();

            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
            when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);
            when(reservationMapper.toDTO(reservation)).thenReturn(expiredResponse);

            ReservationResponseDTO result = reservationService.updateReservationStatus(1L, request);

            assertThat(result.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
            verify(reservationWebSocketService, never()).confirmReservation(anyLong(), anyLong());
            verify(reservationWebSocketService, never()).releaseTimeSlot(anyLong(), anyLong());
            verify(matchRepository, never()).save(any());
        }
    }

    // ================================================================
    // deleteReservation
    // ================================================================

    @Nested
    @DisplayName("deleteReservation")
    class DeleteReservationTests {

        @Test
        @DisplayName("Debe eliminar la reserva exitosamente cuando existe")
        void deleteReservation_ExistingId_DeletesSuccessfully() {
            when(reservationRepository.existsById(1L)).thenReturn(true);
            doNothing().when(reservationRepository).deleteById(1L);

            assertThatCode(() -> reservationService.deleteReservation(1L))
                    .doesNotThrowAnyException();

            verify(reservationRepository).existsById(1L);
            verify(reservationRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando la reserva no existe al eliminar")
        void deleteReservation_NonExistingId_ThrowsException() {
            when(reservationRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> reservationService.deleteReservation(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Reservation not found");

            verify(reservationRepository, never()).deleteById(any());
        }
    }
}
