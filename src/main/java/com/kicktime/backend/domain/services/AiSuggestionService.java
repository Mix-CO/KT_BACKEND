package com.kicktime.backend.domain.services;

import com.kicktime.backend.domain.model.Availability;
import com.kicktime.backend.domain.model.Match;
import com.kicktime.backend.domain.model.TimeSlot;
import com.kicktime.backend.domain.model.User;
import com.kicktime.backend.domain.model.dto.response.AiSuggestionResponseDTO;
import com.kicktime.backend.repository.AvailabilityRepository;
import com.kicktime.backend.repository.MatchRepository;
import com.kicktime.backend.repository.TimeSlotRepository;
import com.kicktime.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AiSuggestionService {

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final AvailabilityRepository availabilityRepository;
    private final TimeSlotRepository timeSlotRepository;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash:generateContent?key=";

    public AiSuggestionResponseDTO suggestTimeSlot(Long matchId) {

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        List<User> homePlayers = userRepository.findByTeam_Id(match.getHomeTeam().getId());
        List<User> awayPlayers = userRepository.findByTeam_Id(match.getAwayTeam().getId());

        List<TimeSlot> allSlots = timeSlotRepository.findAll();

        Map<Long, Integer> homeAvailCount = new HashMap<>();
        Map<Long, Integer> awayAvailCount = new HashMap<>();

        for (User player : homePlayers) {
            List<Availability> avails = availabilityRepository.findByUser_Id(player.getId());
            for (Availability a : avails) {
                homeAvailCount.merge(a.getTimeSlot().getId(), 1, Integer::sum);
            }
        }

        for (User player : awayPlayers) {
            List<Availability> avails = availabilityRepository.findByUser_Id(player.getId());
            for (Availability a : avails) {
                awayAvailCount.merge(a.getTimeSlot().getId(), 1, Integer::sum);
            }
        }

        StringBuilder availabilitySummary = new StringBuilder();
        for (TimeSlot slot : allSlots) {
            int home = homeAvailCount.getOrDefault(slot.getId(), 0);
            int away = awayAvailCount.getOrDefault(slot.getId(), 0);
            if (home > 0 || away > 0) {
                availabilitySummary.append(String.format(
                        "- Slot ID %d: %s %s-%s | %s: %d jugadores | %s: %d jugadores%n",
                        slot.getId(),
                        slot.getDayOfWeek(),
                        slot.getStart(),
                        slot.getEnd(),
                        match.getHomeTeam().getName(), home,
                        match.getAwayTeam().getName(), away
                ));
            }
        }

        String prompt = String.format("""
            Eres un asistente de programación de torneos universitarios de fútbol.
            
            Debes sugerir el mejor horario para el siguiente partido:
            - Equipo local: %s (%d jugadores en total)
            - Equipo visitante: %s (%d jugadores en total)
            
            Disponibilidad de jugadores por franja horaria:
            %s
            
            Selecciona el Slot ID que maximice la asistencia combinada de ambos equipos.
            
            Responde ÚNICAMENTE en este formato JSON, sin texto adicional:
            {
              "slotId": <número>,
              "explanation": "<explicación breve en español de por qué este horario es el mejor>"
            }
            """,
                match.getHomeTeam().getName(), homePlayers.size(),
                match.getAwayTeam().getName(), awayPlayers.size(),
                availabilitySummary.toString()
        );

        RestTemplate restTemplate = new RestTemplate();
        String url = GEMINI_URL + geminiApiKey;

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        String rawText = "";
        try {
            List candidates = (List) response.getBody().get("candidates");
            Map candidate = (Map) candidates.get(0);
            Map content = (Map) candidate.get("content");
            List parts = (List) content.get("parts");
            Map part = (Map) parts.get(0);
            rawText = (String) part.get("text");

            rawText = rawText.replaceAll("```json", "").replaceAll("```", "").trim();
            rawText = rawText.replaceAll("\\s+", " ");

            Long slotId = Long.parseLong(rawText.replaceAll(".*\"slotId\":\\s*(\\d+).*", "$1"));
            String explanation = rawText.replaceAll(".*\"explanation\":\\s*\"([^\"]+)\".*", "$1");

            return AiSuggestionResponseDTO.builder()
                    .matchId(matchId)
                    .homeTeamName(match.getHomeTeam().getName())
                    .awayTeamName(match.getAwayTeam().getName())
                    .suggestedTimeSlotId(slotId)
                    .explanation(explanation)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Error al procesar respuesta de Gemini: " + e.getMessage(), e);
        }
    }
}