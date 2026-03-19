package com.kicktime.backend.repository;

import com.kicktime.backend.domain.model.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
}

