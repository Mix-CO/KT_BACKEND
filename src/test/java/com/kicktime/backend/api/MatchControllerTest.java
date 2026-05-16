package com.kicktime.backend.api;

import com.kicktime.backend.domain.model.*;
import com.kicktime.backend.domain.model.dto.request.CreateMatchRequestDTO;
import com.kicktime.backend.domain.model.dto.request.RecordMatchResultRequestDTO;
import com.kicktime.backend.domain.model.dto.request.ScheduleMatchRequestDTO;
import com.kicktime.backend.domain.model.dto.response.MatchResponseDTO;
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
@DisplayName("MatchController - Pruebas de Integración")
class MatchControllerTest {

    @Autowired
    private MatchController matchController;

    @Autowired private MatchRepository       matchRepository;
    @Autowired private MatchResultRepository matchResultRepository;
    @Autowired private TeamRepository        teamRepository;
    @Autowired private UserRepository        userRepository;
    @Autowired private TournamentRepository  tournamentRepository;
    @Autowired private FieldRepository       fieldRepository;
    @Autowired private TimeSlotRepository    timeSlotRepository;
    @Autowired private StandingRepository    standingRepository;

    // =========================================================================
    // Helpers de fixtures
    // =========================================================================

    private Tournament buildTournament() {
        return tournamentRepository.save(
                Tournament.builder()
                        .name("Torneo Match Test " + System.nanoTime())
                        .semester("2026-1")
                        .status(TournamentStatus.ONGOING)
                        .category(TournamentCategory.MALE)
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now().plusMonths(2))
                        .minPlayersPerTeam(5).maxPlayersPerTeam(11)
                        .minTeams(2).maxTeams(8)
                        .build()
        );
    }

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

    private Team buildTeam(String tag, User captain, Tournament tournament) {
        return teamRepository.save(
                Team.builder()
                        .name("Equipo " + tag + " " + System.nanoTime())
                        .captain(captain)
                        .tournament(tournament)
                        .build()
        );
    }

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

    private TimeSlot buildTimeSlot() {
        return timeSlotRepository.save(
                TimeSlot.builder()
                        .status(TimeSlotStatus.AVAILABLE)
                        .build()
        );
    }

    /** Field usa @Id manual — number único con nanoTime mod para evitar colisiones */
    private Field buildField() {
        Field field = new Field();
        field.setNumber((int)(Math.abs(System.nanoTime()) % 90000) + 10000);
        return fieldRepository.save(field);
    }

    /**
     * Fixture completo: Tournament + 2 capitanes + 2 equipos.
     * [0] homeCaptain, [1] awayCaptain, [2] homeTeam, [3] awayTeam, [4] tournament
     */
    private Object[] buildTeamFixture() {
        Tournament tournament  = buildTournament();
        User       homeCaptain = buildCaptain("home_" + System.nanoTime());
        User       awayCaptain = buildCaptain("away_" + System.nanoTime());
        Team       homeTeam    = buildTeam("home", homeCaptain, tournament);
        Team       awayTeam    = buildTeam("away", awayCaptain, tournament);
        return new Object[]{ homeCaptain, awayCaptain, homeTeam, awayTeam, tournament };
    }

    /**
     * Limpia en orden FK:
     * MatchResult → Match → TimeSlot → Field → Teams → Captains → Tournament
     */
    private void cleanMatchFixture(Long matchId, Long timeSlotId, Integer fieldNumber,
                                   Long homeTeamId, Long awayTeamId,
                                   Long homeCaptainId, Long awayCaptainId,
                                   Long tournamentId) {
        // Borrar result ligado al match si existe
        if (matchId != null) {
            matchRepository.findById(matchId).ifPresent(m -> {
                // Nullear FKs del match antes de borrarlo
                m.setTimeSlot(null);
                m.setField(null);
                if (m.getResult() != null) {
                    Long resultId = m.getResult().getId();
                    m.setResult(null);
                    matchRepository.save(m);
                    matchResultRepository.deleteById(resultId);
                } else {
                    matchRepository.save(m);
                }
            });
            matchRepository.deleteById(matchId);
        }
        if (timeSlotId  != null) timeSlotRepository.deleteById(timeSlotId);
        if (fieldNumber != null && fieldRepository.existsById(fieldNumber.longValue())) {
            fieldRepository.deleteById(fieldNumber.longValue());
        }
        // Nullear team.captain antes de borrar users
        if (homeTeamId != null) {
            teamRepository.findById(homeTeamId).ifPresent(t -> {
                t.setCaptain(null); teamRepository.save(t);
            });
        }
        if (awayTeamId != null) {
            teamRepository.findById(awayTeamId).ifPresent(t -> {
                t.setCaptain(null); teamRepository.save(t);
            });
        }
        if (homeTeamId    != null) teamRepository.deleteById(homeTeamId);
        if (awayTeamId    != null) teamRepository.deleteById(awayTeamId);
        if (homeCaptainId != null && userRepository.existsById(homeCaptainId))
            userRepository.deleteById(homeCaptainId);
        if (awayCaptainId != null && userRepository.existsById(awayCaptainId))
            userRepository.deleteById(awayCaptainId);
        if (tournamentId  != null) tournamentRepository.deleteById(tournamentId);
    }

    // =========================================================================
    // createMatch
    // =========================================================================

    @Nested
    @DisplayName("createMatch")
    class CreateMatchTests {

        @Test
        @DisplayName("Debe retornar 201 CREATED cuando los equipos y torneo existen")
        void createMatch_ValidRequest_Returns201() {
            Object[]   f           = buildTeamFixture();
            User       homeCaptain = (User)       f[0];
            User       awayCaptain = (User)       f[1];
            Team       homeTeam    = (Team)       f[2];
            Team       awayTeam    = (Team)       f[3];
            Tournament tournament  = (Tournament) f[4];
            Long       matchId     = null;

            try {
                CreateMatchRequestDTO request = CreateMatchRequestDTO.builder()
                        .homeTeamId(homeTeam.getId())
                        .awayTeamId(awayTeam.getId())
                        .tournamentId(tournament.getId())
                        .build();

                ResponseEntity<MatchResponseDTO> response =
                        matchController.createMatch(request);

                assertNotNull(response);
                assertEquals(201, response.getStatusCode().value());
                assertNotNull(response.getBody());
                assertNotNull(response.getBody().getId());
                matchId = response.getBody().getId();

            } finally {
                cleanMatchFixture(matchId, null, null,
                        homeTeam.getId(), awayTeam.getId(),
                        homeCaptain.getId(), awayCaptain.getId(),
                        tournament.getId());
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el equipo local no existe")
        void createMatch_HomeTeamNotFound_ThrowsException() {
            CreateMatchRequestDTO request = CreateMatchRequestDTO.builder()
                    .homeTeamId(999999L)
                    .awayTeamId(1L)
                    .tournamentId(1L)
                    .build();

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> matchController.createMatch(request));
            assertTrue(ex.getMessage().contains("Home team not found"));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el equipo visitante no existe")
        void createMatch_AwayTeamNotFound_ThrowsException() {
            Object[]   f           = buildTeamFixture();
            User       homeCaptain = (User)       f[0];
            User       awayCaptain = (User)       f[1];
            Team       homeTeam    = (Team)       f[2];
            Team       awayTeam    = (Team)       f[3];
            Tournament tournament  = (Tournament) f[4];

            try {
                CreateMatchRequestDTO request = CreateMatchRequestDTO.builder()
                        .homeTeamId(homeTeam.getId())
                        .awayTeamId(999999L)
                        .tournamentId(tournament.getId())
                        .build();

                assertThrows(RuntimeException.class,
                        () -> matchController.createMatch(request));

            } finally {
                cleanMatchFixture(null, null, null,
                        homeTeam.getId(), awayTeam.getId(),
                        homeCaptain.getId(), awayCaptain.getId(),
                        tournament.getId());
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el torneo no existe")
        void createMatch_TournamentNotFound_ThrowsException() {
            Object[]   f           = buildTeamFixture();
            User       homeCaptain = (User)       f[0];
            User       awayCaptain = (User)       f[1];
            Team       homeTeam    = (Team)       f[2];
            Team       awayTeam    = (Team)       f[3];
            Tournament tournament  = (Tournament) f[4];

            try {
                CreateMatchRequestDTO request = CreateMatchRequestDTO.builder()
                        .homeTeamId(homeTeam.getId())
                        .awayTeamId(awayTeam.getId())
                        .tournamentId(999999L)
                        .build();

                assertThrows(RuntimeException.class,
                        () -> matchController.createMatch(request));

            } finally {
                cleanMatchFixture(null, null, null,
                        homeTeam.getId(), awayTeam.getId(),
                        homeCaptain.getId(), awayCaptain.getId(),
                        tournament.getId());
            }
        }
    }

    // =========================================================================
    // getMatch
    // =========================================================================

    @Nested
    @DisplayName("getMatch")
    class GetMatchTests {

        @Test
        @DisplayName("Debe retornar 200 OK con el partido cuando existe")
        void getMatch_ExistingId_Returns200() {
            Object[]   f           = buildTeamFixture();
            User       homeCaptain = (User)       f[0];
            User       awayCaptain = (User)       f[1];
            Team       homeTeam    = (Team)       f[2];
            Team       awayTeam    = (Team)       f[3];
            Tournament tournament  = (Tournament) f[4];
            Match      match       = buildMatch(homeTeam, awayTeam, tournament);

            try {
                ResponseEntity<MatchResponseDTO> response =
                        matchController.getMatch(match.getId());

                assertNotNull(response);
                assertEquals(200, response.getStatusCode().value());
                assertNotNull(response.getBody());
                assertEquals(match.getId(), response.getBody().getId());

            } finally {
                cleanMatchFixture(match.getId(), null, null,
                        homeTeam.getId(), awayTeam.getId(),
                        homeCaptain.getId(), awayCaptain.getId(),
                        tournament.getId());
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el partido no existe")
        void getMatch_NonExistingId_ThrowsException() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> matchController.getMatch(999999L));
            assertTrue(ex.getMessage().contains("Match not found"));
        }
    }

    // =========================================================================
    // getMatchesByTournament
    // =========================================================================

    @Nested
    @DisplayName("getMatchesByTournament")
    class GetMatchesByTournamentTests {

        @Test
        @DisplayName("Debe retornar 200 OK con lista vacía cuando no hay partidos")
        void getMatchesByTournament_NoMatches_Returns200Empty() {
            ResponseEntity<List<MatchResponseDTO>> response =
                    matchController.getMatchesByTournament(999999L);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isEmpty());
        }

        @Test
        @DisplayName("Debe retornar 200 OK con partidos cuando existen para el torneo")
        void getMatchesByTournament_WithMatches_Returns200AndList() {
            Object[]   f           = buildTeamFixture();
            User       homeCaptain = (User)       f[0];
            User       awayCaptain = (User)       f[1];
            Team       homeTeam    = (Team)       f[2];
            Team       awayTeam    = (Team)       f[3];
            Tournament tournament  = (Tournament) f[4];
            Match      match       = buildMatch(homeTeam, awayTeam, tournament);

            try {
                ResponseEntity<List<MatchResponseDTO>> response =
                        matchController.getMatchesByTournament(tournament.getId());

                assertNotNull(response);
                assertEquals(200, response.getStatusCode().value());
                assertNotNull(response.getBody());
                assertFalse(response.getBody().isEmpty());

            } finally {
                cleanMatchFixture(match.getId(), null, null,
                        homeTeam.getId(), awayTeam.getId(),
                        homeCaptain.getId(), awayCaptain.getId(),
                        tournament.getId());
            }
        }
    }

    // =========================================================================
    // scheduleMatch
    // =========================================================================

    @Nested
    @DisplayName("scheduleMatch")
    class ScheduleMatchTests {

        @Test
        @DisplayName("Debe retornar 200 OK al programar partido con field y timeSlot válidos")
        void scheduleMatch_ValidRequest_Returns200() {
            Object[]   f           = buildTeamFixture();
            User       homeCaptain = (User)       f[0];
            User       awayCaptain = (User)       f[1];
            Team       homeTeam    = (Team)       f[2];
            Team       awayTeam    = (Team)       f[3];
            Tournament tournament  = (Tournament) f[4];
            Match      match       = buildMatch(homeTeam, awayTeam, tournament);
            TimeSlot   timeSlot    = buildTimeSlot();
            Field      field       = buildField();

            try {
                ScheduleMatchRequestDTO request = ScheduleMatchRequestDTO.builder()
                        .fieldId((long) field.getNumber())
                        .timeSlotId(timeSlot.getId())
                        .build();

                ResponseEntity<MatchResponseDTO> response =
                        matchController.scheduleMatch(match.getId(), request);

                assertNotNull(response);
                assertEquals(200, response.getStatusCode().value());
                assertNotNull(response.getBody());

            } finally {
                cleanMatchFixture(match.getId(), timeSlot.getId(), field.getNumber(),
                        homeTeam.getId(), awayTeam.getId(),
                        homeCaptain.getId(), awayCaptain.getId(),
                        tournament.getId());
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el partido no existe")
        void scheduleMatch_MatchNotFound_ThrowsException() {
            ScheduleMatchRequestDTO request = ScheduleMatchRequestDTO.builder()
                    .fieldId(1L)
                    .timeSlotId(1L)
                    .build();

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> matchController.scheduleMatch(999999L, request));
            assertTrue(ex.getMessage().contains("Match not found"));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el field no existe")
        void scheduleMatch_FieldNotFound_ThrowsException() {
            Object[]   f           = buildTeamFixture();
            User       homeCaptain = (User)       f[0];
            User       awayCaptain = (User)       f[1];
            Team       homeTeam    = (Team)       f[2];
            Team       awayTeam    = (Team)       f[3];
            Tournament tournament  = (Tournament) f[4];
            Match      match       = buildMatch(homeTeam, awayTeam, tournament);

            try {
                ScheduleMatchRequestDTO request = ScheduleMatchRequestDTO.builder()
                        .fieldId(999999L)
                        .timeSlotId(1L)
                        .build();

                assertThrows(RuntimeException.class,
                        () -> matchController.scheduleMatch(match.getId(), request));

            } finally {
                cleanMatchFixture(match.getId(), null, null,
                        homeTeam.getId(), awayTeam.getId(),
                        homeCaptain.getId(), awayCaptain.getId(),
                        tournament.getId());
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el timeSlot no existe")
        void scheduleMatch_TimeSlotNotFound_ThrowsException() {
            Object[]   f           = buildTeamFixture();
            User       homeCaptain = (User)       f[0];
            User       awayCaptain = (User)       f[1];
            Team       homeTeam    = (Team)       f[2];
            Team       awayTeam    = (Team)       f[3];
            Tournament tournament  = (Tournament) f[4];
            Match      match       = buildMatch(homeTeam, awayTeam, tournament);
            Field      field       = buildField();

            try {
                ScheduleMatchRequestDTO request = ScheduleMatchRequestDTO.builder()
                        .fieldId((long) field.getNumber())
                        .timeSlotId(999999L)
                        .build();

                assertThrows(RuntimeException.class,
                        () -> matchController.scheduleMatch(match.getId(), request));

            } finally {
                cleanMatchFixture(match.getId(), null, field.getNumber(),
                        homeTeam.getId(), awayTeam.getId(),
                        homeCaptain.getId(), awayCaptain.getId(),
                        tournament.getId());
            }
        }
    }

    // =========================================================================
    // recordMatchResult
    // =========================================================================

    @Nested
    @DisplayName("recordMatchResult")
    class RecordMatchResultTests {

        @Test
        @DisplayName("Debe retornar 200 OK cuando el local gana (homeScore > awayScore)")
        void recordMatchResult_HomeWins_Returns200() {
            Object[]   f           = buildTeamFixture();
            User       homeCaptain = (User)       f[0];
            User       awayCaptain = (User)       f[1];
            Team       homeTeam    = (Team)       f[2];
            Team       awayTeam    = (Team)       f[3];
            Tournament tournament  = (Tournament) f[4];
            Match      match       = buildMatch(homeTeam, awayTeam, tournament);

            // Standing necesario para standingService.updateStandingsAfterMatch
            standingRepository.save(Standing.builder()
                    .team(homeTeam).tournament(tournament)
                    .played(0).wins(0).draws(0).losses(0)
                    .goalsFor(0).goalsAgainst(0).points(0).build());
            standingRepository.save(Standing.builder()
                    .team(awayTeam).tournament(tournament)
                    .played(0).wins(0).draws(0).losses(0)
                    .goalsFor(0).goalsAgainst(0).points(0).build());

            try {
                RecordMatchResultRequestDTO request = RecordMatchResultRequestDTO.builder()
                        .homeScore(3)
                        .awayScore(1)
                        .build();

                ResponseEntity<MatchResponseDTO> response =
                        matchController.recordMatchResult(match.getId(), request);

                assertNotNull(response);
                assertEquals(200, response.getStatusCode().value());
                assertNotNull(response.getBody());

            } finally {
                standingRepository.findByTournamentId(tournament.getId())
                        .forEach(s -> standingRepository.deleteById(s.getId()));
                cleanMatchFixture(match.getId(), null, null,
                        homeTeam.getId(), awayTeam.getId(),
                        homeCaptain.getId(), awayCaptain.getId(),
                        tournament.getId());
            }
        }

        @Test
        @DisplayName("Debe retornar 200 OK cuando el visitante gana (awayScore > homeScore)")
        void recordMatchResult_AwayWins_Returns200() {
            Object[]   f           = buildTeamFixture();
            User       homeCaptain = (User)       f[0];
            User       awayCaptain = (User)       f[1];
            Team       homeTeam    = (Team)       f[2];
            Team       awayTeam    = (Team)       f[3];
            Tournament tournament  = (Tournament) f[4];
            Match      match       = buildMatch(homeTeam, awayTeam, tournament);

            standingRepository.save(Standing.builder()
                    .team(homeTeam).tournament(tournament)
                    .played(0).wins(0).draws(0).losses(0)
                    .goalsFor(0).goalsAgainst(0).points(0).build());
            standingRepository.save(Standing.builder()
                    .team(awayTeam).tournament(tournament)
                    .played(0).wins(0).draws(0).losses(0)
                    .goalsFor(0).goalsAgainst(0).points(0).build());

            try {
                RecordMatchResultRequestDTO request = RecordMatchResultRequestDTO.builder()
                        .homeScore(0)
                        .awayScore(2)
                        .build();

                ResponseEntity<MatchResponseDTO> response =
                        matchController.recordMatchResult(match.getId(), request);

                assertNotNull(response);
                assertEquals(200, response.getStatusCode().value());

            } finally {
                standingRepository.findByTournamentId(tournament.getId())
                        .forEach(s -> standingRepository.deleteById(s.getId()));
                cleanMatchFixture(match.getId(), null, null,
                        homeTeam.getId(), awayTeam.getId(),
                        homeCaptain.getId(), awayCaptain.getId(),
                        tournament.getId());
            }
        }

        @Test
        @DisplayName("Debe retornar 200 OK cuando hay empate (homeScore == awayScore, winner null)")
        void recordMatchResult_Draw_Returns200() {
            Object[]   f           = buildTeamFixture();
            User       homeCaptain = (User)       f[0];
            User       awayCaptain = (User)       f[1];
            Team       homeTeam    = (Team)       f[2];
            Team       awayTeam    = (Team)       f[3];
            Tournament tournament  = (Tournament) f[4];
            Match      match       = buildMatch(homeTeam, awayTeam, tournament);

            standingRepository.save(Standing.builder()
                    .team(homeTeam).tournament(tournament)
                    .played(0).wins(0).draws(0).losses(0)
                    .goalsFor(0).goalsAgainst(0).points(0).build());
            standingRepository.save(Standing.builder()
                    .team(awayTeam).tournament(tournament)
                    .played(0).wins(0).draws(0).losses(0)
                    .goalsFor(0).goalsAgainst(0).points(0).build());

            try {
                RecordMatchResultRequestDTO request = RecordMatchResultRequestDTO.builder()
                        .homeScore(1)
                        .awayScore(1)
                        .build();

                ResponseEntity<MatchResponseDTO> response =
                        matchController.recordMatchResult(match.getId(), request);

                assertNotNull(response);
                assertEquals(200, response.getStatusCode().value());

            } finally {
                standingRepository.findByTournamentId(tournament.getId())
                        .forEach(s -> standingRepository.deleteById(s.getId()));
                cleanMatchFixture(match.getId(), null, null,
                        homeTeam.getId(), awayTeam.getId(),
                        homeCaptain.getId(), awayCaptain.getId(),
                        tournament.getId());
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el partido no existe")
        void recordMatchResult_MatchNotFound_ThrowsException() {
            RecordMatchResultRequestDTO request = RecordMatchResultRequestDTO.builder()
                    .homeScore(2)
                    .awayScore(1)
                    .build();

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> matchController.recordMatchResult(999999L, request));
            assertTrue(ex.getMessage().contains("Match not found"));
        }
    }

    // =========================================================================
    // deleteMatch
    // =========================================================================

    @Nested
    @DisplayName("deleteMatch")
    class DeleteMatchTests {

        @Test
        @DisplayName("Debe retornar 204 NO CONTENT al eliminar un partido existente")
        void deleteMatch_ExistingId_Returns204() {
            Object[]   f           = buildTeamFixture();
            User       homeCaptain = (User)       f[0];
            User       awayCaptain = (User)       f[1];
            Team       homeTeam    = (Team)       f[2];
            Team       awayTeam    = (Team)       f[3];
            Tournament tournament  = (Tournament) f[4];
            Match      match       = buildMatch(homeTeam, awayTeam, tournament);

            try {
                ResponseEntity<Void> response =
                        matchController.deleteMatch(match.getId());

                assertNotNull(response);
                assertEquals(204, response.getStatusCode().value());
                assertFalse(matchRepository.existsById(match.getId()));

            } finally {
                // Match ya borrado; limpiar el resto
                cleanMatchFixture(null, null, null,
                        homeTeam.getId(), awayTeam.getId(),
                        homeCaptain.getId(), awayCaptain.getId(),
                        tournament.getId());
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción al eliminar un partido inexistente")
        void deleteMatch_NonExistingId_ThrowsException() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> matchController.deleteMatch(999999L));
            assertTrue(ex.getMessage().contains("Match not found"));
        }
    }
}