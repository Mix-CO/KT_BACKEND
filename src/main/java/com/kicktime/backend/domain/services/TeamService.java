package com.kicktime.backend.domain.services;

import com.kicktime.backend.domain.model.Standing;
import com.kicktime.backend.domain.model.Team;
import com.kicktime.backend.domain.model.Tournament;
import com.kicktime.backend.domain.model.User;
import com.kicktime.backend.domain.model.enums.UserRole;
import com.kicktime.backend.domain.model.dto.request.CreateTeamRequestDTO;
import com.kicktime.backend.domain.model.dto.request.PlayerCreateDTO;
import com.kicktime.backend.domain.model.dto.response.TeamResponseDTO;
import com.kicktime.backend.repository.StandingRepository;
import com.kicktime.backend.repository.TournamentRepository;
import com.kicktime.backend.util.mappers.TeamMapper;
import com.kicktime.backend.repository.TeamRepository;
import com.kicktime.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final StandingRepository standingRepository;
    private final TeamRepository teamRepository;
    private final TournamentRepository tournamentRepository;
    private final UserRepository userRepository;
    private final TeamMapper teamMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private static final String DEFAULT_PASSWORD = "kicktime123";

    private static final String TEAM_NOT_FOUND = "Team not found";

    /**
     * Create a new team
     */
    @Transactional
    public TeamResponseDTO createTeam(CreateTeamRequestDTO request, Long creatorUserId) {

        User captain = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (captain.getTeam() != null) {
            throw new RuntimeException("User already belongs to a team");
        }

        Tournament tournament = tournamentRepository.findById(request.getTournamentId())
                .orElseThrow(() -> new RuntimeException("Tournament not found"));

        captain.setStudentId(request.getCaptainStudentId());
        captain.setRole(UserRole.CAPTAIN);

        Team team = Team.builder()
                .name(request.getName())
                .logoUrl(request.getLogoUrl())
                .captain(captain)
                .tournament(tournament)
                .build();

        team = teamRepository.save(team);

        captain.setTeam(team);
        userRepository.save(captain);

        // create players if provided
        if (request.getPlayers() != null) {
            for (PlayerCreateDTO playerDTO : request.getPlayers()) {
                Team finalTeam = team;
                User player = userRepository.findByEmail(playerDTO.getEmail())
                        .orElseGet(() -> User.builder()
                                .name(playerDTO.getName())
                                .studentId(playerDTO.getStudentId())
                                .email(playerDTO.getEmail())
                                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                                .role(UserRole.PLAYER)
                                .build());

                player.setTeam(finalTeam);
                if (player.getRole() == null) {
                    player.setRole(UserRole.PLAYER);
                }
                userRepository.save(player);
            }
        }

        Team savedTeam = teamRepository.findById(team.getId()).orElseThrow();

        Standing standing = Standing.builder()
                .team(savedTeam)
                .tournament(tournament)
                .played(0)
                .wins(0)
                .draws(0)
                .losses(0)
                .goalsFor(0)
                .goalsAgainst(0)
                .points(0)
                .build();

        standingRepository.save(standing);

        return teamMapper.toDTO(savedTeam);
    }

    /**
     * Get team by id
     */
    @Transactional(readOnly = true)
    public TeamResponseDTO getTeam(Long id) {

        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(TEAM_NOT_FOUND));

        return teamMapper.toDTO(team);
    }

    /**
     * Get all teams
     */
    @Transactional(readOnly = true)
    public List<TeamResponseDTO> getAllTeams() {

        List<Team> teams = teamRepository.findAll();

        return teamMapper.toDTOList(teams);
    }

    /**
     * Get teams by tournament
     */
    @Transactional(readOnly = true)
    public List<TeamResponseDTO> getTeamsByTournament(Long tournamentId) {

        List<Team> teams = teamRepository.findByTournamentId(tournamentId);

        return teamMapper.toDTOList(teams);
    }

    /**
     * Add player to team
     */
    @Transactional
    public TeamResponseDTO addPlayerToTeam(Long teamId, PlayerCreateDTO playerDTO) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException(TEAM_NOT_FOUND));

        User player = userRepository.findByEmail(playerDTO.getEmail())
                .orElseGet(() -> User.builder()
                        .name(playerDTO.getName())
                        .studentId(playerDTO.getStudentId())
                        .email(playerDTO.getEmail())
                        .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                        .role(UserRole.PLAYER)
                        .build());

        player.setTeam(team);
        if (player.getRole() == null) {
            player.setRole(UserRole.PLAYER);
        }
        userRepository.save(player);

        Team updatedTeam = teamRepository.findById(teamId).orElseThrow();

        return teamMapper.toDTO(updatedTeam);
    }

    /**
     * Change captain
     */
    @Transactional
    public TeamResponseDTO changeCaptain(Long teamId, Long userId) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException(TEAM_NOT_FOUND));

        User newCaptain = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (newCaptain.getTeam() == null || !newCaptain.getTeam().getId().equals(teamId)) {
            throw new RuntimeException("User is not part of this team");
        }

        newCaptain.setRole(UserRole.CAPTAIN);

        team.setCaptain(newCaptain);

        Team updatedTeam = teamRepository.save(team);

        return teamMapper.toDTO(updatedTeam);
    }

    /**
     * Delete team
     */
    @Transactional
    public void deleteTeam(Long teamId) {

        if (!teamRepository.existsById(teamId)) {
            throw new RuntimeException(TEAM_NOT_FOUND);
        }

        teamRepository.deleteById(teamId);
    }
}
