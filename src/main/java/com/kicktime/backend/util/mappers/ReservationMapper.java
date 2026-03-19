package com.kicktime.backend.util.mappers;

import com.kicktime.backend.domain.model.Reservation;
import com.kicktime.backend.domain.model.dto.response.ReservationResponseDTO;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ReservationMapper {

    @Mapping(source = "match.id", target = "matchId")
    @Mapping(source = "timeSlot.id", target = "timeSlotId")
    @Mapping(source = "timeSlot.dayOfWeek", target = "dayOfWeek")
    @Mapping(source = "timeSlot.start", target = "startTime")
    @Mapping(source = "timeSlot.end", target = "endTime")
    @Mapping(source = "proposedBy.id", target = "proposedByUserId")
    @Mapping(source = "proposedBy.name", target = "proposedByName")

    ReservationResponseDTO toDTO(Reservation reservation);
}
