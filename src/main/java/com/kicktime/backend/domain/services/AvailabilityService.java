package com.kicktime.backend.domain.services;

import com.kicktime.backend.domain.model.Availability;
import com.kicktime.backend.domain.model.TimeSlot;
import com.kicktime.backend.domain.model.User;

import com.kicktime.backend.domain.model.dto.request.CreateAvailabilityRequestDTO;
import com.kicktime.backend.domain.model.dto.response.AvailabilityResponseDTO;

import com.kicktime.backend.repository.AvailabilityRepository;
import com.kicktime.backend.repository.TimeSlotRepository;
import com.kicktime.backend.repository.UserRepository;

import com.kicktime.backend.util.mappers.AvailabilityMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilityRepository availabilityRepository;
    private final UserRepository userRepository;
    private final TimeSlotRepository timeSlotRepository;

    private final AvailabilityMapper availabilityMapper;

    /**
     * Create availability
     */
    public AvailabilityResponseDTO createAvailability(CreateAvailabilityRequestDTO request) {

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        TimeSlot timeSlot = timeSlotRepository.findById(request.getTimeSlotId())
                .orElseThrow(() -> new RuntimeException("TimeSlot not found"));

        Availability availability = Availability.builder()
                .user(user)
                .timeSlot(timeSlot)
                .build();

        availabilityRepository.save(availability);

        return availabilityMapper.toDTO(availability);
    }

    /**
     * Get availability by user
     */
    public List<AvailabilityResponseDTO> getUserAvailability(Long userId) {

        List<Availability> availabilityList =
                availabilityRepository.findByUser_Id(userId);

        return availabilityList.stream()
                .map(availabilityMapper::toDTO)
                .toList();
    }

    /**
     * Delete availability
     */
    public void deleteAvailability(Long availabilityId) {

        availabilityRepository.deleteById(availabilityId);
    }
}