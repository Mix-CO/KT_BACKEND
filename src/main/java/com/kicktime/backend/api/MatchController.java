package com.kicktime.backend.api;

import com.kicktime.backend.domain.model.dto.request.CreateMatchRequestDTO;
import com.kicktime.backend.domain.model.dto.request.RecordMatchResultRequestDTO;
import com.kicktime.backend.domain.model.dto.request.ScheduleMatchRequestDTO;
import com.kicktime.backend.domain.model.dto.response.MatchResponseDTO;
import com.kicktime.backend.domain.services.MatchService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @PostMapping
    public ResponseEntity<MatchResponseDTO> createMatch(
            @RequestBody CreateMatchRequestDTO request) {

        MatchResponseDTO response = matchService.createMatch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchResponseDTO> getMatch(
            @PathVariable Long id) {

        MatchResponseDTO response = matchService.getMatch(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<List<MatchResponseDTO>> getMatchesByTournament(
            @PathVariable Long tournamentId) {

        List<MatchResponseDTO> response = matchService.getMatchesByTournament(tournamentId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/schedule")
    public ResponseEntity<MatchResponseDTO> scheduleMatch(
            @PathVariable Long id,
            @RequestBody ScheduleMatchRequestDTO request) {

        MatchResponseDTO response = matchService.scheduleMatch(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/result")
    public ResponseEntity<MatchResponseDTO> recordMatchResult(
            @PathVariable Long id,
            @RequestBody RecordMatchResultRequestDTO request) {

        MatchResponseDTO response = matchService.recordMatchResult(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMatch(
            @PathVariable Long id) {

        matchService.deleteMatch(id);
        return ResponseEntity.noContent().build();
    }
}
