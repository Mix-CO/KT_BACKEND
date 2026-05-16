package com.kicktime.backend.domain.services;

import com.kicktime.backend.domain.model.*;

import com.kicktime.backend.domain.model.dto.request.InitializeStandingsRequestDTO;
import com.kicktime.backend.domain.model.dto.response.StandingResponseDTO;

import com.kicktime.backend.repository.*;

import com.kicktime.backend.util.mappers.StandingMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StandingService {

    private final StandingRepository standingRepository;
    private final TournamentRepository tournamentRepository;
    private final TeamRepository teamRepository;

    private final StandingMapper standingMapper;

    /**
     * Initialize standings for tournament
     */
    public void initializeStandings(InitializeStandingsRequestDTO request) {

        Tournament tournament = tournamentRepository.findById(request.getTournamentId())
                .orElseThrow(() -> new RuntimeException("Tournament not found"));

        List<Team> teams = teamRepository.findByTournamentId(tournament.getId());

        for (Team team : teams) {

            Standing standing = Standing.builder()
                    .team(team)
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
        }
    }

    /**
     * Get standings by tournament
     */
    public List<StandingResponseDTO> getStandingsByTournament(Long tournamentId) {

        List<Standing> standings =
                standingRepository.findByTournamentIdOrderByPointsDesc(tournamentId);

        return standings.stream()
                .map(standingMapper::toDTO)
                .toList();
    }

    /**
     * Get or create standing for a team in a tournament
     */
    private Standing getOrCreateStanding(Team team, Tournament tournament) {
        return standingRepository
                .findByTeamIdAndTournamentId(team.getId(), tournament.getId())
                .orElseGet(() -> standingRepository.save(
                        Standing.builder()
                                .team(team)
                                .tournament(tournament)
                                .played(0)
                                .wins(0)
                                .draws(0)
                                .losses(0)
                                .goalsFor(0)
                                .goalsAgainst(0)
                                .points(0)
                                .build()
                ));
    }

    /**
     * Update standings after match
     */
    public void updateStandingsAfterMatch(Match match) {

        MatchResult result = match.getResult();

        if (result == null) {
            return;
        }

        Tournament tournament = match.getTournament();

        Standing homeStanding = getOrCreateStanding(match.getHomeTeam(), tournament);
        Standing awayStanding = getOrCreateStanding(match.getAwayTeam(), tournament);

        int homeScore = result.getHomeScore();
        int awayScore = result.getAwayScore();

        homeStanding.setPlayed(homeStanding.getPlayed() + 1);
        awayStanding.setPlayed(awayStanding.getPlayed() + 1);

        homeStanding.setGoalsFor(homeStanding.getGoalsFor() + homeScore);
        homeStanding.setGoalsAgainst(homeStanding.getGoalsAgainst() + awayScore);

        awayStanding.setGoalsFor(awayStanding.getGoalsFor() + awayScore);
        awayStanding.setGoalsAgainst(awayStanding.getGoalsAgainst() + homeScore);

        if (homeScore > awayScore) {
            homeStanding.setWins(homeStanding.getWins() + 1);
            awayStanding.setLosses(awayStanding.getLosses() + 1);
            homeStanding.setPoints(homeStanding.getPoints() + 3);
        } else if (awayScore > homeScore) {
            awayStanding.setWins(awayStanding.getWins() + 1);
            homeStanding.setLosses(homeStanding.getLosses() + 1);
            awayStanding.setPoints(awayStanding.getPoints() + 3);
        } else {
            homeStanding.setDraws(homeStanding.getDraws() + 1);
            awayStanding.setDraws(awayStanding.getDraws() + 1);
            homeStanding.setPoints(homeStanding.getPoints() + 1);
            awayStanding.setPoints(awayStanding.getPoints() + 1);
        }

        standingRepository.save(homeStanding);
        standingRepository.save(awayStanding);
    }
}