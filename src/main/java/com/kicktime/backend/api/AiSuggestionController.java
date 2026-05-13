package com.kicktime.backend.api;

import com.kicktime.backend.domain.model.dto.response.AiSuggestionResponseDTO;
import com.kicktime.backend.domain.services.AiSuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiSuggestionController {

    private final AiSuggestionService aiSuggestionService;

    @GetMapping("/suggest/{matchId}")
    public ResponseEntity<AiSuggestionResponseDTO> suggestTimeSlot(
            @PathVariable Long matchId) {
        return ResponseEntity.ok(aiSuggestionService.suggestTimeSlot(matchId));
    }
}
