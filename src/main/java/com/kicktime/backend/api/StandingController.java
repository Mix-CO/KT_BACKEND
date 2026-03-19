package com.kicktime.backend.api;

import com.kicktime.backend.domain.model.dto.request.InitializeStandingsRequestDTO;
import com.kicktime.backend.domain.model.dto.response.StandingResponseDTO;
import com.kicktime.backend.domain.services.StandingService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/standings")
@RequiredArgsConstructor
public class StandingController {

    private final StandingService standingService;

    @PostMapping("/initialize")
    public ResponseEntity<Void> initializeStandings(
            @RequestBody InitializeStandingsRequestDTO request) {

        standingService.initializeStandings(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<List<StandingResponseDTO>> getStandingsByTournament(
            @PathVariable Long tournamentId) {

        List<StandingResponseDTO> response = standingService.getStandingsByTournament(tournamentId);
        return ResponseEntity.ok(response);
    }
}