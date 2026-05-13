package com.kicktime.backend.api;

import com.kicktime.backend.domain.model.dto.request.CreateReservationRequestDTO;
import com.kicktime.backend.domain.model.dto.request.RespondReservationDTO;
import com.kicktime.backend.domain.model.dto.request.UpdateReservationStatusRequestDTO;
import com.kicktime.backend.domain.services.ReservationService;
import com.kicktime.backend.domain.services.ReservationWebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ReservationWebSocketController {

    private final ReservationService reservationService;
    private final ReservationWebSocketService reservationWebSocketService;

    /**
     * Cliente envía: /app/reservation/request
     * Bloquea la franja y notifica a todos
     */
    @MessageMapping("/reservation/request")
    public void requestReservation(@Payload CreateReservationRequestDTO request) {
        reservationService.createReservation(request);
    }

    /**
     * Cliente envía: /app/reservation/respond
     * Capitán rival confirma o rechaza
     */
    @MessageMapping("/reservation/respond")
    public void respondToReservation(@Payload RespondReservationDTO request) {
        UpdateReservationStatusRequestDTO dto = new UpdateReservationStatusRequestDTO();
        dto.setStatus(request.getStatus());
        dto.setRespondingUserId(request.getRespondingUserId());
        reservationService.updateReservationStatus(request.getReservationId(), dto);
    }
}
