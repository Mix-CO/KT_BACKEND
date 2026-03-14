package com.kicktime.backend.util.mappers;

import com.kicktime.backend.domain.model.Availability;
import com.kicktime.backend.domain.model.dto.response.AvailabilityResponseDTO;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AvailabilityMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.name", target = "userName")
    @Mapping(source = "timeSlot.id", target = "timeSlotId")
    AvailabilityResponseDTO toDTO(Availability availability);
}
