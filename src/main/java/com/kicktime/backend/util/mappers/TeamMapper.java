package com.kicktime.backend.util.mappers;

import com.kicktime.backend.domain.model.Team;
import com.kicktime.backend.domain.model.User;
import com.kicktime.backend.domain.model.dto.response.TeamResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface TeamMapper {

    @Mapping(source = "captain.name", target = "captainName")
    @Mapping(source = "captain.studentId", target = "captainStudentId")
    @Mapping(target = "players", expression = "java(mapPlayers(team.getPlayers()))")
    TeamResponseDTO toDTO(Team team);

    List<TeamResponseDTO> toDTOList(List<Team> teams);

    default List<String> mapPlayers(List<User> players) {
        if (players == null) return List.of();
        return players.stream()
                .map(User::getName)
                .collect(Collectors.toList());
    }
}

