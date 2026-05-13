package com.kicktime.backend.domain.model.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitializeStandingsRequestDTO {

    private Long tournamentId;
}