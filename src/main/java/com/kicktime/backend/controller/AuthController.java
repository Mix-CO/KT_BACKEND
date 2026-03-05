package com.kicktime.backend.controller;

import com.kicktime.backend.model.dto.request.RegisterRequestDTO;
import com.kicktime.backend.model.User;
import com.kicktime.backend.model.dto.response.RegisterResponseDTO;
import com.kicktime.backend.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(@RequestBody RegisterRequestDTO request) {

        RegisterResponseDTO response = authService.register(request);

        return ResponseEntity.ok(response);
    }


}
