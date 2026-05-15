package com.kicktime.backend.api;

import com.kicktime.backend.domain.model.dto.request.CreateTournamentRequestDTO;
import com.kicktime.backend.domain.model.dto.request.UpdateTournamentStatusRequestDTO;
import com.kicktime.backend.domain.model.dto.response.TeamResponseDTO;
import com.kicktime.backend.domain.model.dto.response.TournamentResponseDTO;
import com.kicktime.backend.domain.model.enums.TournamentCategory;
import com.kicktime.backend.domain.model.enums.TournamentStatus;
import com.kicktime.backend.repository.TournamentRepository;

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
@DisplayName("TournamentController - Pruebas de Integración")
class TournamentControllerTest {

    @Autowired
    private TournamentController tournamentController;

    @Autowired
    private TournamentRepository tournamentRepository;

    // =========================================================================
    // Helper: request base válido
    // =========================================================================

    private CreateTournamentRequestDTO validRequest() {
        return CreateTournamentRequestDTO.builder()
                .name("Torneo Test " + System.nanoTime())
                .semester("2026-1")
                .category(TournamentCategory.MALE)
                .startDate(LocalDate.of(2026, 3, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .minTeams(4)
                .maxTeams(8)
                .minPlayersPerTeam(5)
                .maxPlayersPerTeam(11)
                .build();
    }

    // =========================================================================
    // createTournament
    // =========================================================================

    @Nested
    @DisplayName("createTournament")
    class CreateTournamentTests {

        @Test
        @DisplayName("Debe retornar 201 CREATED cuando los datos son válidos")
        void createTournament_ValidRequest_Returns201() {
            Long createdId = null;
            try {
                ResponseEntity<TournamentResponseDTO> response =
                        tournamentController.createTournament(validRequest());

                assertNotNull(response);
                assertEquals(201, response.getStatusCode().value());
                assertNotNull(response.getBody());
                assertNotNull(response.getBody().getId());
                assertEquals(TournamentStatus.PLANNED, response.getBody().getStatus());
                createdId = response.getBody().getId();

            } finally {
                if (createdId != null) tournamentRepository.deleteById(createdId);
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando startDate es posterior a endDate")
        void createTournament_StartAfterEnd_ThrowsException() {
            CreateTournamentRequestDTO request = validRequest();
            request.setStartDate(LocalDate.of(2026, 6, 1));
            request.setEndDate(LocalDate.of(2026, 3, 1));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> tournamentController.createTournament(request));
            assertTrue(ex.getMessage().contains("Start date cannot be after end date"));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando minTeams es mayor que maxTeams")
        void createTournament_InvalidTeamLimits_ThrowsException() {
            CreateTournamentRequestDTO request = validRequest();
            request.setMinTeams(10);
            request.setMaxTeams(4);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> tournamentController.createTournament(request));
            assertTrue(ex.getMessage().contains("Invalid team limits"));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando minPlayersPerTeam es mayor que maxPlayersPerTeam")
        void createTournament_InvalidPlayerLimits_ThrowsException() {
            CreateTournamentRequestDTO request = validRequest();
            request.setMinPlayersPerTeam(15);
            request.setMaxPlayersPerTeam(5);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> tournamentController.createTournament(request));
            assertTrue(ex.getMessage().contains("Invalid player limits"));
        }
    }

    // =========================================================================
    // getTournament
    // =========================================================================

    @Nested
    @DisplayName("getTournament")
    class GetTournamentTests {

        @Test
        @DisplayName("Debe retornar 200 OK con el torneo cuando existe")
        void getTournament_ExistingId_Returns200() {
            Long createdId = null;
            try {
                ResponseEntity<TournamentResponseDTO> created =
                        tournamentController.createTournament(validRequest());
                createdId = created.getBody().getId();

                ResponseEntity<TournamentResponseDTO> response =
                        tournamentController.getTournament(createdId);

                assertNotNull(response);
                assertEquals(200, response.getStatusCode().value());
                assertNotNull(response.getBody());
                assertEquals(createdId, response.getBody().getId());

            } finally {
                if (createdId != null) tournamentRepository.deleteById(createdId);
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el torneo no existe")
        void getTournament_NonExistingId_ThrowsException() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> tournamentController.getTournament(999999L));
            assertTrue(ex.getMessage().contains("not found"));
        }
    }

    // =========================================================================
    // getAllTournaments
    // =========================================================================

    @Nested
    @DisplayName("getAllTournaments")
    class GetAllTournamentsTests {

        @Test
        @DisplayName("Debe retornar 200 OK con lista no nula")
        void getAllTournaments_Returns200() {
            ResponseEntity<List<TournamentResponseDTO>> response =
                    tournamentController.getAllTournaments();

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("Debe incluir el torneo recién creado en la lista")
        void getAllTournaments_IncludesCreatedTournament() {
            Long createdId = null;
            try {
                ResponseEntity<TournamentResponseDTO> created =
                        tournamentController.createTournament(validRequest());
                createdId = created.getBody().getId();
                final Long id = createdId;

                ResponseEntity<List<TournamentResponseDTO>> response =
                        tournamentController.getAllTournaments();

                assertNotNull(response.getBody());
                assertFalse(response.getBody().isEmpty());
                assertTrue(response.getBody().stream()
                        .anyMatch(t -> t.getId().equals(id)));

            } finally {
                if (createdId != null) tournamentRepository.deleteById(createdId);
            }
        }
    }

    // =========================================================================
    // updateTournamentStatus
    // =========================================================================

    @Nested
    @DisplayName("updateTournamentStatus")
    class UpdateTournamentStatusTests {

        @Test
        @DisplayName("Debe retornar 200 OK con el nuevo estado cuando el torneo existe")
        void updateTournamentStatus_ValidId_Returns200() {
            Long createdId = null;
            try {
                ResponseEntity<TournamentResponseDTO> created =
                        tournamentController.createTournament(validRequest());
                createdId = created.getBody().getId();

                UpdateTournamentStatusRequestDTO request =
                        UpdateTournamentStatusRequestDTO.builder()
                                .status(TournamentStatus.REGISTRATION_OPEN)
                                .build();

                ResponseEntity<TournamentResponseDTO> response =
                        tournamentController.updateTournamentStatus(createdId, request);

                assertNotNull(response);
                assertEquals(200, response.getStatusCode().value());
                assertNotNull(response.getBody());
                assertEquals(TournamentStatus.REGISTRATION_OPEN, response.getBody().getStatus());

            } finally {
                if (createdId != null) tournamentRepository.deleteById(createdId);
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el torneo no existe")
        void updateTournamentStatus_NonExistingId_ThrowsException() {
            UpdateTournamentStatusRequestDTO request =
                    UpdateTournamentStatusRequestDTO.builder()
                            .status(TournamentStatus.ONGOING)
                            .build();

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> tournamentController.updateTournamentStatus(999999L, request));
            assertTrue(ex.getMessage().contains("not found"));
        }
    }

    // =========================================================================
    // getTeamsInTournament
    // =========================================================================

    @Nested
    @DisplayName("getTeamsInTournament")
    class GetTeamsInTournamentTests {

        @Test
        @DisplayName("Debe retornar 200 OK con lista vacía cuando el torneo no tiene equipos")
        void getTeamsInTournament_NoTeams_Returns200Empty() {
            Long createdId = null;
            try {
                ResponseEntity<TournamentResponseDTO> created =
                        tournamentController.createTournament(validRequest());
                createdId = created.getBody().getId();

                ResponseEntity<List<TeamResponseDTO>> response =
                        tournamentController.getTeamsInTournament(createdId);

                assertNotNull(response);
                assertEquals(200, response.getStatusCode().value());
                assertNotNull(response.getBody());
                assertTrue(response.getBody().isEmpty());

            } finally {
                if (createdId != null) tournamentRepository.deleteById(createdId);
            }
        }

        @Test
        @DisplayName("Debe retornar 200 OK con lista no nula para cualquier id")
        void getTeamsInTournament_AnyId_Returns200() {
            ResponseEntity<List<TeamResponseDTO>> response =
                    tournamentController.getTeamsInTournament(999999L);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            assertNotNull(response.getBody());
        }
    }

    // =========================================================================
    // deleteTournament
    // =========================================================================

    @Nested
    @DisplayName("deleteTournament")
    class DeleteTournamentTests {

        @Test
        @DisplayName("Debe retornar 204 NO CONTENT al eliminar un torneo existente")
        void deleteTournament_ExistingId_Returns204() {
            ResponseEntity<TournamentResponseDTO> created =
                    tournamentController.createTournament(validRequest());
            Long createdId = created.getBody().getId();

            // No try/finally: el propio test borra el torneo mediante el controlador
            ResponseEntity<Void> response =
                    tournamentController.deleteTournament(createdId);

            assertNotNull(response);
            assertEquals(204, response.getStatusCode().value());
            assertFalse(tournamentRepository.existsById(createdId));
        }

        @Test
        @DisplayName("Debe lanzar excepción al eliminar un torneo inexistente")
        void deleteTournament_NonExistingId_ThrowsException() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> tournamentController.deleteTournament(999999L));
            assertTrue(ex.getMessage().contains("not found"));
        }
    }
}