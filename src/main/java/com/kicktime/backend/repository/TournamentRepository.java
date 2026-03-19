package com.kicktime.backend.repository;

import com.kicktime.backend.domain.model.Tournament;
import com.kicktime.backend.domain.model.enums.TournamentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    List<Tournament> findByStatus(TournamentStatus status);

}
