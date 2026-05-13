package com.kicktime.backend.domain.model.dto.response;

import lombok.Getter;

@Getter
public class LoginResponseDTO {
    private String token;

    public LoginResponseDTO(String token) {
        this.token = token;
    }

}
