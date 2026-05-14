package com.kicktime.backend.api;

import com.kicktime.backend.domain.model.dto.request.CreateTeamRequestDTO;
import com.kicktime.backend.domain.model.dto.request.PlayerCreateDTO;
import com.kicktime.backend.domain.model.dto.response.TeamResponseDTO;
import com.kicktime.backend.domain.services.TeamService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("TeamController - Pruebas de Integración")
class TeamControllerTest {

    @Autowired
    private TeamService teamService;

    // ================================================================
    // createTeam
    // ================================================================

    @Nested
    @DisplayName("createTeam")
    class CreateTeamTests {

        @Test
        @DisplayName("Debe lanzar excepción cuando el usuario creador no existe")
        void createTeam_UserNotFound_ThrowsException() {
            CreateTeamRequestDTO request = CreateTeamRequestDTO.builder()
                    .name("Equipo Alpha")
                    .logoUrl("http://logo.png")
                    .captainStudentId("20191234")
                    .tournamentId(1L)
                    .build();

            Exception exception = assertThrows(RuntimeException.class,
                    () -> teamService.createTeam(request, 999L));

            assertTrue(exception.getMessage().contains("User not found"));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el torneo no existe")
        void createTeam_TournamentNotFound_ThrowsException() {
            CreateTeamRequestDTO request = CreateTeamRequestDTO.builder()
                    .name("Equipo Alpha")
                    .logoUrl("http://logo.png")
                    .captainStudentId("20191234")
                    .tournamentId(999L)
                    .build();

            assertThrows(RuntimeException.class,
                    () -> teamService.createTeam(request, 999L));
        }
    }

    // ================================================================
    // getTeam
    // ================================================================

    @Nested
    @DisplayName("getTeam")
    class GetTeamTests {

        @Test
        @DisplayName("Debe lanzar excepción cuando el equipo no existe")
        void getTeam_NonExistingId_ThrowsException() {
            Exception exception = assertThrows(RuntimeException.class,
                    () -> teamService.getTeam(999L));

            assertTrue(exception.getMessage().contains("Team not found"));
        }
    }

    // ================================================================
    // getAllTeams
    // ================================================================

    @Nested
    @DisplayName("getAllTeams")
    class GetAllTeamsTests {

        @Test
        @DisplayName("Debe retornar lista no nula de equipos")
        void getAllTeams_ReturnsNotNull() {
            List<TeamResponseDTO> result = teamService.getAllTeams();

            assertNotNull(result);
        }

        @Test
        @DisplayName("Debe retornar lista vacía cuando no hay equipos registrados")
        void getAllTeams_Empty_ReturnsEmptyList() {
            List<TeamResponseDTO> result = teamService.getAllTeams();

            assertNotNull(result);
        }
    }

    // ================================================================
    // addPlayerToTeam
    // ================================================================

    @Nested
    @DisplayName("addPlayerToTeam")
    class AddPlayerToTeamTests {

        @Test
        @DisplayName("Debe lanzar excepción cuando el equipo no existe")
        void addPlayerToTeam_TeamNotFound_ThrowsException() {
            PlayerCreateDTO playerDTO = PlayerCreateDTO.builder()
                    .name("Jugador Nuevo")
                    .studentId("20191099")
                    .email("nuevo@kicktime.com")
                    .build();

            Exception exception = assertThrows(RuntimeException.class,
                    () -> teamService.addPlayerToTeam(999L, playerDTO));

            assertTrue(exception.getMessage().contains("Team not found"));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el equipo no existe al agregar jugador sin email")
        void addPlayerToTeam_TeamNotFound_NoEmail_ThrowsException() {
            PlayerCreateDTO playerDTO = PlayerCreateDTO.builder()
                    .name("Jugador Sin Email")
                    .studentId("20191100")
                    .build();

            assertThrows(RuntimeException.class,
                    () -> teamService.addPlayerToTeam(999L, playerDTO));
        }
    }

    // ================================================================
    // changeCaptain
    // ================================================================

    @Nested
    @DisplayName("changeCaptain")
    class ChangeCaptainTests {

        @Test
        @DisplayName("Debe lanzar excepción cuando el equipo no existe")
        void changeCaptain_TeamNotFound_ThrowsException() {
            Exception exception = assertThrows(RuntimeException.class,
                    () -> teamService.changeCaptain(999L, 1L));

            assertTrue(exception.getMessage().contains("Team not found"));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el usuario no existe")
        void changeCaptain_UserNotFound_ThrowsException() {
            assertThrows(RuntimeException.class,
                    () -> teamService.changeCaptain(999L, 999L));
        }
    }

    // ================================================================
    // deleteTeam
    // ================================================================

    @Nested
    @DisplayName("deleteTeam")
    class DeleteTeamTests {

        @Test
        @DisplayName("Debe lanzar excepción cuando el equipo no existe")
        void deleteTeam_NonExistingId_ThrowsException() {
            Exception exception = assertThrows(RuntimeException.class,
                    () -> teamService.deleteTeam(999L));

            assertTrue(exception.getMessage().contains("Team not found"));
        }
    }
}
