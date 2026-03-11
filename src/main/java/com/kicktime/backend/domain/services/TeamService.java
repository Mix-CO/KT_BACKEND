package com.kicktime.backend.domain.services;

import com.kicktime.backend.domain.model.Team;
import com.kicktime.backend.domain.model.User;
import com.kicktime.backend.domain.model.enums.UserRole;
import com.kicktime.backend.domain.model.dto.request.CreateTeamRequestDTO;
import com.kicktime.backend.domain.model.dto.request.PlayerCreateDTO;
import com.kicktime.backend.domain.model.dto.response.TeamResponseDTO;
import com.kicktime.backend.util.mappers.TeamMapper;
import com.kicktime.backend.repository.TeamRepository;
import com.kicktime.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final TeamMapper teamMapper;

    /**
     * Create a new team
     */
    public TeamResponseDTO createTeam(CreateTeamRequestDTO request, Long creatorUserId) {

        User captain = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (captain.getTeam() != null) {
            throw new RuntimeException("User already belongs to a team");
        }

        // set studentId
        captain.setStudentId(request.getCaptainStudentId());

        // promote to captain
        captain.setRole(UserRole.CAPTAIN);

        Team team = Team.builder()
                .name(request.getName())
                .logoUrl(request.getLogoUrl())
                .captain(captain)
                .build();

        team = teamRepository.save(team);

        captain.setTeam(team);
        userRepository.save(captain);

        // create players if provided
        if (request.getPlayers() != null) {

            for (PlayerCreateDTO playerDTO : request.getPlayers()) {

                User player = User.builder()
                        .name(playerDTO.getName())
                        .studentId(playerDTO.getStudentId())
                        .role(UserRole.PLAYER)
                        .team(team)
                        .build();

                userRepository.save(player);
            }
        }

        Team savedTeam = teamRepository.findById(team.getId())
                .orElseThrow();

        return teamMapper.toDTO(savedTeam);
    }

    /**
     * Get team by id
     */
    public TeamResponseDTO getTeam(Long id) {

        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        return teamMapper.toDTO(team);
    }

    /**
     * Get all teams
     */
    public List<TeamResponseDTO> getAllTeams() {

        List<Team> teams = teamRepository.findAll();

        return teamMapper.toDTOList(teams);
    }

    /**
     * Get teams by tournament
     */
    public List<TeamResponseDTO> getTeamsByTournament(Long tournamentId) {

        List<Team> teams = teamRepository.findByTournamentId(tournamentId);

        return teamMapper.toDTOList(teams);
    }

    /**
     * Add player to team
     */
    public TeamResponseDTO addPlayerToTeam(Long teamId, PlayerCreateDTO playerDTO) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        User player = User.builder()
                .name(playerDTO.getName())
                .studentId(playerDTO.getStudentId())
                .role(UserRole.PLAYER)
                .team(team)
                .build();

        userRepository.save(player);

        Team updatedTeam = teamRepository.findById(teamId)
                .orElseThrow();

        return teamMapper.toDTO(updatedTeam);
    }

    /**
     * Change captain
     */
    public TeamResponseDTO changeCaptain(Long teamId, Long userId) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

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
    public void deleteTeam(Long teamId) {

        if (!teamRepository.existsById(teamId)) {
            throw new RuntimeException("Team not found");
        }

        teamRepository.deleteById(teamId);
    }
}

