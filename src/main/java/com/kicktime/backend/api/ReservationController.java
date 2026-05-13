package com.kicktime.backend.api;

import com.kicktime.backend.domain.model.dto.request.CreateReservationRequestDTO;
import com.kicktime.backend.domain.model.dto.request.UpdateReservationStatusRequestDTO;
import com.kicktime.backend.domain.model.dto.response.ReservationResponseDTO;
import com.kicktime.backend.domain.services.ReservationService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationResponseDTO> createReservation(
            @RequestBody CreateReservationRequestDTO request) {

        ReservationResponseDTO response = reservationService.createReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/match/{matchId}")
    public ResponseEntity<List<ReservationResponseDTO>> getReservationsForMatch(
            @PathVariable Long matchId) {

        List<ReservationResponseDTO> response = reservationService.getReservationsForMatch(matchId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ReservationResponseDTO> updateReservationStatus(
            @PathVariable Long id,
            @RequestBody UpdateReservationStatusRequestDTO request) {

        ReservationResponseDTO response = reservationService.updateReservationStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(
            @PathVariable Long id) {

        reservationService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }
}