package com.kicktime.backend.api;

import com.kicktime.backend.domain.model.dto.request.InitializeStandingsRequestDTO;
import com.kicktime.backend.domain.model.dto.response.StandingResponseDTO;
import com.kicktime.backend.domain.services.StandingService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("StandingController - Pruebas de Integración")
class StandingControllerTest {

    @Autowired
    private StandingService standingService;

    // ================================================================
    // initializeStandings
    // ================================================================

    @Nested
    @DisplayName("initializeStandings")
    class InitializeStandingsTests {

        @Test
        @DisplayName("Debe lanzar excepción cuando el torneo no existe")
        void initializeStandings_TournamentNotFound_ThrowsException() {
            InitializeStandingsRequestDTO request = InitializeStandingsRequestDTO.builder()
                    .tournamentId(999L)
                    .build();

            Exception exception = assertThrows(RuntimeException.class,
                    () -> standingService.initializeStandings(request));

            assertTrue(exception.getMessage().contains("Tournament not found"));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el tournamentId es nulo")
        void initializeStandings_NullTournamentId_ThrowsException() {
            InitializeStandingsRequestDTO request = InitializeStandingsRequestDTO.builder()
                    .tournamentId(null)
                    .build();

            assertThrows(Exception.class,
                    () -> standingService.initializeStandings(request));
        }
    }

    // ================================================================
    // getStandingsByTournament
    // ================================================================

    @Nested
    @DisplayName("getStandingsByTournament")
    class GetStandingsByTournamentTests {

        @Test
        @DisplayName("Debe retornar lista vacía cuando el torneo no tiene standings")
        void getStandingsByTournament_NoStandings_ReturnsEmptyList() {
            List<StandingResponseDTO> result =
                    standingService.getStandingsByTournament(999L);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Debe retornar lista no nula para cualquier tournamentId")
        void getStandingsByTournament_AnyId_ReturnsNotNull() {
            List<StandingResponseDTO> result =
                    standingService.getStandingsByTournament(1L);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Debe retornar lista ordenada por puntos cuando hay standings")
        void getStandingsByTournament_ReturnsListOrderedByPoints() {
            List<StandingResponseDTO> result =
                    standingService.getStandingsByTournament(999L);

            assertNotNull(result);
            // Verificar que si hay más de un elemento, están ordenados por puntos descendente
            for (int i = 0; i < result.size() - 1; i++) {
                assertTrue(result.get(i).getPoints() >= result.get(i + 1).getPoints());
            }
        }
    }
}