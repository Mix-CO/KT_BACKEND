package com.kicktime.backend.domain.services;

import com.kicktime.backend.domain.model.Standing;
import com.kicktime.backend.domain.model.Team;
import com.kicktime.backend.domain.model.Tournament;
import com.kicktime.backend.domain.model.User;
import com.kicktime.backend.domain.model.dto.request.CreateTeamRequestDTO;
import com.kicktime.backend.domain.model.dto.request.PlayerCreateDTO;
import com.kicktime.backend.domain.model.dto.response.TeamResponseDTO;
import com.kicktime.backend.domain.model.enums.UserRole;
import com.kicktime.backend.repository.StandingRepository;
import com.kicktime.backend.repository.TeamRepository;
import com.kicktime.backend.repository.TournamentRepository;
import com.kicktime.backend.repository.UserRepository;
import com.kicktime.backend.util.mappers.TeamMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamService - Pruebas Unitarias")
class TeamServiceTest {

    @Mock private StandingRepository standingRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private TournamentRepository tournamentRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamMapper teamMapper;
    @Mock private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private TeamService teamService;

    // ----------------------------------------------------------------
    // Datos de prueba reutilizables
    // ----------------------------------------------------------------

    private User captain;
    private Tournament tournament;
    private Team team;
    private TeamResponseDTO teamResponseDTO;
    private CreateTeamRequestDTO createRequest;

    @BeforeEach
    void setUp() {
        captain = User.builder()
                .id(1L)
                .name("Capitán KickTime")
                .email("capitan@kicktime.com")
                .role(UserRole.PLAYER)
                .team(null) // sin equipo aún
                .build();

        tournament = Tournament.builder()
                .id(1L)
                .name("Torneo ARSW 2026")
                .build();

        team = Team.builder()
                .id(1L)
                .name("Equipo Alpha")
                .logoUrl("http://logo.png")
                .captain(captain)
                .tournament(tournament)
                .build();

        teamResponseDTO = TeamResponseDTO.builder()
                .id(1L)
                .name("Equipo Alpha")
                .logoUrl("http://logo.png")
                .captainName("Capitán KickTime")
                .build();

        createRequest = CreateTeamRequestDTO.builder()
                .name("Equipo Alpha")
                .logoUrl("http://logo.png")
                .captainStudentId("20191234")
                .tournamentId(1L)
                .players(null) // sin jugadores por defecto
                .build();
    }

    // ================================================================
    // createTeam
    // ================================================================

    @Nested
    @DisplayName("createTeam")
    class CreateTeamTests {

        @Test
        @DisplayName("Debe crear equipo exitosamente sin jugadores")
        void createTeam_ValidRequestWithoutPlayers_ReturnsTeamResponseDTO() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(captain));
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
            when(teamRepository.save(any(Team.class))).thenReturn(team);
            when(userRepository.save(any(User.class))).thenReturn(captain);
            when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
            when(standingRepository.save(any(Standing.class))).thenReturn(Standing.builder().build());
            when(teamMapper.toDTO(team)).thenReturn(teamResponseDTO);

            TeamResponseDTO result = teamService.createTeam(createRequest, 1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Equipo Alpha");
            verify(teamRepository).save(any(Team.class));
            verify(standingRepository).save(any(Standing.class));
        }

        @Test
        @DisplayName("Debe crear equipo con jugadores y codificar contraseñas")
        void createTeam_WithPlayers_SavesPlayersWithEncodedPassword() {
            List<PlayerCreateDTO> players = List.of(
                    PlayerCreateDTO.builder()
                            .name("Jugador Uno")
                            .studentId("20191001")
                            .email("j1@kicktime.com")
                            .build(),
                    PlayerCreateDTO.builder()
                            .name("Jugador Dos")
                            .studentId("20191002")
                            .email("j2@kicktime.com")
                            .build()
            );

            createRequest.setPlayers(players);

            when(userRepository.findById(1L)).thenReturn(Optional.of(captain));
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
            when(teamRepository.save(any(Team.class))).thenReturn(team);
            when(userRepository.save(any(User.class))).thenReturn(captain);
            when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
            when(standingRepository.save(any(Standing.class))).thenReturn(Standing.builder().build());
            when(teamMapper.toDTO(team)).thenReturn(teamResponseDTO);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");

            TeamResponseDTO result = teamService.createTeam(createRequest, 1L);

            assertThat(result).isNotNull();
            // capitán + 2 jugadores = 3 saves de userRepository
            verify(userRepository, times(3)).save(any(User.class));
            verify(passwordEncoder, times(2)).encode(anyString());
        }

