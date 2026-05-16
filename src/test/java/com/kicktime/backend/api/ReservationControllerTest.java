package com.kicktime.backend.api;

import com.kicktime.backend.domain.model.*;
import com.kicktime.backend.domain.model.dto.request.CreateReservationRequestDTO;
import com.kicktime.backend.domain.model.dto.request.UpdateReservationStatusRequestDTO;
import com.kicktime.backend.domain.model.dto.response.ReservationResponseDTO;
import com.kicktime.backend.domain.model.enums.*;
import com.kicktime.backend.repository.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("ReservationController - Pruebas de Integración")
class ReservationControllerTest {

    @Autowired
    private ReservationController reservationController;

    // ─── Repositorios para fixtures ───────────────────────────────────────────
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private MatchRepository       matchRepository;
    @Autowired private TimeSlotRepository    timeSlotRepository;
    @Autowired private UserRepository        userRepository;
    @Autowired private TeamRepository        teamRepository;
    @Autowired private TournamentRepository  tournamentRepository;

    // =========================================================================
    // Helpers de fixtures
    // =========================================================================

    /** Crea un User capitán con email único. */
    private User buildCaptain(String tag) {
        return userRepository.save(
                User.builder()
                        .name("Capitán " + tag)
                        .email("cap_" + tag + "_" + System.nanoTime() + "@kicktime.test")
                        .password("encoded")
                        .role(UserRole.CAPTAIN)
                        .build()
        );
    }

