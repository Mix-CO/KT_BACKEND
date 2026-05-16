package com.kicktime.backend.domain.services;

import com.kicktime.backend.domain.model.Reservation;
import com.kicktime.backend.domain.model.TimeSlot;
import com.kicktime.backend.domain.model.enums.ReservationStatus;
import com.kicktime.backend.domain.model.enums.TimeSlotStatus;
import com.kicktime.backend.repository.ReservationRepository;
import com.kicktime.backend.repository.TimeSlotRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationWebSocketServiceTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private TimeSlotRepository timeSlotRepository;
    @Mock private ReservationRepository reservationRepository;

    @InjectMocks
    private ReservationWebSocketService reservationWebSocketService;

    private TimeSlot timeSlot;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        timeSlot = TimeSlot.builder()
                .id(10L)
                .status(TimeSlotStatus.AVAILABLE)
                .build();

        reservation = Reservation.builder()
                .id(100L)
                .status(ReservationStatus.PENDING)
                .build();
    }

    @Test
    void lockTimeSlot_setsStatusToLocked_andSaves() {
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(timeSlot));

        reservationWebSocketService.lockTimeSlot(10L, 100L);

        assertThat(timeSlot.getStatus()).isEqualTo(TimeSlotStatus.LOCKED);
        verify(timeSlotRepository).save(timeSlot);
    }

    @Test
    void lockTimeSlot_registersTimerInActiveTimers() throws Exception {
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(timeSlot));

        reservationWebSocketService.lockTimeSlot(10L, 100L);

        java.lang.reflect.Field field =
                ReservationWebSocketService.class.getDeclaredField("activeTimers");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Long, ScheduledFuture<?>> activeTimers =
                (Map<Long, ScheduledFuture<?>>) field.get(reservationWebSocketService);

        assertThat(activeTimers).containsKey(10L);
    }

    @Test
    void lockTimeSlot_sendsWebSocketNotification_withLockedStatus() {
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(timeSlot));

        reservationWebSocketService.lockTimeSlot(10L, 100L);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/timeslots/10"),
                payloadCaptor.capture()
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
        assertThat(payload.get("status")).isEqualTo("LOCKED");
        assertThat(payload.get("timeSlotId")).isEqualTo(10L);
        assertThat(payload.get("reservationId")).isEqualTo(100L);
    }

    @Test
    void lockTimeSlot_timeSlotNotFound_throwsException() {
        when(timeSlotRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationWebSocketService.lockTimeSlot(99L, 100L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Time slot not found");

        verify(timeSlotRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    // ─── confirmReservation ───────────────────────────────────────────────────

    @Test
    void confirmReservation_setsTimeSlotToReserved_andSaves() {
        timeSlot.setStatus(TimeSlotStatus.LOCKED);
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(timeSlot));
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        reservationWebSocketService.confirmReservation(10L, 100L);

        assertThat(timeSlot.getStatus()).isEqualTo(TimeSlotStatus.RESERVED);
        verify(timeSlotRepository).save(timeSlot);
    }

    @Test
    void confirmReservation_setsReservationToAccepted_andSaves() {
        timeSlot.setStatus(TimeSlotStatus.LOCKED);
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(timeSlot));
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        reservationWebSocketService.confirmReservation(10L, 100L);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACCEPTED);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void confirmReservation_sendsWebSocketNotification_withReservedStatus() {
        timeSlot.setStatus(TimeSlotStatus.LOCKED);
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(timeSlot));
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        reservationWebSocketService.confirmReservation(10L, 100L);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/timeslots/10"),
                payloadCaptor.capture()
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
        assertThat(payload.get("status")).isEqualTo("RESERVED");
    }

    @Test
    void confirmReservation_timeSlotNotFound_throwsException() {
        when(timeSlotRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationWebSocketService.confirmReservation(99L, 100L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Time slot not found");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void confirmReservation_reservationNotFound_throwsException() {
        timeSlot.setStatus(TimeSlotStatus.LOCKED);
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(timeSlot));
        when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationWebSocketService.confirmReservation(10L, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Reservation not found");

        verify(timeSlotRepository).save(timeSlot);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void releaseTimeSlot_setsTimeSlotToAvailable_andSaves() {
        timeSlot.setStatus(TimeSlotStatus.LOCKED);
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(timeSlot));
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        reservationWebSocketService.releaseTimeSlot(10L, 100L);

        assertThat(timeSlot.getStatus()).isEqualTo(TimeSlotStatus.AVAILABLE);
        verify(timeSlotRepository).save(timeSlot);
    }

    @Test
    void releaseTimeSlot_setsReservationToRejected_andSaves() {
        timeSlot.setStatus(TimeSlotStatus.LOCKED);
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(timeSlot));
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        reservationWebSocketService.releaseTimeSlot(10L, 100L);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.REJECTED);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void releaseTimeSlot_sendsWebSocketNotification_withAvailableStatus() {
        timeSlot.setStatus(TimeSlotStatus.LOCKED);
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(timeSlot));
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        reservationWebSocketService.releaseTimeSlot(10L, 100L);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/timeslots/10"),
                payloadCaptor.capture()
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
        assertThat(payload.get("status")).isEqualTo("AVAILABLE");
    }

    @Test
    void releaseTimeSlot_timeSlotNotFound_throwsException() {
        when(timeSlotRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationWebSocketService.releaseTimeSlot(99L, 100L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Time slot not found");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void releaseTimeSlot_reservationNotFound_throwsException() {
        timeSlot.setStatus(TimeSlotStatus.LOCKED);
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(timeSlot));
        when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationWebSocketService.releaseTimeSlot(10L, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Reservation not found");

        verify(timeSlotRepository).save(timeSlot);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void expireReservation_timeSlotNotLocked_doesNothing() throws Exception {
        timeSlot.setStatus(TimeSlotStatus.AVAILABLE);
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(timeSlot));

        Method method = ReservationWebSocketService.class
                .getDeclaredMethod("expireReservation", Long.class, Long.class);
        method.setAccessible(true);
        method.invoke(reservationWebSocketService, 10L, 100L);

        verify(timeSlotRepository, never()).save(any());
        verify(reservationRepository, never()).findById(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void expireReservation_setsAvailableAndExpired_andNotifies() throws Exception {
        timeSlot.setStatus(TimeSlotStatus.LOCKED);
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(timeSlot));
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        Method method = ReservationWebSocketService.class
                .getDeclaredMethod("expireReservation", Long.class, Long.class);
        method.setAccessible(true);
        method.invoke(reservationWebSocketService, 10L, 100L);

        assertThat(timeSlot.getStatus()).isEqualTo(TimeSlotStatus.AVAILABLE);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);

        verify(timeSlotRepository).save(timeSlot);
        verify(reservationRepository).save(reservation);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/timeslots/10"),
                payloadCaptor.capture()
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
        assertThat(payload.get("status")).isEqualTo("EXPIRED");
    }

    @Test
    void lockTimeSlot_timeSlotLocked_noPreviousCoinFlip_callsResolveCoinFlip() {
        timeSlot.setStatus(TimeSlotStatus.LOCKED);
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(timeSlot));

        Reservation currentReservation = Reservation.builder()
                .id(99L)
                .status(ReservationStatus.PENDING)
                .build();

        when(reservationRepository.findByTimeSlotIdAndStatus(10L, ReservationStatus.PENDING))
                .thenReturn(java.util.List.of(currentReservation));
        lenient().when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        reservationWebSocketService.lockTimeSlot(10L, 100L);

        verify(messagingTemplate, atLeast(1)).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void lockTimeSlot_timeSlotLocked_coinFlipAlreadyResolved_rejectsNewReservation() throws Exception {
        timeSlot.setStatus(TimeSlotStatus.LOCKED);
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(timeSlot));
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        // Simular que ya hubo coin flip agregando el timeSlotId al set
        java.lang.reflect.Field field =
                ReservationWebSocketService.class.getDeclaredField("coinFlipResolved");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Set<Long> coinFlipResolved =
                (java.util.Set<Long>) field.get(reservationWebSocketService);
        coinFlipResolved.add(10L);

        reservationWebSocketService.lockTimeSlot(10L, 100L);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.REJECTED);
        verify(reservationRepository).save(reservation);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/timeslots/10"),
                payloadCaptor.capture()
        );
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
        assertThat(payload.get("status")).isEqualTo("COIN_FLIP_LOST");
    }

    @Test
    void lockTimeSlot_timeSlotReserved_doesNothing() {
        timeSlot.setStatus(TimeSlotStatus.RESERVED);
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(timeSlot));

        reservationWebSocketService.lockTimeSlot(10L, 100L);

        verify(timeSlotRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void resolveCoinFlip_newReservationWins_rejectsCurrentAndNotifies() throws Exception {
        timeSlot.setStatus(TimeSlotStatus.LOCKED);

        Reservation currentReservation = Reservation.builder()
                .id(99L)
                .status(ReservationStatus.PENDING)
                .build();

        ReservationWebSocketService spy = spy(reservationWebSocketService);
        doReturn(true).when(spy).coinFlip();

        injectField(spy, "timeSlotRepository", timeSlotRepository);
        injectField(spy, "reservationRepository", reservationRepository);
        injectField(spy, "messagingTemplate", messagingTemplate);

        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(timeSlot));
        when(reservationRepository.findByTimeSlotIdAndStatus(10L, ReservationStatus.PENDING))
                .thenReturn(java.util.List.of(currentReservation));
        lenient().when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        spy.lockTimeSlot(10L, 100L);

        assertThat(currentReservation.getStatus()).isEqualTo(ReservationStatus.REJECTED);
        verify(reservationRepository).save(currentReservation);
    }

    @Test
    void resolveCoinFlip_currentReservationWins_rejectsNewAndNotifies() throws Exception {
        timeSlot.setStatus(TimeSlotStatus.LOCKED);
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(timeSlot));

        Reservation currentReservation = Reservation.builder()
                .id(99L)
                .status(ReservationStatus.PENDING)
                .build();

        when(reservationRepository.findByTimeSlotIdAndStatus(10L, ReservationStatus.PENDING))
                .thenReturn(java.util.List.of(currentReservation));
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        ReservationWebSocketService spy = spy(reservationWebSocketService);
        doReturn(false).when(spy).coinFlip();

        injectField(spy, "timeSlotRepository", timeSlotRepository);
        injectField(spy, "reservationRepository", reservationRepository);
        injectField(spy, "messagingTemplate", messagingTemplate);

        spy.lockTimeSlot(10L, 100L);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.REJECTED);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void resolveCoinFlip_noPendingReservation_treatsAsAvailable() throws Exception {
        timeSlot.setStatus(TimeSlotStatus.LOCKED);

        TimeSlot freshSlot = TimeSlot.builder()
                .id(10L)
                .status(TimeSlotStatus.LOCKED)
                .build();

        when(timeSlotRepository.findById(10L))
                .thenReturn(Optional.of(freshSlot));
        when(reservationRepository.findByTimeSlotIdAndStatus(10L, ReservationStatus.PENDING))
                .thenReturn(java.util.List.of());

        // Segunda llamada a findById en lockTimeSlot recursivo — franja ya AVAILABLE
        TimeSlot availableSlot = TimeSlot.builder()
                .id(10L)
                .status(TimeSlotStatus.AVAILABLE)
                .build();
        when(timeSlotRepository.findById(10L))
                .thenReturn(Optional.of(freshSlot))
                .thenReturn(Optional.of(availableSlot));

        reservationWebSocketService.lockTimeSlot(10L, 100L);

        verify(timeSlotRepository, atLeast(1)).save(any());
    }

    @Test
    void coinFlip_returnsBoolean() {
        boolean result = reservationWebSocketService.coinFlip();
        assertThat(result).isIn(true, false);
    }

    private void injectField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field =
                ReservationWebSocketService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
