package com.kicktime.backend.api;

import com.kicktime.backend.domain.model.dto.request.CreateTournamentRequestDTO;
import com.kicktime.backend.domain.model.dto.request.UpdateTournamentStatusRequestDTO;
import com.kicktime.backend.domain.model.dto.response.TeamResponseDTO;
import com.kicktime.backend.domain.model.dto.response.TournamentResponseDTO;
import com.kicktime.backend.domain.services.TournamentService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;

    @PostMapping
    public ResponseEntity<TournamentResponseDTO> createTournament(
            @RequestBody CreateTournamentRequestDTO request) {

        TournamentResponseDTO response = tournamentService.createTournament(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TournamentResponseDTO> getTournament(
            @PathVariable Long id) {

        TournamentResponseDTO response = tournamentService.getTournament(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<TournamentResponseDTO>> getAllTournaments() {

        List<TournamentResponseDTO> response = tournamentService.getAllTournaments();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TournamentResponseDTO> updateTournamentStatus(
            @PathVariable Long id,
            @RequestBody UpdateTournamentStatusRequestDTO request) {

        TournamentResponseDTO response = tournamentService.updateTournamentStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/teams")
    public ResponseEntity<List<TeamResponseDTO>> getTeamsInTournament(
            @PathVariable Long id) {

        List<TeamResponseDTO> response = tournamentService.getTeamsInTournament(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTournament(
            @PathVariable Long id) {

        tournamentService.deleteTournament(id);
        return ResponseEntity.noContent().build();
    }
}
