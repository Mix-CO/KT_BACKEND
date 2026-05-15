package com.kicktime.backend.api;

import com.kicktime.backend.domain.model.TimeSlot;
import com.kicktime.backend.domain.model.enums.TimeSlotStatus;
import com.kicktime.backend.repository.TimeSlotRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("TimeSlotController - Pruebas de Integración")
class TimeSlotControllerTest {

    @Autowired
    private TimeSlotController timeSlotController;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Nested
    @DisplayName("getAllTimeSlots")
    class GetAllTimeSlotsTests {

        @Test
        @DisplayName("Debe retornar lista no nula de timeslots")
        void getAllTimeSlots_ReturnsNotNull() {
            ResponseEntity<List<TimeSlot>> response = timeSlotController.getAllTimeSlots();
            assertNotNull(response);
            assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("Debe retornar lista con solo timeslots válidos")
        void getAllTimeSlots_ReturnsValidTimeSlots() {
            ResponseEntity<List<TimeSlot>> response = timeSlotController.getAllTimeSlots();
            assertNotNull(response.getBody());
            response.getBody().forEach(ts -> assertNotNull(ts.getStatus()));
        }
    }

    @Nested
    @DisplayName("getAvailableTimeSlots")
    class GetAvailableTimeSlotsTests {

        @Test
        @DisplayName("Debe retornar solo timeslots con estado AVAILABLE")
        void getAvailableTimeSlots_ReturnsOnlyAvailable() {
            ResponseEntity<List<TimeSlot>> response = timeSlotController.getAvailableTimeSlots();
            assertNotNull(response.getBody());
            response.getBody().forEach(ts ->
                    assertEquals(TimeSlotStatus.AVAILABLE, ts.getStatus())
            );
        }

        @Test
        @DisplayName("Debe retornar lista no nula de timeslots disponibles")
        void getAvailableTimeSlots_ReturnsNotNull() {
            ResponseEntity<List<TimeSlot>> response = timeSlotController.getAvailableTimeSlots();
            assertNotNull(response);
            assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("Debe excluir timeslots LOCKED del resultado")
        void getAvailableTimeSlots_ExcludesLockedSlots() {
            TimeSlot locked = new TimeSlot();
            locked.setStatus(TimeSlotStatus.LOCKED);
            TimeSlot savedLocked = timeSlotRepository.save(locked);

            try {
                ResponseEntity<List<TimeSlot>> response = timeSlotController.getAvailableTimeSlots();
                assertNotNull(response.getBody());
                boolean containsLocked = response.getBody().stream()
                        .anyMatch(ts -> ts.getStatus() == TimeSlotStatus.LOCKED);
                assertFalse(containsLocked);
            } finally {
                timeSlotRepository.deleteById(savedLocked.getId());
            }
        }

        @Test
        @DisplayName("Debe excluir timeslots RESERVED del resultado")
        void getAvailableTimeSlots_ExcludesReservedSlots() {
            TimeSlot reserved = new TimeSlot();
            reserved.setStatus(TimeSlotStatus.RESERVED);
            TimeSlot savedReserved = timeSlotRepository.save(reserved);

            try {
                ResponseEntity<List<TimeSlot>> response = timeSlotController.getAvailableTimeSlots();
                assertNotNull(response.getBody());
                boolean containsReserved = response.getBody().stream()
                        .anyMatch(ts -> ts.getStatus() == TimeSlotStatus.RESERVED);
                assertFalse(containsReserved);
            } finally {
                timeSlotRepository.deleteById(savedReserved.getId());
            }
        }
    }
}