package com.kicktime.backend.api;

import com.kicktime.backend.domain.model.TimeSlot;
import com.kicktime.backend.domain.model.enums.TimeSlotStatus;
import com.kicktime.backend.repository.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/timeslots")
@RequiredArgsConstructor
public class TimeSlotController {

    private final TimeSlotRepository timeSlotRepository;

    @GetMapping
    public ResponseEntity<List<TimeSlot>> getAllTimeSlots() {
        return ResponseEntity.ok(timeSlotRepository.findAll());
    }

    @GetMapping("/available")
    public ResponseEntity<List<TimeSlot>> getAvailableTimeSlots() {
        return ResponseEntity.ok(
                timeSlotRepository.findAll().stream()
                        .filter(ts -> ts.getStatus() == TimeSlotStatus.AVAILABLE)
                        .toList()
        );
    }
}