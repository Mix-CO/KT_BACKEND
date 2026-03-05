package com.kicktime.backend.model.dto.response;

import com.kicktime.backend.model.enums.UserRole;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterResponseDTO {

    private Long id;
    private String name;
    private String email;
    private UserRole role;
}