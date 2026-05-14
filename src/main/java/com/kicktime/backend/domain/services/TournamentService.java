package com.kicktime.backend.domain.services;

import com.kicktime.backend.domain.model.Tournament;
import com.kicktime.backend.domain.model.Team;

import com.kicktime.backend.domain.model.enums.TournamentStatus;

import com.kicktime.backend.domain.model.dto.request.CreateTournamentRequestDTO;
import com.kicktime.backend.domain.model.dto.request.UpdateTournamentStatusRequestDTO;

import com.kicktime.backend.domain.model.dto.response.TournamentResponseDTO;
import com.kicktime.backend.domain.model.dto.response.TeamResponseDTO;

import com.kicktime.backend.repository.TournamentRepository;
import com.kicktime.backend.repository.TeamRepository;

import com.kicktime.backend.util.mappers.TournamentMapper;
import com.kicktime.backend.util.mappers.TeamMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TeamRepository teamRepository;

    private final TournamentMapper tournamentMapper;
    private final TeamMapper teamMapper;

    /**
     * Create tournament
     */
    @Transactional
    public TournamentResponseDTO createTournament(CreateTournamentRequestDTO request) {

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new RuntimeException("Start date cannot be after end date");
        }

        if (request.getMinTeams() > request.getMaxTeams()) {
            throw new RuntimeException("Invalid team limits");
        }

        if (request.getMinPlayersPerTeam() > request.getMaxPlayersPerTeam()) {
            throw new RuntimeException("Invalid player limits");
        }

        Tournament tournament = Tournament.builder()
                .name(request.getName())
                .semester(request.getSemester())
                .category(request.getCategory())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .minPlayersPerTeam(request.getMinPlayersPerTeam())
                .maxPlayersPerTeam(request.getMaxPlayersPerTeam())
                .minTeams(request.getMinTeams())
                .maxTeams(request.getMaxTeams())
                .status(TournamentStatus.PLANNED)
                .build();

        Tournament savedTournament = tournamentRepository.save(tournament);

        return tournamentMapper.toDTO(savedTournament);
    }

    /**
     * Get tournament by id
     */
    @Transactional(readOnly = true)
    public TournamentResponseDTO getTournament(Long id) {

        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));

        return tournamentMapper.toDTO(tournament);
    }

    /**
     * Get all tournaments
     */
    @Transactional(readOnly = true)
    public List<TournamentResponseDTO> getAllTournaments() {

        List<Tournament> tournaments = tournamentRepository.findAll();

        return tournamentMapper.toDTOList(tournaments);
    }

    /**
     * Update tournament status
     */
    @Transactional
    public TournamentResponseDTO updateTournamentStatus(Long tournamentId, UpdateTournamentStatusRequestDTO request) {

        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));

        tournament.setStatus(request.getStatus());

        Tournament updatedTournament = tournamentRepository.save(tournament);

        return tournamentMapper.toDTO(updatedTournament);
    }

    /**
     * Delete tournament
     */
    @Transactional
    public void deleteTournament(Long tournamentId) {

        if (!tournamentRepository.existsById(tournamentId)) {
            throw new RuntimeException("Tournament not found");
        }

        tournamentRepository.deleteById(tournamentId);
    }

    /**
     * Get teams in tournament
     */
    @Transactional(readOnly = true)
    public List<TeamResponseDTO> getTeamsInTournament(Long tournamentId) {

        List<Team> teams = teamRepository.findByTournamentId(tournamentId);

        return teamMapper.toDTOList(teams);
    }
}
