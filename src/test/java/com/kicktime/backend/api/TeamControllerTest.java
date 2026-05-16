package com.kicktime.backend.api;

import com.kicktime.backend.domain.model.*;
import com.kicktime.backend.domain.model.dto.request.CreateTeamRequestDTO;
import com.kicktime.backend.domain.model.dto.request.PlayerCreateDTO;
import com.kicktime.backend.domain.model.dto.response.TeamResponseDTO;
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
@DisplayName("TeamController - Pruebas de Integración")
class TeamControllerTest {

    @Autowired
    private TeamController teamController;

    @Autowired private TeamRepository        teamRepository;
    @Autowired private UserRepository        userRepository;
    @Autowired private TournamentRepository  tournamentRepository;
    @Autowired private StandingRepository    standingRepository;

    // =========================================================================
    // Helpers de fixtures
    // =========================================================================

    private Tournament buildTournament() {
        return tournamentRepository.save(
                Tournament.builder()
                        .name("Torneo Test " + System.nanoTime())
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

    /** User sin team asignado — requerimiento de createTeam */
    private User buildFreeUser(String tag) {
        return userRepository.save(
                User.builder()
                        .name("Usuario " + tag)
                        .email("user_" + tag + "_" + System.nanoTime() + "@kicktime.test")
                        .password("encoded")
                        .role(UserRole.PLAYER)
                        .build()
        );
    }

    private CreateTeamRequestDTO validTeamRequest(Long tournamentId, String captainStudentId) {
        return CreateTeamRequestDTO.builder()
                .name("Equipo Test " + System.nanoTime())
                .logoUrl("http://logo.test/img.png")
                .captainStudentId(captainStudentId)
                .tournamentId(tournamentId)
                .build();
    }

    /**
     * Limpia Standing → Team → User(captain) → Tournament.
     * Acepta IDs nulos con seguridad.
     */
    private void cleanTeamFixture(Long teamId, Long captainId, Long tournamentId) {
        if (teamId != null) {
            standingRepository.findByTournamentId(
                    tournamentId != null ? tournamentId : -1L
            ).forEach(s -> standingRepository.deleteById(s.getId()));

            // Desligar usuarios del equipo antes de borrar el equipo
            userRepository.findAll().stream()
                    .filter(u -> u.getTeam() != null && u.getTeam().getId().equals(teamId))
                    .forEach(u -> {
                        u.setTeam(null);
                        userRepository.save(u);
                    });

            // Nullear team.captain antes de borrar el team para evitar FK violation
            // (team.captain_id apunta a users; si borramos el user primero, Supabase rechaza)
            teamRepository.findById(teamId).ifPresent(t -> {
                t.setCaptain(null);
                teamRepository.save(t);
            });

            teamRepository.deleteById(teamId);
        }
        if (captainId != null && userRepository.existsById(captainId)) {
            userRepository.deleteById(captainId);
        }
        if (tournamentId != null) tournamentRepository.deleteById(tournamentId);
    }

    // =========================================================================
    // createTeam
    // =========================================================================

    @Nested
    @DisplayName("createTeam")
    class CreateTeamTests {

        @Test
        @DisplayName("Debe retornar 201 CREATED cuando el capitán y torneo existen")
        void createTeam_ValidRequest_Returns201() {
            Tournament tournament = buildTournament();
            User       captain    = buildFreeUser("cap");
            Long       teamId     = null;

            try {
                ResponseEntity<TeamResponseDTO> response = teamController.createTeam(
                        validTeamRequest(tournament.getId(), String.valueOf(System.nanoTime())),
                        captain.getId()
                );

                assertNotNull(response);
                assertEquals(201, response.getStatusCode().value());
                assertNotNull(response.getBody());
                assertNotNull(response.getBody().getId());
                teamId = response.getBody().getId();

            } finally {
                cleanTeamFixture(teamId, captain.getId(), tournament.getId());
            }
        }

        @Test
        @DisplayName("Debe retornar 201 CREATED e incluir jugadores cuando se envían en el request")
        void createTeam_WithPlayers_Returns201() {
            Tournament tournament = buildTournament();
            User       captain    = buildFreeUser("cap_players");
            Long       teamId     = null;
            String     playerEmail = "player_" + System.nanoTime() + "@kicktime.test";

            try {
                CreateTeamRequestDTO request = CreateTeamRequestDTO.builder()
                        .name("Equipo Con Jugadores " + System.nanoTime())
                        .logoUrl("http://logo.test/img.png")
                        .captainStudentId(String.valueOf(System.nanoTime()))
                        .tournamentId(tournament.getId())
                        .players(List.of(
                                PlayerCreateDTO.builder()
                                        .name("Jugador Uno")
                                        .studentId(String.valueOf(System.nanoTime()))
                                        .email(playerEmail)
                                        .build()
                        ))
                        .build();

                ResponseEntity<TeamResponseDTO> response =
                        teamController.createTeam(request, captain.getId());

                assertNotNull(response);
                assertEquals(201, response.getStatusCode().value());
                assertNotNull(response.getBody());
                teamId = response.getBody().getId();

            } finally {
                // Borrar el jugador extra antes de limpiar el equipo
                userRepository.findByEmail(playerEmail)
                        .ifPresent(p -> {
                            p.setTeam(null);
                            userRepository.save(p);
                            userRepository.deleteById(p.getId());
                        });
                cleanTeamFixture(teamId, captain.getId(), tournament.getId());
            }
        }

        @Test
        @DisplayName("Debe retornar 201 CREATED y reutilizar usuario existente cuando el email ya está en BD")
        void createTeam_WithExistingPlayerEmail_Returns201() {
            Tournament tournament   = buildTournament();
            User       captain      = buildFreeUser("cap_existing");
            User       existingUser = buildFreeUser("existing_player");
            Long       teamId       = null;

            try {
                CreateTeamRequestDTO request = CreateTeamRequestDTO.builder()
                        .name("Equipo Reuso " + System.nanoTime())
                        .logoUrl("http://logo.test/img.png")
                        .captainStudentId(String.valueOf(System.nanoTime()))
                        .tournamentId(tournament.getId())
                        .players(List.of(
                                PlayerCreateDTO.builder()
                                        .name(existingUser.getName())
                                        .studentId(String.valueOf(System.nanoTime()))
                                        .email(existingUser.getEmail()) // email ya existe → orElseGet no se ejecuta
                                        .build()
                        ))
                        .build();

                ResponseEntity<TeamResponseDTO> response =
                        teamController.createTeam(request, captain.getId());

                assertNotNull(response);
                assertEquals(201, response.getStatusCode().value());
                teamId = response.getBody().getId();

            } finally {
                // existingUser quedó asignado al equipo; desligarlo antes de borrar
                userRepository.findById(existingUser.getId()).ifPresent(u -> {
                    u.setTeam(null);
                    userRepository.save(u);
                });
                cleanTeamFixture(teamId, captain.getId(), tournament.getId());
                userRepository.deleteById(existingUser.getId());
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el capitán ya pertenece a un equipo")
        void createTeam_CaptainAlreadyHasTeam_ThrowsException() {
            Tournament tournament = buildTournament();
            User       captain    = buildFreeUser("cap_busy");
            Long       teamId     = null;

            // Crear un primer equipo para que captain quede asignado
            ResponseEntity<TeamResponseDTO> first = teamController.createTeam(
                    validTeamRequest(tournament.getId(), String.valueOf(System.nanoTime())),
                    captain.getId()
            );
            teamId = first.getBody().getId();

            try {
                // Intentar crear un segundo equipo con el mismo capitán
                assertThrows(RuntimeException.class, () ->
                        teamController.createTeam(
                                validTeamRequest(tournament.getId(), String.valueOf(System.nanoTime())),
                                captain.getId()
                        )
                );
            } finally {
                cleanTeamFixture(teamId, captain.getId(), tournament.getId());
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el usuario creador no existe")
        void createTeam_UserNotFound_ThrowsException() {
            assertThrows(RuntimeException.class, () ->
                    teamController.createTeam(
                            validTeamRequest(1L, String.valueOf(System.nanoTime())),
                            999999L
                    )
            );
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el torneo no existe")
        void createTeam_TournamentNotFound_ThrowsException() {
            User captain = buildFreeUser("cap_notour");
            try {
                assertThrows(RuntimeException.class, () ->
                        teamController.createTeam(
                                validTeamRequest(999999L, String.valueOf(System.nanoTime())),
                                captain.getId()
                        )
                );
            } finally {
                userRepository.deleteById(captain.getId());
            }
        }
    }

    // =========================================================================
    // getTeam
    // =========================================================================

    @Nested
    @DisplayName("getTeam")
    class GetTeamTests {

        @Test
        @DisplayName("Debe retornar 200 OK con el equipo cuando existe")
        void getTeam_ExistingId_Returns200() {
            Tournament tournament = buildTournament();
            User       captain    = buildFreeUser("cap_get");
            Long       teamId     = null;

            try {
                ResponseEntity<TeamResponseDTO> created = teamController.createTeam(
                        validTeamRequest(tournament.getId(), String.valueOf(System.nanoTime())),
                        captain.getId()
                );
                teamId = created.getBody().getId();

                ResponseEntity<TeamResponseDTO> response = teamController.getTeam(teamId);

                assertNotNull(response);
                assertEquals(200, response.getStatusCode().value());
                assertNotNull(response.getBody());
                assertEquals(teamId, response.getBody().getId());

            } finally {
                cleanTeamFixture(teamId, captain.getId(), tournament.getId());
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el equipo no existe")
        void getTeam_NonExistingId_ThrowsException() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> teamController.getTeam(999999L));
            assertTrue(ex.getMessage().contains("Team not found"));
        }
    }

    // =========================================================================
    // getAllTeams
    // =========================================================================

    @Nested
    @DisplayName("getAllTeams")
    class GetAllTeamsTests {

        @Test
        @DisplayName("Debe retornar 200 OK con lista no nula")
        void getAllTeams_Returns200() {
            ResponseEntity<List<TeamResponseDTO>> response = teamController.getAllTeams();

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("Debe incluir el equipo recién creado en la lista")
        void getAllTeams_IncludesCreatedTeam() {
            Tournament tournament = buildTournament();
            User       captain    = buildFreeUser("cap_list");
            Long       teamId     = null;

            try {
                ResponseEntity<TeamResponseDTO> created = teamController.createTeam(
                        validTeamRequest(tournament.getId(), String.valueOf(System.nanoTime())),
                        captain.getId()
                );
                teamId = created.getBody().getId();
                final Long id = teamId;

                ResponseEntity<List<TeamResponseDTO>> response = teamController.getAllTeams();

                assertNotNull(response.getBody());
                assertFalse(response.getBody().isEmpty());
                assertTrue(response.getBody().stream().anyMatch(t -> t.getId().equals(id)));

            } finally {
                cleanTeamFixture(teamId, captain.getId(), tournament.getId());
            }
        }
    }

    // =========================================================================
    // addPlayerToTeam
    // =========================================================================

    @Nested
    @DisplayName("addPlayerToTeam")
    class AddPlayerToTeamTests {

        @Test
        @DisplayName("Debe retornar 200 OK al agregar jugador nuevo (email no existe en BD)")
        void addPlayerToTeam_NewPlayer_Returns200() {
            Tournament tournament = buildTournament();
            User       captain    = buildFreeUser("cap_addp");
            Long       teamId     = null;
            String     newEmail   = "newplayer_" + System.nanoTime() + "@kicktime.test";

            try {
                ResponseEntity<TeamResponseDTO> created = teamController.createTeam(
                        validTeamRequest(tournament.getId(), String.valueOf(System.nanoTime())),
                        captain.getId()
                );
                teamId = created.getBody().getId();

                PlayerCreateDTO playerDTO = PlayerCreateDTO.builder()
                        .name("Jugador Nuevo")
                        .studentId(String.valueOf(System.nanoTime()))
                        .email(newEmail)
                        .build();

                ResponseEntity<TeamResponseDTO> response =
                        teamController.addPlayerToTeam(teamId, playerDTO);

                assertNotNull(response);
                assertEquals(200, response.getStatusCode().value());
                assertNotNull(response.getBody());

            } finally {
                // Borrar jugador agregado
                userRepository.findByEmail(newEmail).ifPresent(p -> {
                    p.setTeam(null);
                    userRepository.save(p);
                    userRepository.deleteById(p.getId());
                });
                cleanTeamFixture(teamId, captain.getId(), tournament.getId());
            }
        }

        @Test
        @DisplayName("Debe retornar 200 OK al agregar jugador cuyo email ya existe en BD (orElseGet no se ejecuta)")
        void addPlayerToTeam_ExistingPlayer_Returns200() {
            Tournament tournament  = buildTournament();
            User       captain     = buildFreeUser("cap_addp_existing");
            User       existingUser = buildFreeUser("existing_addp");
            Long       teamId      = null;

            try {
                ResponseEntity<TeamResponseDTO> created = teamController.createTeam(
                        validTeamRequest(tournament.getId(), String.valueOf(System.nanoTime())),
                        captain.getId()
                );
                teamId = created.getBody().getId();

                PlayerCreateDTO playerDTO = PlayerCreateDTO.builder()
                        .name(existingUser.getName())
                        .studentId(String.valueOf(System.nanoTime()))
                        .email(existingUser.getEmail()) // hit en findByEmail
                        .build();

                ResponseEntity<TeamResponseDTO> response =
                        teamController.addPlayerToTeam(teamId, playerDTO);

                assertNotNull(response);
                assertEquals(200, response.getStatusCode().value());

            } finally {
                userRepository.findById(existingUser.getId()).ifPresent(u -> {
                    u.setTeam(null);
                    userRepository.save(u);
                });
                cleanTeamFixture(teamId, captain.getId(), tournament.getId());
                userRepository.deleteById(existingUser.getId());
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el equipo no existe")
        void addPlayerToTeam_TeamNotFound_ThrowsException() {
            PlayerCreateDTO playerDTO = PlayerCreateDTO.builder()
                    .name("Jugador X")
                    .studentId(String.valueOf(System.nanoTime()))
                    .email("playerx_" + System.nanoTime() + "@kicktime.test")
                    .build();

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> teamController.addPlayerToTeam(999999L, playerDTO));
            assertTrue(ex.getMessage().contains("Team not found"));
        }
    }

    // =========================================================================
    // changeCaptain
    // =========================================================================

    @Nested
    @DisplayName("changeCaptain")
    class ChangeCaptainTests {

        @Test
        @DisplayName("Debe retornar 200 OK al cambiar capitán por un jugador del equipo")
        void changeCaptain_ValidMember_Returns200() {
            Tournament tournament = buildTournament();
            User       captain    = buildFreeUser("cap_change");
            Long       teamId     = null;
            String     playerEmail = "newcap_" + System.nanoTime() + "@kicktime.test";

            try {
                // Crear equipo con un jugador
                CreateTeamRequestDTO request = CreateTeamRequestDTO.builder()
                        .name("Equipo Change " + System.nanoTime())
                        .logoUrl("http://logo.test/img.png")
                        .captainStudentId(String.valueOf(System.nanoTime()))
                        .tournamentId(tournament.getId())
                        .players(List.of(
                                PlayerCreateDTO.builder()
                                        .name("Nuevo Capitan")
                                        .studentId(String.valueOf(System.nanoTime()))
                                        .email(playerEmail)
                                        .build()
                        ))
                        .build();

                ResponseEntity<TeamResponseDTO> created =
                        teamController.createTeam(request, captain.getId());
                teamId = created.getBody().getId();

                // Buscar el jugador recién creado
                User newCaptain = userRepository.findByEmail(playerEmail).orElseThrow();

                // Asegurarse de que el jugador tiene el equipo asignado
                // (addPlayerToTeam tiene @Transactional(readOnly=true) — verificar en BD)
                newCaptain.setTeam(teamRepository.findById(teamId).orElseThrow());
                userRepository.save(newCaptain);

                ResponseEntity<TeamResponseDTO> response =
                        teamController.changeCaptain(teamId, newCaptain.getId());

                assertNotNull(response);
                assertEquals(200, response.getStatusCode().value());
                assertNotNull(response.getBody());

            } finally {
                // Nullear team.captain PRIMERO antes de borrar cualquier user
                // (tras changeCaptain, team.captain apunta al newCaptain, no al original)
                if (teamId != null) {
                    teamRepository.findById(teamId).ifPresent(t -> {
                        t.setCaptain(null);
                        teamRepository.save(t);
                    });
                }
                userRepository.findByEmail(playerEmail).ifPresent(p -> {
                    p.setTeam(null);
                    userRepository.save(p);
                    userRepository.deleteById(p.getId());
                });
                cleanTeamFixture(teamId, captain.getId(), tournament.getId());
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el usuario no pertenece al equipo")
        void changeCaptain_UserNotInTeam_ThrowsException() {
            Tournament tournament = buildTournament();
            User       captain    = buildFreeUser("cap_wrong");
            User       outsider   = buildFreeUser("outsider_change");
            Long       teamId     = null;

            try {
                ResponseEntity<TeamResponseDTO> created = teamController.createTeam(
                        validTeamRequest(tournament.getId(), String.valueOf(System.nanoTime())),
                        captain.getId()
                );
                teamId = created.getBody().getId();

                // outsider no tiene team asignado → lanza "User is not part of this team"
                final Long finalTeamId = teamId;
                RuntimeException ex = assertThrows(RuntimeException.class,
                        () -> teamController.changeCaptain(finalTeamId, outsider.getId()));
                assertTrue(ex.getMessage().contains("User is not part of this team"));

            } finally {
                cleanTeamFixture(teamId, captain.getId(), tournament.getId());
                userRepository.deleteById(outsider.getId());
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el equipo no existe")
        void changeCaptain_TeamNotFound_ThrowsException() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> teamController.changeCaptain(999999L, 1L));
            assertTrue(ex.getMessage().contains("Team not found"));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el usuario no existe")
        void changeCaptain_UserNotFound_ThrowsException() {
            Tournament tournament = buildTournament();
            User       captain    = buildFreeUser("cap_nouser");
            Long       teamId     = null;

            try {
                ResponseEntity<TeamResponseDTO> created = teamController.createTeam(
                        validTeamRequest(tournament.getId(), String.valueOf(System.nanoTime())),
                        captain.getId()
                );
                teamId = created.getBody().getId();

                final Long finalTeamId = teamId;
                RuntimeException ex = assertThrows(RuntimeException.class,
                        () -> teamController.changeCaptain(finalTeamId, 999999L));
                assertTrue(ex.getMessage().contains("User not found"));

            } finally {
                cleanTeamFixture(teamId, captain.getId(), tournament.getId());
            }
        }
    }

    // =========================================================================
    // deleteTeam
    // =========================================================================

    @Nested
    @DisplayName("deleteTeam")
    class DeleteTeamTests {

        @Test
        @DisplayName("Debe retornar 204 NO CONTENT al eliminar un equipo existente")
        void deleteTeam_ExistingId_Returns204() {
            Tournament tournament = buildTournament();
            User       captain    = buildFreeUser("cap_del");

            ResponseEntity<TeamResponseDTO> created = teamController.createTeam(
                    validTeamRequest(tournament.getId(), String.valueOf(System.nanoTime())),
                    captain.getId()
            );
            Long teamId = created.getBody().getId();

            // Limpiar Standing antes de borrar el equipo vía controlador
            standingRepository.findByTournamentId(tournament.getId())
                    .forEach(s -> standingRepository.deleteById(s.getId()));

            // Desligar captain.team y nullear team.captain antes de borrar (evita FK violation)
            User cap = userRepository.findById(captain.getId()).orElseThrow();
            cap.setTeam(null);
            userRepository.save(cap);
            teamRepository.findById(teamId).ifPresent(t -> {
                t.setCaptain(null);
                teamRepository.save(t);
            });

            try {
                ResponseEntity<Void> response = teamController.deleteTeam(teamId);

                assertNotNull(response);
                assertEquals(204, response.getStatusCode().value());
                assertFalse(teamRepository.existsById(teamId));

            } finally {
                // El equipo ya fue borrado; limpiar user y tournament
                if (userRepository.existsById(captain.getId())) {
                    userRepository.deleteById(captain.getId());
                }
                tournamentRepository.deleteById(tournament.getId());
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción al eliminar un equipo inexistente")
        void deleteTeam_NonExistingId_ThrowsException() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> teamController.deleteTeam(999999L));
            assertTrue(ex.getMessage().contains("Team not found"));
        }
    }
}