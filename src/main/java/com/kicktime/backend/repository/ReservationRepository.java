package com.kicktime.backend.repository;

import com.kicktime.backend.domain.model.Reservation;
import com.kicktime.backend.domain.model.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByMatchId(Long matchId);

    List<Reservation> findByProposedById(Long userId);

    List<Reservation> findByStatus(ReservationStatus status);

    List<Reservation> findByTimeSlotIdAndStatus(Long timeSlotId, ReservationStatus status);
}
