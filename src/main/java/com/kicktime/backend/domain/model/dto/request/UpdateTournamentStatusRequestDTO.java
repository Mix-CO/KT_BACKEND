package com.kicktime.backend.domain.model.dto.request;

import com.kicktime.backend.domain.model.enums.TournamentStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTournamentStatusRequestDTO {

    private TournamentStatus status;

}
