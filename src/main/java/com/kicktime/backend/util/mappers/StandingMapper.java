package com.kicktime.backend.util.mappers;

import com.kicktime.backend.domain.model.Standing;
import com.kicktime.backend.domain.model.dto.response.StandingResponseDTO;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface StandingMapper {

    @Mapping(source = "team.id", target = "teamId")
    @Mapping(source = "team.name", target = "teamName")
    @Mapping(source = "tournament.id", target = "tournamentId")

    StandingResponseDTO toDTO(Standing standing);
}
