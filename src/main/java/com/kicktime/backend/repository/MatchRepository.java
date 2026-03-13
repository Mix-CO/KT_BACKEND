package com.kicktime.backend.repository;

import com.kicktime.backend.domain.model.Match;
import com.kicktime.backend.domain.model.Team;
import com.kicktime.backend.domain.model.enums.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByHomeTeam(Team homeTeam);

    List<Match> findByAwayTeam(Team awayTeam);

    List<Match> findByHomeTeamOrAwayTeam(Team homeTeam, Team awayTeam);

    List<Match> findByStatus(MatchStatus status);

    List<Match> findByTournament_Id(Long tournamentId);

}
