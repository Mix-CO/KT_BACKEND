package com.kicktime.backend.util.mappers;

import com.kicktime.backend.domain.model.Tournament;
import com.kicktime.backend.domain.model.dto.response.TournamentResponseDTO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {TeamMapper.class})
public interface TournamentMapper {

    TournamentResponseDTO toDTO(Tournament tournament);
    List<TournamentResponseDTO> toDTOList(List<Tournament> tournaments);
}