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
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private static final long LOCK_TIMEOUT_SECONDS = 120;

    private final Map<Long, ScheduledFuture<?>> activeTimers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    private final Set<Long> coinFlipResolved = ConcurrentHashMap.newKeySet();

    private static final String TIME_SLOT_NOT_FOUND = "Time slot not found";
    private static final String RESERVATION_NOT_FOUND = "Reservation not found";
    private static final String COIN_FLIP_LOST = "COIN_FLIP_LOST";

    private static final java.util.Random RANDOM = new java.util.Random();

    public void lockTimeSlot(Long timeSlotId, Long reservationId) {
        TimeSlot timeSlot = timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new RuntimeException(TIME_SLOT_NOT_FOUND));

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
            if (coinFlipResolved.contains(timeSlotId)) {
                // Ya hubo coin flip — rechazar directo sin moneda
                Reservation newReservation = reservationRepository.findById(reservationId)
                        .orElseThrow(() -> new RuntimeException(RESERVATION_NOT_FOUND));
                newReservation.setStatus(ReservationStatus.REJECTED);
                reservationRepository.save(newReservation);
                notifyTimeSlotUpdate(timeSlotId, COIN_FLIP_LOST, reservationId);
            } else {
                // Primera apelación — coin flip
                coinFlipResolved.add(timeSlotId);
                resolveCoinFlip(timeSlotId, reservationId);
            }
        }
    }

    private void resolveCoinFlip(Long timeSlotId, Long newReservationId) {
        List<Reservation> pendingReservations = reservationRepository
                .findByTimeSlotIdAndStatus(timeSlotId, ReservationStatus.PENDING);

        Reservation currentReservation = pendingReservations.isEmpty() ? null : pendingReservations.get(0);

        if (currentReservation == null) {
            TimeSlot timeSlot = timeSlotRepository.findById(timeSlotId)
                    .orElseThrow(() -> new RuntimeException(TIME_SLOT_NOT_FOUND));
            timeSlot.setStatus(TimeSlotStatus.AVAILABLE);
            timeSlotRepository.save(timeSlot);
            coinFlipResolved.remove(timeSlotId);
            lockTimeSlot(timeSlotId, newReservationId);
            return;
        }

        boolean newReservationWins = coinFlip();

        if (newReservationWins) {
            currentReservation.setStatus(ReservationStatus.REJECTED);
            reservationRepository.save(currentReservation);

            cancelTimer(timeSlotId);

            notifyTimeSlotUpdate(timeSlotId, COIN_FLIP_LOST, currentReservation.getId());

            ScheduledFuture<?> timer = scheduler.schedule(
                    () -> expireReservation(timeSlotId, newReservationId),
                    LOCK_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            );
            activeTimers.put(timeSlotId, timer);

            notifyTimeSlotUpdate(timeSlotId, "COIN_FLIP_WON", newReservationId);

        } else {
            Reservation newReservation = reservationRepository.findById(newReservationId)
                    .orElseThrow(() -> new RuntimeException(RESERVATION_NOT_FOUND));

            newReservation.setStatus(ReservationStatus.REJECTED);
            reservationRepository.save(newReservation);

            notifyTimeSlotUpdate(timeSlotId, COIN_FLIP_LOST, newReservationId);
            notifyTimeSlotUpdate(timeSlotId, "COIN_FLIP_WON", currentReservation.getId());
        }
    }

    boolean coinFlip() {
        return RANDOM.nextBoolean();
    }

    public void confirmReservation(Long timeSlotId, Long reservationId) {
        cancelTimer(timeSlotId);
        coinFlipResolved.remove(timeSlotId);

        TimeSlot timeSlot = timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new RuntimeException(TIME_SLOT_NOT_FOUND));

        timeSlot.setStatus(TimeSlotStatus.RESERVED);
        timeSlotRepository.save(timeSlot);

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException(RESERVATION_NOT_FOUND));

        reservation.setStatus(ReservationStatus.ACCEPTED);
        reservationRepository.save(reservation);

        notifyTimeSlotUpdate(timeSlotId, "RESERVED", reservationId);
    }

    public void releaseTimeSlot(Long timeSlotId, Long reservationId) {
        cancelTimer(timeSlotId);
        coinFlipResolved.remove(timeSlotId);

        TimeSlot timeSlot = timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new RuntimeException(TIME_SLOT_NOT_FOUND));

        timeSlot.setStatus(TimeSlotStatus.AVAILABLE);
        timeSlotRepository.save(timeSlot);

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException(RESERVATION_NOT_FOUND));

        reservation.setStatus(ReservationStatus.REJECTED);
        reservationRepository.save(reservation);

        notifyTimeSlotUpdate(timeSlotId, "AVAILABLE", reservationId);
    }

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
        coinFlipResolved.remove(timeSlotId);
        notifyTimeSlotUpdate(timeSlotId, "EXPIRED", reservationId);
    }

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