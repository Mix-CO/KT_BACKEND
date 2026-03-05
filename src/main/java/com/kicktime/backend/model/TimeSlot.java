package com.kicktime.backend.model;

import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlot {

    private LocalTime start;

    private LocalTime end;
}
