package com.kicktime.backend.domain.services;

import com.kicktime.backend.domain.model.*;
import com.kicktime.backend.domain.model.enums.MatchStatus;
import com.kicktime.backend.domain.model.enums.ReservationStatus;
import com.kicktime.backend.domain.model.dto.request.CreateReservationRequestDTO;
import com.kicktime.backend.domain.model.dto.request.UpdateReservationStatusRequestDTO;
import com.kicktime.backend.domain.model.dto.response.ReservationResponseDTO;

import com.kicktime.backend.domain.model.enums.TimeSlotStatus;
import com.kicktime.backend.repository.*;
import com.kicktime.backend.util.mappers.ReservationMapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final MatchRepository matchRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final UserRepository userRepository;
    private final ReservationMapper reservationMapper;
    private final ReservationWebSocketService reservationWebSocketService;

    /**
     * Create reservation
     */
    public ReservationResponseDTO createReservation(CreateReservationRequestDTO request) {

        Match match = matchRepository.findById(request.getMatchId())
                .orElseThrow(() -> new RuntimeException("Match not found"));

        TimeSlot timeSlot = timeSlotRepository.findById(request.getTimeSlotId())
                .orElseThrow(() -> new RuntimeException("TimeSlot not found"));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isHomeCaptain = match.getHomeTeam().getCaptain().getId().equals(user.getId());
        boolean isAwayCaptain = match.getAwayTeam().getCaptain().getId().equals(user.getId());

        if (!isHomeCaptain && !isAwayCaptain) {
            throw new RuntimeException("Only team captains can create reservations");
        }

        // Permitir AVAILABLE y LOCKED — si está LOCKED el WebSocket resuelve con coin flip
        if (timeSlot.getStatus() == TimeSlotStatus.RESERVED) {
            throw new RuntimeException("TimeSlot is already reserved");
        }

        Reservation reservation = Reservation.builder()
                .match(match)
                .timeSlot(timeSlot)
                .proposedBy(user)
                .status(ReservationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);
        reservationWebSocketService.lockTimeSlot(timeSlot.getId(), savedReservation.getId());

        return reservationMapper.toDTO(savedReservation);
    }

    /**
     * Get reservations for match
     */
    public List<ReservationResponseDTO> getReservationsForMatch(Long matchId) {

        List<Reservation> reservations = reservationRepository.findByMatchId(matchId);

        return reservations.stream()
                .map(reservationMapper::toDTO)
                .toList();
    }

    /**
     * Update reservation status (accept / reject)
     */
    public ReservationResponseDTO updateReservationStatus(Long reservationId, UpdateReservationStatusRequestDTO request) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        Match match = reservation.getMatch();
        User proposedBy = reservation.getProposedBy();

        User rivalCaptain;
        if (match.getHomeTeam().getCaptain().getId().equals(proposedBy.getId())) {
            rivalCaptain = match.getAwayTeam().getCaptain();
        } else {
            rivalCaptain = match.getHomeTeam().getCaptain();
        }

        if (!request.getRespondingUserId().equals(rivalCaptain.getId())) {
            throw new RuntimeException("Only the rival captain can confirm or reject this reservation");
        }

        reservation.setStatus(request.getStatus());
        Reservation updatedReservation = reservationRepository.save(reservation);

        /**
         * If accepted -> schedule match
         */
        if (request.getStatus() == ReservationStatus.ACCEPTED) {
            reservationWebSocketService.confirmReservation(
                    reservation.getTimeSlot().getId(),
                    reservationId
            );

            match.setTimeSlot(reservation.getTimeSlot());
            match.setStatus(MatchStatus.CONFIRMED);
            matchRepository.save(match);

            /**
             * Reject other reservations for same match
             */
            List<Reservation> otherReservations =
                    reservationRepository.findByMatchId(match.getId());
            for (Reservation r : otherReservations) {
                if (!r.getId().equals(reservationId)) {
                    r.setStatus(ReservationStatus.REJECTED);
                    reservationRepository.save(r);
                }
            }
        } else if (request.getStatus() == ReservationStatus.REJECTED) {
            reservationWebSocketService.releaseTimeSlot(
                    reservation.getTimeSlot().getId(),
                    reservationId
            );
        }

        return reservationMapper.toDTO(updatedReservation);
    }

    /**
     * Delete reservation
     */
    public void deleteReservation(Long reservationId) {

        if (!reservationRepository.existsById(reservationId)) {
            throw new RuntimeException("Reservation not found");
        }

        reservationRepository.deleteById(reservationId);
    }
}