package com.kicktime.backend.repository;

import com.kicktime.backend.domain.model.Standing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StandingRepository extends JpaRepository<Standing, Long> {

    List<Standing> findByTournamentId(Long tournamentId);

    Optional<Standing> findByTeamIdAndTournamentId(Long teamId, Long tournamentId);
    List<Standing> findByTournamentIdOrderByPointsDesc(Long tournamentId);
}

