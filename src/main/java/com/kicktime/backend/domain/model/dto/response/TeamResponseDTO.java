package com.kicktime.backend.domain.model.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamResponseDTO {

    private Long id;

    private String name;

    private String logoUrl;

    private String captainName;

    private String captainStudentId;

    private List<String> players;
}

