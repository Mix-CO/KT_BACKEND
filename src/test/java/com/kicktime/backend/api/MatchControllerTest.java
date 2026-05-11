package com.kicktime.backend.api;

import com.kicktime.backend.domain.model.dto.request.CreateMatchRequestDTO;
import com.kicktime.backend.domain.model.dto.request.RecordMatchResultRequestDTO;
import com.kicktime.backend.domain.model.dto.request.ScheduleMatchRequestDTO;
import com.kicktime.backend.domain.model.dto.response.MatchResponseDTO;
import com.kicktime.backend.domain.services.MatchService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("MatchController - Pruebas de Integración")
class MatchControllerTest {

    @Autowired
    private MatchService matchService;

    // ================================================================
    // createMatch
    // ================================================================

    @Nested
    @DisplayName("createMatch")
    class CreateMatchTests {

        @Test
        @DisplayName("Debe lanzar excepción cuando el equipo local no existe")
        void createMatch_HomeTeamNotFound_ThrowsException() {
            CreateMatchRequestDTO request = CreateMatchRequestDTO.builder()
                    .homeTeamId(999L)
                    .awayTeamId(1L)
                    .tournamentId(1L)
                    .build();

            Exception exception = assertThrows(RuntimeException.class,
                    () -> matchService.createMatch(request));

            assertTrue(exception.getMessage().contains("Home team not found"));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el equipo visitante no existe")
        void createMatch_AwayTeamNotFound_ThrowsException() {
            CreateMatchRequestDTO request = CreateMatchRequestDTO.builder()
                    .homeTeamId(999L)
                    .awayTeamId(999L)
                    .tournamentId(1L)
                    .build();

            assertThrows(RuntimeException.class,
                    () -> matchService.createMatch(request));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el torneo no existe")
        void createMatch_TournamentNotFound_ThrowsException() {
            CreateMatchRequestDTO request = CreateMatchRequestDTO.builder()
                    .homeTeamId(999L)
                    .awayTeamId(999L)
                    .tournamentId(999L)
                    .build();

            assertThrows(RuntimeException.class,
                    () -> matchService.createMatch(request));
        }
    }

    // ================================================================
    // getMatch
    // ================================================================

    @Nested
    @DisplayName("getMatch")
    class GetMatchTests {

        @Test
        @DisplayName("Debe lanzar excepción cuando el partido no existe")
        void getMatch_NonExistingId_ThrowsException() {
            Exception exception = assertThrows(RuntimeException.class,
                    () -> matchService.getMatch(999L));

            assertTrue(exception.getMessage().contains("Match not found"));
        }
    }

    // ================================================================
    // getMatchesByTournament
    // ================================================================

    @Nested
    @DisplayName("getMatchesByTournament")
    class GetMatchesByTournamentTests {

        @Test
        @DisplayName("Debe retornar lista vacía cuando el torneo no tiene partidos")
        void getMatchesByTournament_NoMatches_ReturnsEmptyList() {
            List<MatchResponseDTO> result = matchService.getMatchesByTournament(999L);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Debe retornar lista no nula para cualquier tournamentId")
        void getMatchesByTournament_AnyId_ReturnsNotNull() {
            List<MatchResponseDTO> result = matchService.getMatchesByTournament(1L);

            assertNotNull(result);
        }
    }

    // ================================================================
    // scheduleMatch
    // ================================================================

    @Nested
    @DisplayName("scheduleMatch")
    class ScheduleMatchTests {

        @Test
        @DisplayName("Debe lanzar excepción cuando el partido no existe")
        void scheduleMatch_MatchNotFound_ThrowsException() {
            ScheduleMatchRequestDTO request = ScheduleMatchRequestDTO.builder()
                    .fieldId(1L)
                    .timeSlotId(1L)
                    .build();

            Exception exception = assertThrows(RuntimeException.class,
                    () -> matchService.scheduleMatch(999L, request));

            assertTrue(exception.getMessage().contains("Match not found"));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el campo no existe")
        void scheduleMatch_FieldNotFound_ThrowsException() {
            ScheduleMatchRequestDTO request = ScheduleMatchRequestDTO.builder()
                    .fieldId(999L)
                    .timeSlotId(1L)
                    .build();

            assertThrows(RuntimeException.class,
                    () -> matchService.scheduleMatch(999L, request));
        }
    }

    // ================================================================
    // recordMatchResult
    // ================================================================

    @Nested
    @DisplayName("recordMatchResult")
    class RecordMatchResultTests {

        @Test
        @DisplayName("Debe lanzar excepción cuando el partido no existe")
        void recordMatchResult_MatchNotFound_ThrowsException() {
            RecordMatchResultRequestDTO request = RecordMatchResultRequestDTO.builder()
                    .homeScore(2)
                    .awayScore(1)
                    .build();

            Exception exception = assertThrows(RuntimeException.class,
                    () -> matchService.recordMatchResult(999L, request));

            assertTrue(exception.getMessage().contains("Match not found"));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el partido no existe al registrar empate")
        void recordMatchResult_DrawMatchNotFound_ThrowsException() {
            RecordMatchResultRequestDTO request = RecordMatchResultRequestDTO.builder()
                    .homeScore(1)
                    .awayScore(1)
                    .build();

            assertThrows(RuntimeException.class,
                    () -> matchService.recordMatchResult(999L, request));
        }
    }

    // ================================================================
    // deleteMatch
    // ================================================================

    @Nested
    @DisplayName("deleteMatch")
    class DeleteMatchTests {

        @Test
        @DisplayName("Debe lanzar excepción cuando el partido no existe")
        void deleteMatch_NonExistingId_ThrowsException() {
            Exception exception = assertThrows(RuntimeException.class,
                    () -> matchService.deleteMatch(999L));

            assertTrue(exception.getMessage().contains("Match not found"));
        }
    }
}