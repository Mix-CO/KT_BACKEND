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
import java.util.List;

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

    private static final String TIME_SLOT_NOT_FOUND = "Time slot not found";

    /**
     * Bloquea una franja y arranca el timer de expiración.
     * Si la franja ya está LOCKED, lanza una moneda para decidir quién espera.
     */
    public void lockTimeSlot(Long timeSlotId, Long reservationId) {
        TimeSlot timeSlot = timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new RuntimeException(TIME_SLOT_NOT_FOUND));

        // Franja disponible — flujo normal
        if (timeSlot.getStatus() == TimeSlotStatus.AVAILABLE) {
            timeSlot.setStatus(TimeSlotStatus.LOCKED);
            timeSlotRepository.save(timeSlot);
            notifyTimeSlotUpdate(timeSlotId, "LOCKED", reservationId);

            ScheduledFuture<?> timer = scheduler.schedule(
                    () -> expireReservation(timeSlotId, reservationId),
                    LOCK_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            );
            activeTimers.put(timeSlotId, timer);

        } else if (timeSlot.getStatus() == TimeSlotStatus.LOCKED) {
            // Franja ya bloqueada — coin flip
            resolveCoinFlip(timeSlotId, reservationId);
        }
        // Si está RESERVED no hacemos nada — ya tiene dueño
    }

    /**
     * Lanza una moneda para decidir entre la reserva actual y la nueva.
     * El ganador queda como PENDING, el perdedor queda REJECTED.
     */
    private void resolveCoinFlip(Long timeSlotId, Long newReservationId) {
        // Buscar la reserva activa actual para esta franja
        List<Reservation> pendingReservations = reservationRepository
                .findByTimeSlotIdAndStatus(timeSlotId, ReservationStatus.PENDING);

        Reservation currentReservation = pendingReservations.isEmpty() ? null : pendingReservations.get(0);

        if (currentReservation == null) {
            // No hay reserva activa, tratar como franja disponible
            TimeSlot timeSlot = timeSlotRepository.findById(timeSlotId)
                    .orElseThrow(() -> new RuntimeException(TIME_SLOT_NOT_FOUND));
            timeSlot.setStatus(TimeSlotStatus.AVAILABLE);
            timeSlotRepository.save(timeSlot);
            lockTimeSlot(timeSlotId, newReservationId);
            return;
        }

        boolean newReservationWins = coinFlip();

        if (newReservationWins) {
            // Nueva reserva gana — rechazar la actual y bloquear para la nueva
            currentReservation.setStatus(ReservationStatus.REJECTED);
            reservationRepository.save(currentReservation);

            // Cancelar el timer de la reserva anterior
            cancelTimer(timeSlotId);

            // Notificar al perdedor
            notifyTimeSlotUpdate(timeSlotId, "COIN_FLIP_LOST", currentReservation.getId());

            // Arrancar nuevo timer para la nueva reserva
            ScheduledFuture<?> timer = scheduler.schedule(
                    () -> expireReservation(timeSlotId, newReservationId),
                    LOCK_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            );
            activeTimers.put(timeSlotId, timer);

            // Notificar al ganador
            notifyTimeSlotUpdate(timeSlotId, "COIN_FLIP_WON", newReservationId);

        } else {
            // Reserva actual gana — rechazar la nueva inmediatamente
            Reservation newReservation = reservationRepository.findById(newReservationId)
                    .orElseThrow(() -> new RuntimeException("Reservation not found"));

            newReservation.setStatus(ReservationStatus.REJECTED);
            reservationRepository.save(newReservation);

            // Notificar al perdedor
            notifyTimeSlotUpdate(timeSlotId, "COIN_FLIP_LOST", newReservationId);

            // Notificar al ganador que sigue esperando
            notifyTimeSlotUpdate(timeSlotId, "COIN_FLIP_WON", currentReservation.getId());
        }
    }

    /**
     * Lanza la moneda — 50/50
     */
    boolean coinFlip() {
        return new java.util.Random().nextBoolean();
    }

    /**
     * Confirma la reserva — cancela el timer y marca como RESERVED
     */
    public void confirmReservation(Long timeSlotId, Long reservationId) {
        cancelTimer(timeSlotId);

        TimeSlot timeSlot = timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new RuntimeException(TIME_SLOT_NOT_FOUND));

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
                .orElseThrow(() -> new RuntimeException(TIME_SLOT_NOT_FOUND));

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
}