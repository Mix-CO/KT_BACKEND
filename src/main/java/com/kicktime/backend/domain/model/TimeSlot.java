package com.kicktime.backend.domain.model;

import com.kicktime.backend.domain.model.enums.TimeSlotStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "start_time")
    private LocalTime start;
    private DayOfWeek dayOfWeek;
    @Column(name = "end_time")
    private LocalTime end;
    @ManyToOne
    private Field field;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimeSlotStatus status = TimeSlotStatus.AVAILABLE;
}
