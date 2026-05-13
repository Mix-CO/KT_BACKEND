package com.kicktime.backend.domain.services;

import com.kicktime.backend.domain.model.Team;
import com.kicktime.backend.domain.model.Tournament;
import com.kicktime.backend.domain.model.dto.request.CreateTournamentRequestDTO;
import com.kicktime.backend.domain.model.dto.request.UpdateTournamentStatusRequestDTO;
import com.kicktime.backend.domain.model.dto.response.TeamResponseDTO;
import com.kicktime.backend.domain.model.dto.response.TournamentResponseDTO;
import com.kicktime.backend.domain.model.enums.TournamentCategory;
import com.kicktime.backend.domain.model.enums.TournamentStatus;
import com.kicktime.backend.repository.TeamRepository;
import com.kicktime.backend.repository.TournamentRepository;
import com.kicktime.backend.util.mappers.TeamMapper;
import com.kicktime.backend.util.mappers.TournamentMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TournamentService - Pruebas Unitarias")
class TournamentServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TournamentMapper tournamentMapper;

    @Mock
    private TeamMapper teamMapper;

    @InjectMocks
    private TournamentService tournamentService;

    // ----------------------------------------------------------------
    // Datos de prueba reutilizables
    // ----------------------------------------------------------------

    private Tournament tournament;
    private TournamentResponseDTO tournamentResponseDTO;
    private CreateTournamentRequestDTO createRequest;

    @BeforeEach
    void setUp() {
        tournament = Tournament.builder()
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

        tournamentResponseDTO = TournamentResponseDTO.builder()
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
    // createTournament
    // ================================================================

    @Nested
    @DisplayName("createTournament")
    class CreateTournamentTests {

        @Test
        @DisplayName("Debe crear un torneo exitosamente con datos válidos")
        void createTournament_ValidRequest_ReturnsTournamentResponseDTO() {
            when(tournamentRepository.save(any(Tournament.class))).thenReturn(tournament);
            when(tournamentMapper.toDTO(tournament)).thenReturn(tournamentResponseDTO);

            TournamentResponseDTO result = tournamentService.createTournament(createRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Torneo ARSW 2026");
            assertThat(result.getStatus()).isEqualTo(TournamentStatus.PLANNED);

            verify(tournamentRepository, times(1)).save(any(Tournament.class));
            verify(tournamentMapper, times(1)).toDTO(tournament);
        }

        @Test
        @DisplayName("Debe asignar estado PLANNED automáticamente al crear")
        void createTournament_ShouldAssignPlannedStatusAutomatically() {
            when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> {
                Tournament saved = invocation.getArgument(0);
                assertThat(saved.getStatus()).isEqualTo(TournamentStatus.PLANNED);
                return tournament;
            });
            when(tournamentMapper.toDTO(any())).thenReturn(tournamentResponseDTO);

            tournamentService.createTournament(createRequest);

            verify(tournamentRepository).save(any(Tournament.class));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando startDate es posterior a endDate")
        void createTournament_StartDateAfterEndDate_ThrowsException() {
            CreateTournamentRequestDTO badRequest = CreateTournamentRequestDTO.builder()
                    .name("Torneo inválido")
                    .semester("2026-1")
                    .category(TournamentCategory.MALE)
                    .startDate(LocalDate.of(2026, 8, 1))
                    .endDate(LocalDate.of(2026, 3, 1))
                    .minTeams(4)
                    .maxTeams(8)
                    .minPlayersPerTeam(5)
                    .maxPlayersPerTeam(11)
                    .build();

            assertThatThrownBy(() -> tournamentService.createTournament(badRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Start date cannot be after end date");

            verify(tournamentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando minTeams es mayor que maxTeams")
        void createTournament_InvalidTeamLimits_ThrowsException() {
            CreateTournamentRequestDTO badRequest = CreateTournamentRequestDTO.builder()
                    .name("Torneo inválido")
                    .semester("2026-1")
                    .category(TournamentCategory.MALE)
                    .startDate(LocalDate.of(2026, 3, 1))
                    .endDate(LocalDate.of(2026, 6, 30))
                    .minTeams(10)
                    .maxTeams(4)
                    .minPlayersPerTeam(5)
                    .maxPlayersPerTeam(11)
                    .build();

            assertThatThrownBy(() -> tournamentService.createTournament(badRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Invalid team limits");

            verify(tournamentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando minPlayersPerTeam es mayor que maxPlayersPerTeam")
        void createTournament_InvalidPlayerLimits_ThrowsException() {
            CreateTournamentRequestDTO badRequest = CreateTournamentRequestDTO.builder()
                    .name("Torneo inválido")
                    .semester("2026-1")
                    .category(TournamentCategory.MALE)
                    .startDate(LocalDate.of(2026, 3, 1))
                    .endDate(LocalDate.of(2026, 6, 30))
                    .minTeams(4)
                    .maxTeams(8)
                    .minPlayersPerTeam(15)
                    .maxPlayersPerTeam(5)
                    .build();

            assertThatThrownBy(() -> tournamentService.createTournament(badRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Invalid player limits");

            verify(tournamentRepository, never()).save(any());
        }
    }

    // ================================================================
    // getTournament
    // ================================================================

    @Nested
    @DisplayName("getTournament")
    class GetTournamentTests {

        @Test
        @DisplayName("Debe retornar el torneo cuando existe el ID")
        void getTournament_ExistingId_ReturnsTournamentResponseDTO() {
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
            when(tournamentMapper.toDTO(tournament)).thenReturn(tournamentResponseDTO);

            TournamentResponseDTO result = tournamentService.getTournament(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Torneo ARSW 2026");
            verify(tournamentRepository).findById(1L);
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando no existe el ID")
        void getTournament_NonExistingId_ThrowsException() {
            when(tournamentRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tournamentService.getTournament(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Tournament not found");

            verify(tournamentMapper, never()).toDTO(any());
        }
    }

    // ================================================================
    // getAllTournaments
    // ================================================================

    @Nested
    @DisplayName("getAllTournaments")
    class GetAllTournamentsTests {

        @Test
        @DisplayName("Debe retornar lista de torneos cuando existen registros")
        void getAllTournaments_WithData_ReturnsList() {
            List<Tournament> tournaments = List.of(tournament);
            List<TournamentResponseDTO> responseDTOs = List.of(tournamentResponseDTO);

            when(tournamentRepository.findAll()).thenReturn(tournaments);
            when(tournamentMapper.toDTOList(tournaments)).thenReturn(responseDTOs);

            List<TournamentResponseDTO> result = tournamentService.getAllTournaments();

            assertThat(result).isNotNull().hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Torneo ARSW 2026");
            verify(tournamentRepository).findAll();
        }

        @Test
        @DisplayName("Debe retornar lista vacía cuando no hay torneos")
        void getAllTournaments_Empty_ReturnsEmptyList() {
            when(tournamentRepository.findAll()).thenReturn(List.of());
            when(tournamentMapper.toDTOList(List.of())).thenReturn(List.of());

            List<TournamentResponseDTO> result = tournamentService.getAllTournaments();

            assertThat(result).isNotNull().isEmpty();
            verify(tournamentRepository).findAll();
        }
    }

    // ================================================================
    // updateTournamentStatus
    // ================================================================

    @Nested
    @DisplayName("updateTournamentStatus")
    class UpdateTournamentStatusTests {

        @Test
        @DisplayName("Debe actualizar el estado a REGISTRATION_OPEN exitosamente")
        void updateTournamentStatus_ToRegistrationOpen_ReturnsUpdatedDTO() {
            UpdateTournamentStatusRequestDTO request = UpdateTournamentStatusRequestDTO.builder()
                    .status(TournamentStatus.REGISTRATION_OPEN)
                    .build();

            Tournament updatedTournament = Tournament.builder()
                    .id(1L)
                    .name("Torneo ARSW 2026")
                    .status(TournamentStatus.REGISTRATION_OPEN)
                    .build();

            TournamentResponseDTO updatedResponse = TournamentResponseDTO.builder()
                    .id(1L)
                    .name("Torneo ARSW 2026")
                    .status(TournamentStatus.REGISTRATION_OPEN)
                    .build();

            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
            when(tournamentRepository.save(any(Tournament.class))).thenReturn(updatedTournament);
            when(tournamentMapper.toDTO(updatedTournament)).thenReturn(updatedResponse);

            TournamentResponseDTO result = tournamentService.updateTournamentStatus(1L, request);

            assertThat(result.getStatus()).isEqualTo(TournamentStatus.REGISTRATION_OPEN);
            verify(tournamentRepository).findById(1L);
            verify(tournamentRepository).save(any(Tournament.class));
        }

        @Test
        @DisplayName("Debe actualizar el estado a ONGOING exitosamente")
        void updateTournamentStatus_ToOngoing_ReturnsUpdatedDTO() {
            UpdateTournamentStatusRequestDTO request = UpdateTournamentStatusRequestDTO.builder()
                    .status(TournamentStatus.ONGOING)
                    .build();

            Tournament updatedTournament = Tournament.builder()
                    .id(1L)
                    .name("Torneo ARSW 2026")
                    .status(TournamentStatus.ONGOING)
                    .build();

            TournamentResponseDTO updatedResponse = TournamentResponseDTO.builder()
                    .id(1L)
                    .status(TournamentStatus.ONGOING)
                    .build();

            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
            when(tournamentRepository.save(any(Tournament.class))).thenReturn(updatedTournament);
            when(tournamentMapper.toDTO(updatedTournament)).thenReturn(updatedResponse);

            TournamentResponseDTO result = tournamentService.updateTournamentStatus(1L, request);

            assertThat(result.getStatus()).isEqualTo(TournamentStatus.ONGOING);
        }

        @Test
        @DisplayName("Debe lanzar excepción si el torneo no existe al actualizar")
        void updateTournamentStatus_NonExistingId_ThrowsException() {
            UpdateTournamentStatusRequestDTO request = UpdateTournamentStatusRequestDTO.builder()
                    .status(TournamentStatus.ONGOING)
                    .build();

            when(tournamentRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tournamentService.updateTournamentStatus(99L, request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Tournament not found");

            verify(tournamentRepository, never()).save(any());
        }
    }

    // ================================================================
    // deleteTournament
    // ================================================================

    @Nested
    @DisplayName("deleteTournament")
    class DeleteTournamentTests {

        @Test
        @DisplayName("Debe eliminar el torneo exitosamente cuando existe")
        void deleteTournament_ExistingId_DeletesSuccessfully() {
            when(tournamentRepository.existsById(1L)).thenReturn(true);
            doNothing().when(tournamentRepository).deleteById(1L);

            assertThatCode(() -> tournamentService.deleteTournament(1L))
                    .doesNotThrowAnyException();

            verify(tournamentRepository).existsById(1L);
            verify(tournamentRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el torneo no existe al eliminar")
        void deleteTournament_NonExistingId_ThrowsException() {
            when(tournamentRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> tournamentService.deleteTournament(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Tournament not found");

            verify(tournamentRepository, never()).deleteById(any());
        }
    }

    // ================================================================
    // getTeamsInTournament
    // ================================================================

    @Nested
    @DisplayName("getTeamsInTournament")
    class GetTeamsInTournamentTests {

        @Test
        @DisplayName("Debe retornar lista de equipos del torneo")
        void getTeamsInTournament_ExistingTournament_ReturnsTeamList() {
            Team team = Team.builder()
                    .id(1L)
                    .name("Equipo Alpha")
                    .logoUrl("http://logo.png")
                    .build();

            TeamResponseDTO teamDTO = TeamResponseDTO.builder()
                    .id(1L)
                    .name("Equipo Alpha")
                    .logoUrl("http://logo.png")
                    .build();

            when(teamRepository.findByTournamentId(1L)).thenReturn(List.of(team));
            when(teamMapper.toDTOList(List.of(team))).thenReturn(List.of(teamDTO));

            List<TeamResponseDTO> result = tournamentService.getTeamsInTournament(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Equipo Alpha");
            assertThat(result.get(0).getLogoUrl()).isEqualTo("http://logo.png");
            verify(teamRepository).findByTournamentId(1L);
        }

        @Test
        @DisplayName("Debe retornar lista vacía si el torneo no tiene equipos")
        void getTeamsInTournament_NoTeams_ReturnsEmptyList() {
            when(teamRepository.findByTournamentId(1L)).thenReturn(List.of());
            when(teamMapper.toDTOList(List.of())).thenReturn(List.of());

            List<TeamResponseDTO> result = tournamentService.getTeamsInTournament(1L);

            assertThat(result).isNotNull().isEmpty();
            verify(teamRepository).findByTournamentId(1L);
        }
    }
}