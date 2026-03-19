package com.kicktime.backend.repository;

import com.kicktime.backend.domain.model.MatchResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {
}