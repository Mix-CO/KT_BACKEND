package com.kicktime.backend.domain.services;

import com.kicktime.backend.domain.model.*;
import com.kicktime.backend.domain.model.enums.MatchStatus;

import com.kicktime.backend.domain.model.dto.request.CreateMatchRequestDTO;
import com.kicktime.backend.domain.model.dto.request.ScheduleMatchRequestDTO;
import com.kicktime.backend.domain.model.dto.request.RecordMatchResultRequestDTO;

import com.kicktime.backend.domain.model.dto.response.MatchResponseDTO;

import com.kicktime.backend.repository.*;

import com.kicktime.backend.util.mappers.MatchMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final TournamentRepository tournamentRepository;
    private final FieldRepository fieldRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final MatchResultRepository matchResultRepository;

    private final MatchMapper matchMapper;
    private final StandingService standingService;
    /**
     * Create match
     */
    public MatchResponseDTO createMatch(CreateMatchRequestDTO request) {

        Team homeTeam = teamRepository.findById(request.getHomeTeamId())
                .orElseThrow(() -> new RuntimeException("Home team not found"));

        Team awayTeam = teamRepository.findById(request.getAwayTeamId())
                .orElseThrow(() -> new RuntimeException("Away team not found"));

        Tournament tournament = tournamentRepository.findById(request.getTournamentId())
                .orElseThrow(() -> new RuntimeException("Tournament not found"));

        Match match = Match.builder()
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .status(MatchStatus.SCHEDULED)
                .build();

        Match savedMatch = matchRepository.save(match);

        return matchMapper.toDTO(savedMatch);
    }

    /**
     * Get match by id
     */
    public MatchResponseDTO getMatch(Long matchId) {

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        return matchMapper.toDTO(match);
    }

    /**
     * Get matches by tournament
     */
    public List<MatchResponseDTO> getMatchesByTournament(Long tournamentId) {

        List<Match> matches = matchRepository.findByTournament_Id(tournamentId);

        return matches.stream()
                .map(matchMapper::toDTO)
                .toList();
    }

    /**
     * Schedule match
     */
    public MatchResponseDTO scheduleMatch(Long matchId, ScheduleMatchRequestDTO request) {

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        Field field = fieldRepository.findById(request.getFieldId())
                .orElseThrow(() -> new RuntimeException("Field not found"));

        TimeSlot timeSlot = timeSlotRepository.findById(request.getTimeSlotId())
                .orElseThrow(() -> new RuntimeException("TimeSlot not found"));

        match.setField(field);
        match.setTimeSlot(timeSlot);
        match.setStatus(MatchStatus.CONFIRMED);

        Match updatedMatch = matchRepository.save(match);

        return matchMapper.toDTO(updatedMatch);
    }

    /**
     * Record match result
     */
    public MatchResponseDTO recordMatchResult(Long matchId, RecordMatchResultRequestDTO request) {

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        Team winner = null;

        if (request.getHomeScore() > request.getAwayScore()) {
            winner = match.getHomeTeam();
        } else if (request.getAwayScore() > request.getHomeScore()) {
            winner = match.getAwayTeam();
        }

        MatchResult result = MatchResult.builder()
                .homeScore(request.getHomeScore())
                .awayScore(request.getAwayScore())
                .winner(winner)
                .recordedAt(LocalDateTime.now())
                .build();

        matchResultRepository.save(result);

        match.setResult(result);
        match.setStatus(MatchStatus.PLAYED);

        Match updatedMatch = matchRepository.save(match);
        standingService.updateStandingsAfterMatch(match);
        return matchMapper.toDTO(updatedMatch);
    }

    /**
     * Delete match
     */
    public void deleteMatch(Long matchId) {

        if (!matchRepository.existsById(matchId)) {
            throw new RuntimeException("Match not found");
        }

        matchRepository.deleteById(matchId);
    }
}
