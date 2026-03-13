package com.kicktime.backend.repository;

import com.kicktime.backend.domain.model.Availability;
import com.kicktime.backend.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface AvailabilityRepository extends JpaRepository<Availability, Long> {

    List<Availability> findByUser_Id(Long userId);

    List<Availability> findByUserAndDayOfWeek(User user, DayOfWeek dayOfWeek);

}
