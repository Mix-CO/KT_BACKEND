package com.kicktime.backend.util.mappers;

import com.kicktime.backend.domain.model.Match;
import com.kicktime.backend.domain.model.dto.response.MatchResponseDTO;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface MatchMapper {

    @Mapping(source = "homeTeam.id", target = "homeTeamId")
    @Mapping(source = "homeTeam.name", target = "homeTeamName")

    @Mapping(source = "awayTeam.id", target = "awayTeamId")
    @Mapping(source = "awayTeam.name", target = "awayTeamName")

    @Mapping(source = "field.number", target = "fieldId")

    @Mapping(source = "timeSlot.id", target = "timeSlotId")

    @Mapping(source = "result.homeScore", target = "homeScore")
    @Mapping(source = "result.awayScore", target = "awayScore")

    MatchResponseDTO toDTO(Match match);
}
