package com.kicktime.backend.domain.services;

import com.kicktime.backend.domain.model.Reservation;
import com.kicktime.backend.domain.model.TimeSlot;
import com.kicktime.backend.domain.model.enums.ReservationStatus;
import com.kicktime.backend.domain.model.enums.TimeSlotStatus;
import com.kicktime.backend.repository.ReservationRepository;
import com.kicktime.backend.repository.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
@EnableScheduling
@RequiredArgsConstructor
public class ReservationWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final TimeSlotRepository timeSlotRepository;
    private final ReservationRepository reservationRepository;

    // Timer de 2 minutos por franja bloqueada
    private static final long LOCK_TIMEOUT_SECONDS = 120;

    // Mapa de timers activos: timeSlotId -> ScheduledFuture
    private final Map<Long, ScheduledFuture<?>> activeTimers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    /**
     * Bloquea una franja y arranca el timer de expiración
     */
    public void lockTimeSlot(Long timeSlotId, Long reservationId) {
        TimeSlot timeSlot = timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new RuntimeException("TimeSlot not found"));

        timeSlot.setStatus(TimeSlotStatus.LOCKED);
        timeSlotRepository.save(timeSlot);

        // Notifica a todos los clientes suscritos
        notifyTimeSlotUpdate(timeSlotId, "LOCKED", reservationId);

        // Arranca el timer de expiración
        ScheduledFuture<?> timer = scheduler.schedule(
                () -> expireReservation(timeSlotId, reservationId),
                LOCK_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
        );

        activeTimers.put(timeSlotId, timer);
    }

    /**
     * Confirma la reserva — cancela el timer y marca como RESERVED
     */
    public void confirmReservation(Long timeSlotId, Long reservationId) {
        cancelTimer(timeSlotId);

        TimeSlot timeSlot = timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new RuntimeException("TimeSlot not found"));

        timeSlot.setStatus(TimeSlotStatus.RESERVED);
        timeSlotRepository.save(timeSlot);

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        reservation.setStatus(ReservationStatus.ACCEPTED);
        reservationRepository.save(reservation);

        notifyTimeSlotUpdate(timeSlotId, "RESERVED", reservationId);
    }

    /**
     * Libera la franja cuando el timer expira o se rechaza
     */
    public void releaseTimeSlot(Long timeSlotId, Long reservationId) {
        cancelTimer(timeSlotId);

        TimeSlot timeSlot = timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new RuntimeException("TimeSlot not found"));

        timeSlot.setStatus(TimeSlotStatus.AVAILABLE);
        timeSlotRepository.save(timeSlot);

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        reservation.setStatus(ReservationStatus.REJECTED);
        reservationRepository.save(reservation);

        notifyTimeSlotUpdate(timeSlotId, "AVAILABLE", reservationId);
    }

    /**
     * Expira automáticamente cuando el timer llega a cero
     */
    private void expireReservation(Long timeSlotId, Long reservationId) {
        TimeSlot timeSlot = timeSlotRepository.findById(timeSlotId).orElse(null);
        if (timeSlot == null || timeSlot.getStatus() != TimeSlotStatus.LOCKED) return;

        timeSlot.setStatus(TimeSlotStatus.AVAILABLE);
        timeSlotRepository.save(timeSlot);

        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        if (reservation != null) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);
        }

        activeTimers.remove(timeSlotId);
        notifyTimeSlotUpdate(timeSlotId, "EXPIRED", reservationId);
    }

    /**
     * Publica el cambio de estado al topic del torneo
     */
    private void notifyTimeSlotUpdate(Long timeSlotId, String status, Long reservationId) {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("timeSlotId", timeSlotId);
        payload.put("status", status);
        payload.put("reservationId", reservationId);
        payload.put("timestamp", LocalDateTime.now().toString());

        messagingTemplate.convertAndSend("/topic/timeslots/" + timeSlotId, (Object) payload);
    }

    private void cancelTimer(Long timeSlotId) {
        ScheduledFuture<?> timer = activeTimers.remove(timeSlotId);
        if (timer != null) {
            timer.cancel(false);
        }
    }

    /**
     * Resuelve conflicto cuando dos capitanes piden la misma franja LOCKED
     * Elige al azar entre la reserva existente y la nueva
     */
    public void resolveConflict(Long timeSlotId, Long existingReservationId, Long challengerReservationId) {
        boolean existingWins = new java.util.Random().nextBoolean();

        Long winnerId = existingWins ? existingReservationId : challengerReservationId;
        Long loserId = existingWins ? challengerReservationId : existingReservationId;

        // Rechaza al perdedor
        reservationRepository.findById(loserId).ifPresent(loser -> {
            loser.setStatus(ReservationStatus.REJECTED);
            reservationRepository.save(loser);
        });

        // Notifica el resultado a todos
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("timeSlotId", timeSlotId);
        payload.put("status", "LOCKED");
        payload.put("reservationId", winnerId);
        payload.put("conflictResolved", true);
        payload.put("winnerId", winnerId);
        payload.put("loserId", loserId);
        payload.put("timestamp", LocalDateTime.now().toString());

        messagingTemplate.convertAndSend("/topic/timeslots/" + timeSlotId, (Object) payload);
    }
}