        @Test
        @DisplayName("Debe asignar rol CAPTAIN al creador del equipo")
        void createTeam_ShouldAssignCaptainRoleToCreator() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(captain));
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
            when(teamRepository.save(any(Team.class))).thenReturn(team);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User saved = invocation.getArgument(0);
                if (saved.getRole() == UserRole.CAPTAIN) {
                    assertThat(saved.getStudentId()).isEqualTo("20191234");
                }
                return saved;
            });
            when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
            when(standingRepository.save(any(Standing.class))).thenReturn(Standing.builder().build());
            when(teamMapper.toDTO(team)).thenReturn(teamResponseDTO);

            teamService.createTeam(createRequest, 1L);

            verify(userRepository, atLeastOnce()).save(any(User.class));
        }

        @Test
        @DisplayName("Debe inicializar standing en cero al crear el equipo")
        void createTeam_ShouldInitializeStandingWithZeros() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(captain));
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
            when(teamRepository.save(any(Team.class))).thenReturn(team);
            when(userRepository.save(any(User.class))).thenReturn(captain);
            when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
            when(standingRepository.save(any(Standing.class))).thenAnswer(invocation -> {
                Standing saved = invocation.getArgument(0);
                assertThat(saved.getPlayed()).isZero();
                assertThat(saved.getWins()).isZero();
                assertThat(saved.getDraws()).isZero();
                assertThat(saved.getLosses()).isZero();
                assertThat(saved.getGoalsFor()).isZero();
                assertThat(saved.getGoalsAgainst()).isZero();
                assertThat(saved.getPoints()).isZero();
                return saved;
            });
            when(teamMapper.toDTO(team)).thenReturn(teamResponseDTO);

            teamService.createTeam(createRequest, 1L);

            verify(standingRepository).save(any(Standing.class));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el usuario no existe")
        void createTeam_UserNotFound_ThrowsException() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> teamService.createTeam(createRequest, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");

            verify(teamRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el usuario ya pertenece a un equipo")
        void createTeam_UserAlreadyInTeam_ThrowsException() {
            captain.setTeam(team); // ya tiene equipo

            when(userRepository.findById(1L)).thenReturn(Optional.of(captain));

            assertThatThrownBy(() -> teamService.createTeam(createRequest, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User already belongs to a team");

            verify(teamRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el torneo no existe")
        void createTeam_TournamentNotFound_ThrowsException() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(captain));
            when(tournamentRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> teamService.createTeam(createRequest, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Tournament not found");

            verify(teamRepository, never()).save(any());
        }
    }

    // ================================================================
    // getTeam
    // ================================================================

    @Nested
    @DisplayName("getTeam")
    class GetTeamTests {

        @Test
        @DisplayName("Debe retornar el equipo cuando existe el ID")
        void getTeam_ExistingId_ReturnsTeamResponseDTO() {
            when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
            when(teamMapper.toDTO(team)).thenReturn(teamResponseDTO);

            TeamResponseDTO result = teamService.getTeam(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Equipo Alpha");
            verify(teamRepository).findById(1L);
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando no existe el ID")
        void getTeam_NonExistingId_ThrowsException() {
            when(teamRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> teamService.getTeam(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Team not found");

            verify(teamMapper, never()).toDTO(any());
        }
    }

    // ================================================================
    // getAllTeams
    // ================================================================

    @Nested
    @DisplayName("getAllTeams")
    class GetAllTeamsTests {

        @Test
        @DisplayName("Debe retornar lista de equipos cuando existen registros")
        void getAllTeams_WithData_ReturnsList() {
            when(teamRepository.findAll()).thenReturn(List.of(team));
            when(teamMapper.toDTOList(List.of(team))).thenReturn(List.of(teamResponseDTO));

            List<TeamResponseDTO> result = teamService.getAllTeams();

            assertThat(result).isNotNull().hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Equipo Alpha");
            verify(teamRepository).findAll();
        }

        @Test
        @DisplayName("Debe retornar lista vacía cuando no hay equipos")
        void getAllTeams_Empty_ReturnsEmptyList() {
            when(teamRepository.findAll()).thenReturn(List.of());
            when(teamMapper.toDTOList(List.of())).thenReturn(List.of());

            List<TeamResponseDTO> result = teamService.getAllTeams();

            assertThat(result).isNotNull().isEmpty();
            verify(teamRepository).findAll();
        }
    }

    // ================================================================
    // getTeamsByTournament
    // ================================================================

    @Nested
    @DisplayName("getTeamsByTournament")
    class GetTeamsByTournamentTests {

        @Test
        @DisplayName("Debe retornar equipos de un torneo específico")
        void getTeamsByTournament_WithData_ReturnsList() {
            when(teamRepository.findByTournamentId(1L)).thenReturn(List.of(team));
            when(teamMapper.toDTOList(List.of(team))).thenReturn(List.of(teamResponseDTO));

            List<TeamResponseDTO> result = teamService.getTeamsByTournament(1L);

            assertThat(result).isNotNull().hasSize(1);
            verify(teamRepository).findByTournamentId(1L);
        }

        @Test
        @DisplayName("Debe retornar lista vacía cuando el torneo no tiene equipos")
        void getTeamsByTournament_Empty_ReturnsEmptyList() {
            when(teamRepository.findByTournamentId(1L)).thenReturn(List.of());
            when(teamMapper.toDTOList(List.of())).thenReturn(List.of());

            List<TeamResponseDTO> result = teamService.getTeamsByTournament(1L);

            assertThat(result).isNotNull().isEmpty();
            verify(teamRepository).findByTournamentId(1L);
        }
    }

    // ================================================================
    // addPlayerToTeam
    // ================================================================

    @Nested
    @DisplayName("addPlayerToTeam")
    class AddPlayerToTeamTests {

        @Test
        @DisplayName("Debe agregar jugador al equipo exitosamente")
        void addPlayerToTeam_ValidRequest_ReturnsUpdatedTeam() {
            PlayerCreateDTO playerDTO = PlayerCreateDTO.builder()
                    .name("Jugador Nuevo")
                    .studentId("20191099")
                    .email("nuevo@kicktime.com")
                    .build();

            when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
            when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
            when(userRepository.save(any(User.class))).thenReturn(User.builder().build());
            when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
            when(teamMapper.toDTO(team)).thenReturn(teamResponseDTO);

            TeamResponseDTO result = teamService.addPlayerToTeam(1L, playerDTO);

            assertThat(result).isNotNull();
            verify(userRepository).save(any(User.class));
            verify(passwordEncoder).encode(anyString());
        }

        @Test
        @DisplayName("Debe asignar rol PLAYER al jugador agregado")
        void addPlayerToTeam_ShouldAssignPlayerRole() {
            PlayerCreateDTO playerDTO = PlayerCreateDTO.builder()
                    .name("Jugador Nuevo")
                    .studentId("20191099")
                    .email("nuevo@kicktime.com")
                    .build();

            when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
            when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User saved = invocation.getArgument(0);
                assertThat(saved.getRole()).isEqualTo(UserRole.PLAYER);
                return saved;
            });
            when(teamMapper.toDTO(team)).thenReturn(teamResponseDTO);

            teamService.addPlayerToTeam(1L, playerDTO);

            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el equipo no existe")
        void addPlayerToTeam_TeamNotFound_ThrowsException() {
            PlayerCreateDTO playerDTO = PlayerCreateDTO.builder()
                    .name("Jugador Nuevo")
                    .studentId("20191099")
                    .email("nuevo@kicktime.com")
                    .build();

            when(teamRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> teamService.addPlayerToTeam(99L, playerDTO))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Team not found");

            verify(userRepository, never()).save(any());
        }
    }

    // ================================================================
    // changeCaptain
    // ================================================================

    @Nested
    @DisplayName("changeCaptain")
    class ChangeCaptainTests {

        @Test
        @DisplayName("Debe cambiar el capitán exitosamente cuando el usuario pertenece al equipo")
        void changeCaptain_ValidUser_ReturnsUpdatedTeam() {
            User newCaptain = User.builder()
                    .id(2L)
                    .name("Nuevo Capitán")
                    .email("nuevo.capitan@kicktime.com")
                    .role(UserRole.PLAYER)
                    .team(team)
                    .build();

            TeamResponseDTO updatedResponse = TeamResponseDTO.builder()
                    .id(1L)
                    .name("Equipo Alpha")
                    .captainName("Nuevo Capitán")
                    .build();

            when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
            when(userRepository.findById(2L)).thenReturn(Optional.of(newCaptain));
            when(teamRepository.save(any(Team.class))).thenReturn(team);
            when(teamMapper.toDTO(team)).thenReturn(updatedResponse);

            TeamResponseDTO result = teamService.changeCaptain(1L, 2L);

            assertThat(result).isNotNull();
            assertThat(result.getCaptainName()).isEqualTo("Nuevo Capitán");
            verify(teamRepository).save(any(Team.class));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el equipo no existe")
        void changeCaptain_TeamNotFound_ThrowsException() {
            when(teamRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> teamService.changeCaptain(99L, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Team not found");

            verify(teamRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el usuario no existe")
        void changeCaptain_UserNotFound_ThrowsException() {
            when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> teamService.changeCaptain(1L, 99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");

            verify(teamRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el usuario no pertenece al equipo")
        void changeCaptain_UserNotInTeam_ThrowsException() {
            User outsider = User.builder()
                    .id(3L)
                    .name("Foráneo")
                    .email("foraneo@kicktime.com")
                    .role(UserRole.PLAYER)
                    .team(null) // no pertenece al equipo
                    .build();

            when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
            when(userRepository.findById(3L)).thenReturn(Optional.of(outsider));

            assertThatThrownBy(() -> teamService.changeCaptain(1L, 3L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User is not part of this team");

            verify(teamRepository, never()).save(any());
        }
    }

    // ================================================================
    // deleteTeam
    // ================================================================

    @Nested
    @DisplayName("deleteTeam")
    class DeleteTeamTests {

        @Test
        @DisplayName("Debe eliminar el equipo exitosamente cuando existe")
        void deleteTeam_ExistingId_DeletesSuccessfully() {
            when(teamRepository.existsById(1L)).thenReturn(true);
            doNothing().when(teamRepository).deleteById(1L);

            assertThatCode(() -> teamService.deleteTeam(1L))
                    .doesNotThrowAnyException();

            verify(teamRepository).existsById(1L);
            verify(teamRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el equipo no existe al eliminar")
        void deleteTeam_NonExistingId_ThrowsException() {
            when(teamRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> teamService.deleteTeam(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Team not found");

            verify(teamRepository, never()).deleteById(any());
        }
    }
}
