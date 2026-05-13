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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock private AvailabilityRepository availabilityRepository;
    @Mock private UserRepository userRepository;
    @Mock private TimeSlotRepository timeSlotRepository;
    @Mock private AvailabilityMapper availabilityMapper;

    @InjectMocks
    private AvailabilityService availabilityService;

    private User user;
    private TimeSlot timeSlot;
    private Availability availability;
    private CreateAvailabilityRequestDTO request;
    private AvailabilityResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Julian Torres")
                .email("julian@kicktime.com")
                .build();

        timeSlot = TimeSlot.builder()
                .id(10L)
                .build();

        availability = Availability.builder()
                .id(100L)
                .user(user)
                .timeSlot(timeSlot)
                .build();

        request = CreateAvailabilityRequestDTO.builder()
                .userId(1L)
                .timeSlotId(10L)
                .build();

        responseDTO = AvailabilityResponseDTO.builder()
                .id(100L)
                .userId(1L)
                .userName("Julian Torres")
                .timeSlotId(10L)
                .build();
    }

    // ─── createAvailability ────────────────────────────────────────────────────

    @Test
    void createAvailability_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(timeSlot));
        when(availabilityMapper.toDTO(any(Availability.class))).thenReturn(responseDTO);

        AvailabilityResponseDTO result = availabilityService.createAvailability(request);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getTimeSlotId()).isEqualTo(10L);
        assertThat(result.getUserName()).isEqualTo("Julian Torres");

        verify(availabilityRepository).save(any(Availability.class));
    }

    @Test
    void createAvailability_userNotFound_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> availabilityService.createAvailability(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");

        verify(availabilityRepository, never()).save(any());
    }

    @Test
    void createAvailability_timeSlotNotFound_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> availabilityService.createAvailability(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("TimeSlot not found");

        verify(availabilityRepository, never()).save(any());
    }

    @Test
    void createAvailability_savesCorrectEntity() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(timeSlot));
        when(availabilityMapper.toDTO(any(Availability.class))).thenReturn(responseDTO);

        availabilityService.createAvailability(request);

        ArgumentCaptor<Availability> captor = ArgumentCaptor.forClass(Availability.class);
        verify(availabilityRepository).save(captor.capture());

        Availability saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getTimeSlot()).isEqualTo(timeSlot);
    }

    @Test
    void createAvailability_mapperCalledWithSavedEntity() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(timeSlot));
        when(availabilityMapper.toDTO(any(Availability.class))).thenReturn(responseDTO);

        availabilityService.createAvailability(request);

        ArgumentCaptor<Availability> captor = ArgumentCaptor.forClass(Availability.class);
        verify(availabilityMapper).toDTO(captor.capture());

        Availability mapped = captor.getValue();
        assertThat(mapped.getUser()).isEqualTo(user);
        assertThat(mapped.getTimeSlot()).isEqualTo(timeSlot);
    }

    // ─── getUserAvailability ───────────────────────────────────────────────────

    @Test
    void getUserAvailability_returnsMappedList() {
        Availability a2 = Availability.builder().id(200L).user(user).timeSlot(timeSlot).build();
        AvailabilityResponseDTO dto2 = AvailabilityResponseDTO.builder()
                .id(200L).userId(1L).timeSlotId(10L).build();

        when(availabilityRepository.findByUser_Id(1L)).thenReturn(List.of(availability, a2));
        when(availabilityMapper.toDTO(availability)).thenReturn(responseDTO);
        when(availabilityMapper.toDTO(a2)).thenReturn(dto2);

        List<AvailabilityResponseDTO> result = availabilityService.getUserAvailability(1L);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(responseDTO, dto2);
        verify(availabilityMapper, times(2)).toDTO(any(Availability.class));
    }

    @Test
    void getUserAvailability_returnsEmptyList_whenNoAvailabilities() {
        when(availabilityRepository.findByUser_Id(99L)).thenReturn(List.of());

        List<AvailabilityResponseDTO> result = availabilityService.getUserAvailability(99L);

        assertThat(result).isEmpty();
        verify(availabilityMapper, never()).toDTO(any());
    }

    // ─── deleteAvailability ────────────────────────────────────────────────────

    @Test
    void deleteAvailability_success() {
        doNothing().when(availabilityRepository).deleteById(100L);

        assertThatCode(() -> availabilityService.deleteAvailability(100L))
                .doesNotThrowAnyException();

        verify(availabilityRepository).deleteById(100L);
    }

    @Test
    void deleteAvailability_correctIdPassedToRepository() {
        doNothing().when(availabilityRepository).deleteById(anyLong());

        availabilityService.deleteAvailability(55L);

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(availabilityRepository).deleteById(captor.capture());

        assertThat(captor.getValue()).isEqualTo(55L);
    }
}
