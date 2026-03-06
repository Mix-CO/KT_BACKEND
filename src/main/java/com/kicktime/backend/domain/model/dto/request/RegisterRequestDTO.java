package com.kicktime.backend.domain.model.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequestDTO {
    private String name;
    private String email;
    private String password;
}