    /** Crea un Tournament mínimo. */
    private Tournament buildTournament() {
        return tournamentRepository.save(
                Tournament.builder()
                        .name("Torneo Test " + System.nanoTime())
                        .semester("2025-1")
                        .status(TournamentStatus.ONGOING)
                        .category(TournamentCategory.MALE)
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now().plusMonths(2))
                        .minPlayersPerTeam(5)
                        .maxPlayersPerTeam(11)
                        .minTeams(2)
                        .maxTeams(8)
                        .build()
        );
    }

    /** Crea un Team con su capitán y torneo. */
    private Team buildTeam(String tag, User captain, Tournament tournament) {
        return teamRepository.save(
                Team.builder()
                        .name("Equipo " + tag + " " + System.nanoTime())
                        .captain(captain)
                        .tournament(tournament)
                        .build()
        );
    }

    /** Crea un TimeSlot AVAILABLE. */
    private TimeSlot buildTimeSlot() {
        return timeSlotRepository.save(
                TimeSlot.builder()
                        .status(TimeSlotStatus.AVAILABLE)
                        .build()
        );
    }

    /** Crea un Match SCHEDULED entre dos equipos. */
    private Match buildMatch(Team home, Team away, Tournament tournament) {
        return matchRepository.save(
                Match.builder()
                        .homeTeam(home)
                        .awayTeam(away)
                        .status(MatchStatus.SCHEDULED)
                        .tournament(tournament)
                        .build()
        );
    }

    /**
     * Fixture completo: Tournament + 2 capitanes + 2 equipos + TimeSlot + Match.
     * Devuelve el contexto como array:
     *   [0] homeCaptain, [1] awayCaptain, [2] homeTeam, [3] awayTeam,
     *   [4] timeSlot,    [5] match,       [6] tournament
     */
    private Object[] buildFullFixture() {
        Tournament tournament  = buildTournament();
        User       homeCaptain = buildCaptain("home");
        User       awayCaptain = buildCaptain("away");
        Team       homeTeam    = buildTeam("home", homeCaptain, tournament);
        Team       awayTeam    = buildTeam("away", awayCaptain, tournament);
        TimeSlot   timeSlot    = buildTimeSlot();
        Match      match       = buildMatch(homeTeam, awayTeam, tournament);
        return new Object[]{ homeCaptain, awayCaptain, homeTeam, awayTeam,
                timeSlot, match, tournament };
    }

    /**
     * Limpia en orden correcto respetando FKs.
     * reservationIds → match → timeSlot → teams → captains → tournament
     */
    private void cleanFixture(List<Long> reservationIds, Long matchId,
                              Long timeSlotId, Long homeTeamId, Long awayTeamId,
                              Long homeCaptainId, Long awayCaptainId,
                              Long tournamentId) {
        reservationIds.forEach(id -> {
            if (id != null && reservationRepository.existsById(id)) {
                reservationRepository.deleteById(id);
            }
        });
        if (matchId    != null) matchRepository.deleteById(matchId);
        if (timeSlotId != null) timeSlotRepository.deleteById(timeSlotId);
        if (homeTeamId != null) teamRepository.deleteById(homeTeamId);
        if (awayTeamId != null) teamRepository.deleteById(awayTeamId);
        if (homeCaptainId != null) userRepository.deleteById(homeCaptainId);
        if (awayCaptainId != null) userRepository.deleteById(awayCaptainId);
        if (tournamentId  != null) tournamentRepository.deleteById(tournamentId);
    }

    // =========================================================================
    // createReservation
    // =========================================================================

    @Nested
    @DisplayName("createReservation")
    class CreateReservationTests {

        @Test
        @DisplayName("Debe retornar 201 CREATED cuando el capitán local crea reserva válida")
        void createReservation_HomeCaptain_Returns201() {
            Object[] f           = buildFullFixture();
            User      homeCaptain = (User)     f[0];
            User      awayCaptain = (User)     f[1];
            Team      homeTeam    = (Team)     f[2];
            Team      awayTeam    = (Team)     f[3];
            TimeSlot  timeSlot    = (TimeSlot) f[4];
            Match     match       = (Match)    f[5];
            Tournament tournament = (Tournament) f[6];

            Long reservationId = null;
            try {
                CreateReservationRequestDTO request = CreateReservationRequestDTO.builder()
                        .matchId(match.getId())
                        .timeSlotId(timeSlot.getId())
                        .userId(homeCaptain.getId())
                        .build();

                ResponseEntity<ReservationResponseDTO> response =
                        reservationController.createReservation(request);

                assertNotNull(response);
                assertEquals(201, response.getStatusCode().value());
                assertNotNull(response.getBody());
                assertNotNull(response.getBody().getId());
                reservationId = response.getBody().getId();

            } finally {
                cleanFixture(
                        List.of(reservationId != null ? reservationId : -1L),
                        match.getId(), timeSlot.getId(),
                        homeTeam.getId(), awayTeam.getId(),
                        homeCaptain.getId(), awayCaptain.getId(),
                        tournament.getId()
                );
            }
        }

        @Test
        @DisplayName("Debe retornar 201 CREATED cuando el capitán visitante crea reserva válida")
        void createReservation_AwayCaptain_Returns201() {
            Object[] f           = buildFullFixture();
            User      homeCaptain = (User)     f[0];
            User      awayCaptain = (User)     f[1];
            Team      homeTeam    = (Team)     f[2];
            Team      awayTeam    = (Team)     f[3];
            TimeSlot  timeSlot    = (TimeSlot) f[4];
            Match     match       = (Match)    f[5];
            Tournament tournament = (Tournament) f[6];

            Long reservationId = null;
            try {
                CreateReservationRequestDTO request = CreateReservationRequestDTO.builder()
                        .matchId(match.getId())
                        .timeSlotId(timeSlot.getId())
                        .userId(awayCaptain.getId())
                        .build();

                ResponseEntity<ReservationResponseDTO> response =
                        reservationController.createReservation(request);

                assertNotNull(response);
                assertEquals(201, response.getStatusCode().value());
                assertNotNull(response.getBody());
                reservationId = response.getBody().getId();

            } finally {
                cleanFixture(
                        List.of(reservationId != null ? reservationId : -1L),
                        match.getId(), timeSlot.getId(),
                        homeTeam.getId(), awayTeam.getId(),
                        homeCaptain.getId(), awayCaptain.getId(),
                        tournament.getId()
                );
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el TimeSlot ya está RESERVED")
        void createReservation_TimeSlotReserved_ThrowsException() {
            Object[] f           = buildFullFixture();
            User      homeCaptain = (User)     f[0];
            User      awayCaptain = (User)     f[1];
            Team      homeTeam    = (Team)     f[2];
            Team      awayTeam    = (Team)     f[3];
            TimeSlot  timeSlot    = (TimeSlot) f[4];
            Match     match       = (Match)    f[5];
            Tournament tournament = (Tournament) f[6];

            // Forzar estado RESERVED en el TimeSlot
            timeSlot.setStatus(TimeSlotStatus.RESERVED);
            timeSlotRepository.save(timeSlot);

            try {
                CreateReservationRequestDTO request = CreateReservationRequestDTO.builder()
                        .matchId(match.getId())
                        .timeSlotId(timeSlot.getId())
                        .userId(homeCaptain.getId())
                        .build();

                assertThrows(RuntimeException.class,
                        () -> reservationController.createReservation(request));

            } finally {
                cleanFixture(
                        List.of(),
                        match.getId(), timeSlot.getId(),
                        homeTeam.getId(), awayTeam.getId(),
                        homeCaptain.getId(), awayCaptain.getId(),
                        tournament.getId()
                );
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el usuario no es capitán del partido")
        void createReservation_UserNotCaptain_ThrowsException() {
            Object[] f           = buildFullFixture();
            User      homeCaptain = (User)     f[0];
            User      awayCaptain = (User)     f[1];
            Team      homeTeam    = (Team)     f[2];
            Team      awayTeam    = (Team)     f[3];
            TimeSlot  timeSlot    = (TimeSlot) f[4];
            Match     match       = (Match)    f[5];
            Tournament tournament = (Tournament) f[6];

            // Usuario ajeno al partido
            User outsider = userRepository.save(
                    User.builder()
                            .name("Jugador Externo")
                            .email("outsider_" + System.nanoTime() + "@kicktime.test")
                            .password("encoded")
                            .role(UserRole.PLAYER)
                            .build()
            );

            try {
                CreateReservationRequestDTO request = CreateReservationRequestDTO.builder()
                        .matchId(match.getId())
                        .timeSlotId(timeSlot.getId())
                        .userId(outsider.getId())
                        .build();

                assertThrows(RuntimeException.class,
                        () -> reservationController.createReservation(request));

            } finally {
                cleanFixture(
                        List.of(),
                        match.getId(), timeSlot.getId(),
                        homeTeam.getId(), awayTeam.getId(),
                        homeCaptain.getId(), awayCaptain.getId(),
                        tournament.getId()
                );
                userRepository.deleteById(outsider.getId());
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el Match no existe")
        void createReservation_MatchNotFound_ThrowsException() {
            CreateReservationRequestDTO request = CreateReservationRequestDTO.builder()
                    .matchId(999999L)
                    .timeSlotId(1L)
                    .userId(1L)
                    .build();

            assertThrows(RuntimeException.class,
                    () -> reservationController.createReservation(request));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el TimeSlot no existe")
        void createReservation_TimeSlotNotFound_ThrowsException() {
            Object[] f           = buildFullFixture();
            User      homeCaptain = (User)     f[0];
            User      awayCaptain = (User)     f[1];
            Team      homeTeam    = (Team)     f[2];
            Team      awayTeam    = (Team)     f[3];
            TimeSlot  timeSlot    = (TimeSlot) f[4];
            Match     match       = (Match)    f[5];
            Tournament tournament = (Tournament) f[6];

            try {
                CreateReservationRequestDTO request = CreateReservationRequestDTO.builder()
                        .matchId(match.getId())
                        .timeSlotId(999999L)
                        .userId(homeCaptain.getId())
                        .build();

                assertThrows(RuntimeException.class,
                        () -> reservationController.createReservation(request));

            } finally {
                cleanFixture(
                        List.of(),
                        match.getId(), timeSlot.getId(),
                        homeTeam.getId(), awayTeam.getId(),
                        homeCaptain.getId(), awayCaptain.getId(),
                        tournament.getId()
                );
            }
        }
    }

    // =========================================================================
    // getReservationsForMatch
    // =========================================================================

    @Nested
    @DisplayName("getReservationsForMatch")
    class GetReservationsForMatchTests {

        @Test
        @DisplayName("Debe retornar 200 OK con lista vacía cuando no hay reservas")
        void getReservationsForMatch_NoReservations_Returns200() {
            ResponseEntity<List<ReservationResponseDTO>> response =
                    reservationController.getReservationsForMatch(999999L);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isEmpty());
        }

        @Test
        @DisplayName("Debe retornar 200 OK con reservas cuando existen para el partido")
        void getReservationsForMatch_WithReservations_Returns200AndList() {
            Object[] f           = buildFullFixture();
            User      homeCaptain = (User)     f[0];
            User      awayCaptain = (User)     f[1];
            Team      homeTeam    = (Team)     f[2];
            Team      awayTeam    = (Team)     f[3];
            TimeSlot  timeSlot    = (TimeSlot) f[4];
            Match     match       = (Match)    f[5];
            Tournament tournament = (Tournament) f[6];

            // Insertar reserva directamente
            Reservation reservation = reservationRepository.save(
                    Reservation.builder()
                            .match(match)
                            .timeSlot(timeSlot)
                            .proposedBy(homeCaptain)
                            .status(ReservationStatus.PENDING)
                            .createdAt(java.time.LocalDateTime.now())
                            .build()
            );

            try {
                ResponseEntity<List<ReservationResponseDTO>> response =
                        reservationController.getReservationsForMatch(match.getId());

                assertNotNull(response);
                assertEquals(200, response.getStatusCode().value());
                assertNotNull(response.getBody());
                assertFalse(response.getBody().isEmpty());

            } finally {
                cleanFixture(
                        List.of(reservation.getId()),
                        match.getId(), timeSlot.getId(),
                        homeTeam.getId(), awayTeam.getId(),
                        homeCaptain.getId(), awayCaptain.getId(),
                        tournament.getId()
                );
            }
        }
    }

    // =========================================================================
    // updateReservationStatus
    // =========================================================================

    @Nested
    @DisplayName("updateReservationStatus")
    class UpdateReservationStatusTests {

        @Test
        @DisplayName("Debe retornar 200 OK y rechazar la reserva cuando el capitán rival la rechaza")
        void updateReservationStatus_RivalCaptainRejects_Returns200() {
            Object[] f           = buildFullFixture();
            User      homeCaptain = (User)     f[0];
            User      awayCaptain = (User)     f[1];
            Team      homeTeam    = (Team)     f[2];
            Team      awayTeam    = (Team)     f[3];
            TimeSlot  timeSlot    = (TimeSlot) f[4];
            Match     match       = (Match)    f[5];
            Tournament tournament = (Tournament) f[6];

            // homeCaptain propone → awayCaptain responde
            Reservation reservation = reservationRepository.save(
                    Reservation.builder()
                            .match(match)
                            .timeSlot(timeSlot)
                            .proposedBy(homeCaptain)
                            .status(ReservationStatus.PENDING)
                            .createdAt(java.time.LocalDateTime.now())
                            .build()
            );

            try {
                UpdateReservationStatusRequestDTO request =
                        UpdateReservationStatusRequestDTO.builder()
                                .status(ReservationStatus.REJECTED)
                                .respondingUserId(awayCaptain.getId())
                                .build();

                ResponseEntity<ReservationResponseDTO> response =
                        reservationController.updateReservationStatus(reservation.getId(), request);

                assertNotNull(response);
                assertEquals(200, response.getStatusCode().value());
                assertNotNull(response.getBody());
                assertEquals(ReservationStatus.REJECTED, response.getBody().getStatus());

            } finally {
                cleanFixture(
                        List.of(reservation.getId()),
                        match.getId(), timeSlot.getId(),
                        homeTeam.getId(), awayTeam.getId(),
                        homeCaptain.getId(), awayCaptain.getId(),
                        tournament.getId()
                );
            }
        }

        @Test
        @DisplayName("Debe retornar 200 OK, confirmar match y rechazar otras reservas cuando se acepta")
        void updateReservationStatus_RivalCaptainAccepts_Returns200AndConfirmsMatch() {
            Object[] f           = buildFullFixture();
            User      homeCaptain = (User)     f[0];
            User      awayCaptain = (User)     f[1];
            Team      homeTeam    = (Team)     f[2];
            Team      awayTeam    = (Team)     f[3];
            TimeSlot  timeSlot    = (TimeSlot) f[4];
            Match     match       = (Match)    f[5];
            Tournament tournament = (Tournament) f[6];

            // Segunda reserva del mismo partido (para verificar que queda REJECTED)
            TimeSlot secondSlot = buildTimeSlot();

            Reservation mainReservation = reservationRepository.save(
                    Reservation.builder()
                            .match(match)
                            .timeSlot(timeSlot)
                            .proposedBy(homeCaptain)
                            .status(ReservationStatus.PENDING)
                            .createdAt(java.time.LocalDateTime.now())
                            .build()
            );
            Reservation otherReservation = reservationRepository.save(
                    Reservation.builder()
                            .match(match)
                            .timeSlot(secondSlot)
                            .proposedBy(awayCaptain)
                            .status(ReservationStatus.PENDING)
                            .createdAt(java.time.LocalDateTime.now())
                            .build()
            );

            try {
                UpdateReservationStatusRequestDTO request =
                        UpdateReservationStatusRequestDTO.builder()
                                .status(ReservationStatus.ACCEPTED)
                                .respondingUserId(awayCaptain.getId())
                                .build();

                ResponseEntity<ReservationResponseDTO> response =
                        reservationController.updateReservationStatus(mainReservation.getId(), request);

                assertNotNull(response);
                assertEquals(200, response.getStatusCode().value());
                assertNotNull(response.getBody());
                assertEquals(ReservationStatus.ACCEPTED, response.getBody().getStatus());

                // La otra reserva del mismo match debe haber quedado REJECTED
                Reservation updated = reservationRepository.findById(otherReservation.getId())
                        .orElseThrow();
                assertEquals(ReservationStatus.REJECTED, updated.getStatus());

            } finally {
                // Borrar todas las reservas del match primero
                reservationRepository.findByMatchId(match.getId())
                        .forEach(r -> reservationRepository.deleteById(r.getId()));
                matchRepository.deleteById(match.getId());
                timeSlotRepository.deleteById(timeSlot.getId());
                timeSlotRepository.deleteById(secondSlot.getId());
                teamRepository.deleteById(homeTeam.getId());
                teamRepository.deleteById(awayTeam.getId());
                userRepository.deleteById(homeCaptain.getId());
                userRepository.deleteById(awayCaptain.getId());
                tournamentRepository.deleteById(tournament.getId());
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando quien responde no es el capitán rival")
        void updateReservationStatus_WrongUser_ThrowsException() {
            Object[] f           = buildFullFixture();
            User      homeCaptain = (User)     f[0];
            User      awayCaptain = (User)     f[1];
            Team      homeTeam    = (Team)     f[2];
            Team      awayTeam    = (Team)     f[3];
            TimeSlot  timeSlot    = (TimeSlot) f[4];
            Match     match       = (Match)    f[5];
            Tournament tournament = (Tournament) f[6];

            Reservation reservation = reservationRepository.save(
                    Reservation.builder()
                            .match(match)
                            .timeSlot(timeSlot)
                            .proposedBy(homeCaptain)
                            .status(ReservationStatus.PENDING)
                            .createdAt(java.time.LocalDateTime.now())
                            .build()
            );

            try {
                // homeCaptain intenta responder su propia reserva — no es el rival
                UpdateReservationStatusRequestDTO request =
                        UpdateReservationStatusRequestDTO.builder()
                                .status(ReservationStatus.ACCEPTED)
                                .respondingUserId(homeCaptain.getId())
                                .build();

                assertThrows(RuntimeException.class,
                        () -> reservationController.updateReservationStatus(
                                reservation.getId(), request));

            } finally {
                cleanFixture(
                        List.of(reservation.getId()),
                        match.getId(), timeSlot.getId(),
                        homeTeam.getId(), awayTeam.getId(),
                        homeCaptain.getId(), awayCaptain.getId(),
                        tournament.getId()
                );
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando la reserva no existe")
        void updateReservationStatus_ReservationNotFound_ThrowsException() {
            UpdateReservationStatusRequestDTO request =
                    UpdateReservationStatusRequestDTO.builder()
                            .status(ReservationStatus.ACCEPTED)
                            .respondingUserId(1L)
                            .build();

            assertThrows(RuntimeException.class,
                    () -> reservationController.updateReservationStatus(999999L, request));
        }
    }

    // =========================================================================
    // deleteReservation
    // =========================================================================

    @Nested
    @DisplayName("deleteReservation")
    class DeleteReservationTests {

        @Test
        @DisplayName("Debe retornar 204 NO CONTENT al eliminar una reserva existente")
        void deleteReservation_ExistingReservation_Returns204() {
            Object[] f           = buildFullFixture();
            User      homeCaptain = (User)     f[0];
            User      awayCaptain = (User)     f[1];
            Team      homeTeam    = (Team)     f[2];
            Team      awayTeam    = (Team)     f[3];
            TimeSlot  timeSlot    = (TimeSlot) f[4];
            Match     match       = (Match)    f[5];
            Tournament tournament = (Tournament) f[6];

            Reservation reservation = reservationRepository.save(
                    Reservation.builder()
                            .match(match)
                            .timeSlot(timeSlot)
                            .proposedBy(homeCaptain)
                            .status(ReservationStatus.PENDING)
                            .createdAt(java.time.LocalDateTime.now())
                            .build()
            );

            try {
                ResponseEntity<Void> response =
                        reservationController.deleteReservation(reservation.getId());

                assertNotNull(response);
                assertEquals(204, response.getStatusCode().value());
                assertFalse(reservationRepository.existsById(reservation.getId()));

            } finally {
                // La reserva ya fue borrada por el controlador; limpiar el resto
                cleanFixture(
                        List.of(), // ya borrada
                        match.getId(), timeSlot.getId(),
                        homeTeam.getId(), awayTeam.getId(),
                        homeCaptain.getId(), awayCaptain.getId(),
                        tournament.getId()
                );
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción al eliminar una reserva inexistente")
        void deleteReservation_NonExistingReservation_ThrowsException() {
            assertThrows(RuntimeException.class,
                    () -> reservationController.deleteReservation(999999L));
        }
    }
}