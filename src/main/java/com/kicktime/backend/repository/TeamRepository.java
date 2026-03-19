package com.kicktime.backend.repository;

import com.kicktime.backend.domain.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findByName(String name);

    List<Team> findByTournamentId(Long tournamentId);

    Optional<Team> findByCaptainId(Long captainId);

}

