package com.kicktime.backend.api;

import com.kicktime.backend.domain.model.dto.response.AiSuggestionResponseDTO;
import com.kicktime.backend.domain.services.AiSuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiSuggestionController {

    private final AiSuggestionService aiSuggestionService;

    @GetMapping("/suggest/{matchId}")
    public ResponseEntity<?> suggestTimeSlot(@PathVariable Long matchId) {
        try {
            return ResponseEntity.ok(aiSuggestionService.suggestTimeSlot(matchId));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Match not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Partido no encontrado con id: " + matchId);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al generar sugerencia de IA: " + e.getMessage());
        }
    }
}