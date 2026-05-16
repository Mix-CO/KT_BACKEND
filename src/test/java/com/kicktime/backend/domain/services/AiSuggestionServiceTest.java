package com.kicktime.backend.domain.services;

import com.kicktime.backend.repository.AvailabilityRepository;
import com.kicktime.backend.repository.MatchRepository;
import com.kicktime.backend.repository.TimeSlotRepository;
import com.kicktime.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiSuggestionServiceTest {

    @Mock private MatchRepository matchRepository;
    @Mock private UserRepository userRepository;
    @Mock private AvailabilityRepository availabilityRepository;
    @Mock private TimeSlotRepository timeSlotRepository;

    @InjectMocks
    private AiSuggestionService aiSuggestionService;

    @Test
    void suggestTimeSlot_matchNotFound_throwsException() {
        when(matchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiSuggestionService.suggestTimeSlot(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Match not found");

        verify(userRepository, never()).findByTeam_Id(any());
        verify(timeSlotRepository, never()).findAll();
    }
}
