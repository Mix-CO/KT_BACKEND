package com.kicktime.backend.domain.model.dto.request;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTeamRequestDTO {

    private String name;
    private String logoUrl;
    private String captainStudentId;
    private Long tournamentId;
    private List<PlayerCreateDTO> players;
}
