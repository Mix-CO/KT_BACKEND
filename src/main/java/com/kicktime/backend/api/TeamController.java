package com.kicktime.backend.api;

import com.kicktime.backend.domain.model.dto.request.CreateTeamRequestDTO;
import com.kicktime.backend.domain.model.dto.request.PlayerCreateDTO;
import com.kicktime.backend.domain.model.dto.response.TeamResponseDTO;
import com.kicktime.backend.domain.services.TeamService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    public ResponseEntity<TeamResponseDTO> createTeam(
            @RequestBody CreateTeamRequestDTO request,
            @RequestParam Long creatorUserId) {

        TeamResponseDTO response = teamService.createTeam(request, creatorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeamResponseDTO> getTeam(
            @PathVariable Long id) {

        TeamResponseDTO response = teamService.getTeam(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<TeamResponseDTO>> getAllTeams() {

        List<TeamResponseDTO> response = teamService.getAllTeams();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/players")
    public ResponseEntity<TeamResponseDTO> addPlayerToTeam(
            @PathVariable Long id,
            @RequestBody PlayerCreateDTO playerDTO) {

        TeamResponseDTO response = teamService.addPlayerToTeam(id, playerDTO);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/captain")
    public ResponseEntity<TeamResponseDTO> changeCaptain(
            @PathVariable Long id,
            @RequestParam Long userId) {

        TeamResponseDTO response = teamService.changeCaptain(id, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(
            @PathVariable Long id) {

        teamService.deleteTeam(id);
        return ResponseEntity.noContent().build();
    }
}
