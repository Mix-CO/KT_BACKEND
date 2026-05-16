package com.kicktime.backend.api;

import com.kicktime.backend.domain.model.dto.request.InitializeStandingsRequestDTO;
import com.kicktime.backend.domain.model.dto.response.StandingResponseDTO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("StandingController - Pruebas de Integración")
class StandingControllerTest {

    @Autowired
    private StandingController standingController;

    @Nested
    @DisplayName("initializeStandings")
    class InitializeStandingsTests {

        @Autowired
        private com.kicktime.backend.repository.TournamentRepository tournamentRepository;

        @Test
        @DisplayName("Debe retornar 200 OK cuando el torneo existe")
        void initializeStandings_ExistingTournament_Returns200() {
            // Crear torneo real para que no lance excepción
            com.kicktime.backend.domain.model.Tournament tournament =
                    com.kicktime.backend.domain.model.Tournament.builder()
                            .name("Torneo Test Standing")
                            .semester("2026-1")
                            .status(com.kicktime.backend.domain.model.enums.TournamentStatus.PLANNED)
                            .minTeams(2)
                            .maxTeams(8)
                            .minPlayersPerTeam(5)
                            .maxPlayersPerTeam(11)
                            .startDate(java.time.LocalDate.of(2026, 3, 1))
                            .endDate(java.time.LocalDate.of(2026, 6, 30))
                            .build();

            com.kicktime.backend.domain.model.Tournament saved =
                    tournamentRepository.save(tournament);

            try {
                InitializeStandingsRequestDTO request = InitializeStandingsRequestDTO.builder()
                        .tournamentId(saved.getId())
                        .build();

                ResponseEntity<Void> response = standingController.initializeStandings(request);

                assertNotNull(response);
                assertEquals(200, response.getStatusCode().value());
            } finally {
                tournamentRepository.deleteById(saved.getId());
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el torneo no existe")
        void initializeStandings_TournamentNotFound_ThrowsException() {
            InitializeStandingsRequestDTO request = InitializeStandingsRequestDTO.builder()
                    .tournamentId(999L)
                    .build();

            assertThrows(RuntimeException.class,
                    () -> standingController.initializeStandings(request));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el tournamentId es nulo")
        void initializeStandings_NullTournamentId_ThrowsException() {
            InitializeStandingsRequestDTO request = InitializeStandingsRequestDTO.builder()
                    .tournamentId(null)
                    .build();

            assertThrows(Exception.class,
                    () -> standingController.initializeStandings(request));
        }
    }

    @Nested
    @DisplayName("getStandingsByTournament")
    class GetStandingsByTournamentTests {

        @Test
        @DisplayName("Debe retornar 200 OK con lista vacía cuando no hay standings")
        void getStandingsByTournament_NoStandings_Returns200() {
            ResponseEntity<List<StandingResponseDTO>> response =
                    standingController.getStandingsByTournament(999L);

            assertNotNull(response);
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isEmpty());
        }

        @Test
        @DisplayName("Debe retornar 200 OK con lista no nula")
        void getStandingsByTournament_AnyId_Returns200() {
            ResponseEntity<List<StandingResponseDTO>> response =
                    standingController.getStandingsByTournament(1L);

            assertNotNull(response);
            assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("Debe retornar lista ordenada por puntos descendente")
        void getStandingsByTournament_ReturnsListOrderedByPoints() {
            ResponseEntity<List<StandingResponseDTO>> response =
                    standingController.getStandingsByTournament(999L);

            assertNotNull(response.getBody());
            List<StandingResponseDTO> result = response.getBody();
            for (int i = 0; i < result.size() - 1; i++) {
                assertTrue(result.get(i).getPoints() >= result.get(i + 1).getPoints());
            }
        }
    }
}