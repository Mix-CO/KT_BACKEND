package com.kicktime.backend.api;

import com.kicktime.backend.domain.model.dto.request.CreateAvailabilityRequestDTO;
import com.kicktime.backend.domain.model.dto.response.AvailabilityResponseDTO;
import com.kicktime.backend.domain.services.AvailabilityService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @PostMapping
    public ResponseEntity<AvailabilityResponseDTO> createAvailability(
            @RequestBody CreateAvailabilityRequestDTO request) {

        AvailabilityResponseDTO response = availabilityService.createAvailability(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AvailabilityResponseDTO>> getUserAvailability(
            @PathVariable Long userId) {

        List<AvailabilityResponseDTO> response = availabilityService.getUserAvailability(userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAvailability(
            @PathVariable Long id) {

        availabilityService.deleteAvailability(id);
        return ResponseEntity.noContent().build();
    }
}