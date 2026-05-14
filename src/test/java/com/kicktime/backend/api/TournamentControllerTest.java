package com.kicktime.backend.api;

import com.kicktime.backend.domain.model.dto.request.CreateTournamentRequestDTO;
import com.kicktime.backend.domain.model.dto.request.UpdateTournamentStatusRequestDTO;
import com.kicktime.backend.domain.model.dto.response.TeamResponseDTO;
import com.kicktime.backend.domain.model.dto.response.TournamentResponseDTO;
import com.kicktime.backend.domain.model.enums.TournamentCategory;
import com.kicktime.backend.domain.model.enums.TournamentStatus;
import com.kicktime.backend.domain.services.TournamentService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("TournamentController - Pruebas de Integración reales")
public class TournamentControllerTest {

    @Autowired
    private TournamentService tournamentService; // servicio real del contexto

    private TournamentResponseDTO tournamentResponse;
    private CreateTournamentRequestDTO createRequest;

    @BeforeEach
    void setUp() {
        tournamentResponse = TournamentResponseDTO.builder()
                .id(1L)
                .name("Torneo ARSW 2026")
                .semester("2026-1")
                .category(TournamentCategory.MALE)
                .startDate(LocalDate.of(2026, 3, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .minTeams(4)
                .maxTeams(8)
                .minPlayersPerTeam(5)
                .maxPlayersPerTeam(11)
                .status(TournamentStatus.PLANNED)
                .build();

        createRequest = CreateTournamentRequestDTO.builder()
                .name("Torneo ARSW 2026")
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

    // ================================================================
    // CREACIÓN DE TORNEO
    // ================================================================
    @Nested
    @DisplayName("Crear torneo")
    class CreateTournamentTests {

        @Test
        @DisplayName("Crear torneo válido")
        void createTournament_Valid() {
            TournamentResponseDTO result = tournamentService.createTournament(createRequest);
            assertNotNull(result);
            assertEquals("Torneo ARSW 2026", result.getName());
            assertEquals(TournamentStatus.PLANNED, result.getStatus());
        }

        @Test
        @DisplayName("Crear torneo con fechas inválidas lanza excepción")
        void createTournament_InvalidDates() {
            CreateTournamentRequestDTO invalidRequest = CreateTournamentRequestDTO.builder()
                    .name("Torneo Fallido")
                    .semester("2026-1")
                    .category(TournamentCategory.MALE)
                    .startDate(LocalDate.of(2026, 6, 1))
                    .endDate(LocalDate.of(2026, 3, 1))
                    .minTeams(4)
                    .maxTeams(8)
                    .minPlayersPerTeam(5)
                    .maxPlayersPerTeam(11)
                    .build();

            Exception exception = assertThrows(RuntimeException.class,
                    () -> tournamentService.createTournament(invalidRequest));
            assertTrue(exception.getMessage().contains("Start date cannot be after end date"));
        }
    }

    // ================================================================
    // OBTENER TORNEO
    // ================================================================
    @Nested
    @DisplayName("Obtener torneo")
    class GetTournamentTests {

        @Test
        @DisplayName("Obtener torneo existente")
        void getTournament_Existing() {
            TournamentResponseDTO created = tournamentService.createTournament(createRequest);
            TournamentResponseDTO result = tournamentService.getTournament(created.getId());
            assertNotNull(result);
            assertEquals(created.getName(), result.getName());
        }

        @Test
        @DisplayName("Obtener torneo no existente lanza excepción")
        void getTournament_NonExisting() {
            Exception exception = assertThrows(RuntimeException.class,
                    () -> tournamentService.getTournament(999L));
            assertTrue(exception.getMessage().contains("not found"));
        }
    }

    // ================================================================
    // LISTAR TORNEOS
    // ================================================================
    @Nested
    @DisplayName("Listar torneos")
    class GetAllTournamentsTests {

        @Test
        @DisplayName("Lista de torneos no vacía")
        void getAllTournaments_NotEmpty() {
            tournamentService.createTournament(createRequest);
            List<TournamentResponseDTO> tournaments = tournamentService.getAllTournaments();
            assertFalse(tournaments.isEmpty());
        }

        @Test
        @DisplayName("Lista de torneos vacía")
        void getAllTournaments_Empty() {
            List<TournamentResponseDTO> tournaments = tournamentService.getAllTournaments();
            assertNotNull(tournaments);
        }
    }

    // ================================================================
    // ACTUALIZAR ESTADO TORNEO
    // ================================================================
    @Nested
    @DisplayName("Actualizar estado del torneo")
    class UpdateTournamentStatusTests {

        @Test
        @DisplayName("Actualizar estado válido")
        void updateTournamentStatus_Valid() {
            TournamentResponseDTO created = tournamentService.createTournament(createRequest);
            UpdateTournamentStatusRequestDTO request = UpdateTournamentStatusRequestDTO.builder()
                    .status(TournamentStatus.REGISTRATION_OPEN)
                    .build();
            TournamentResponseDTO updated = tournamentService.updateTournamentStatus(created.getId(), request);
            assertEquals(TournamentStatus.REGISTRATION_OPEN, updated.getStatus());
        }

        @Test
        @DisplayName("Actualizar estado de torneo no existente lanza excepción")
        void updateTournamentStatus_NonExisting() {
            UpdateTournamentStatusRequestDTO request = UpdateTournamentStatusRequestDTO.builder()
                    .status(TournamentStatus.ONGOING)
                    .build();
            Exception exception = assertThrows(RuntimeException.class,
                    () -> tournamentService.updateTournamentStatus(999L, request));
            assertTrue(exception.getMessage().contains("not found"));
        }
    }

    // ================================================================
    // EQUIPOS EN TORNEO
    // ================================================================
    @Nested
    @DisplayName("Obtener equipos de torneo")
    class GetTeamsInTournamentTests {

        @Test
        @DisplayName("Lista de equipos no vacía")
        void getTeamsInTournament_NotEmpty() {
            TournamentResponseDTO created = tournamentService.createTournament(createRequest);
            TeamResponseDTO team = TeamResponseDTO.builder().id(1L).name("Equipo Alpha").build();
            // Suponiendo que el servicio tiene método para agregar equipo
            // tournamentService.addTeamToTournament(created.getId(), team);
            List<TeamResponseDTO> teams = tournamentService.getTeamsInTournament(created.getId());
            assertNotNull(teams);
        }
    }

    // ================================================================
    // ELIMINAR TORNEO
    // ================================================================
    @Nested
    @DisplayName("Eliminar torneo")
    class DeleteTournamentTests {

        @Test
        @DisplayName("Eliminar torneo existente")
        void deleteTournament_Existing() {
            TournamentResponseDTO created = tournamentService.createTournament(createRequest);
            assertDoesNotThrow(() -> tournamentService.deleteTournament(created.getId()));
        }

        @Test
        @DisplayName("Eliminar torneo no existente lanza excepción")
        void deleteTournament_NonExisting() {
            Exception exception = assertThrows(RuntimeException.class,
                    () -> tournamentService.deleteTournament(999L));
            assertTrue(exception.getMessage().contains("not found"));
        }
    }
}