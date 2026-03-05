package com.kicktime.backend.model;

import com.kicktime.backend.model.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Match match;

    private LocalDateTime proposedTime;

    @ManyToOne
    private User proposedBy;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;
}
