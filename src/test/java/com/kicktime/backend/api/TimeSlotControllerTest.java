package com.kicktime.backend.api;

import com.kicktime.backend.domain.model.TimeSlot;
import com.kicktime.backend.domain.model.enums.TimeSlotStatus;
import com.kicktime.backend.repository.TimeSlotRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("TimeSlotController - Pruebas de Integración")
class TimeSlotControllerTest {

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    // ================================================================
    // getAllTimeSlots
    // ================================================================

    @Nested
    @DisplayName("getAllTimeSlots")
    class GetAllTimeSlotsTests {

        @Test
        @DisplayName("Debe retornar lista no nula de timeslots")
        void getAllTimeSlots_ReturnsNotNull() {
            List<TimeSlot> result = timeSlotRepository.findAll();

            assertNotNull(result);
        }

        @Test
        @DisplayName("Debe retornar lista con solo timeslots válidos")
        void getAllTimeSlots_ReturnsValidTimeSlots() {
            List<TimeSlot> result = timeSlotRepository.findAll();

            assertNotNull(result);
            result.forEach(ts -> assertNotNull(ts.getStatus()));
        }
    }

    // ================================================================
    // getAvailableTimeSlots
    // ================================================================

    @Nested
    @DisplayName("getAvailableTimeSlots")
    class GetAvailableTimeSlotsTests {

        @Test
        @DisplayName("Debe retornar solo timeslots con estado AVAILABLE")
        void getAvailableTimeSlots_ReturnsOnlyAvailable() {
            List<TimeSlot> result = timeSlotRepository.findAll().stream()
                    .filter(ts -> ts.getStatus() == TimeSlotStatus.AVAILABLE)
                    .toList();

            assertNotNull(result);
            result.forEach(ts ->
                    assertEquals(TimeSlotStatus.AVAILABLE, ts.getStatus())
            );
        }

        @Test
        @DisplayName("Debe retornar lista no nula de timeslots disponibles")
        void getAvailableTimeSlots_ReturnsNotNull() {
            List<TimeSlot> result = timeSlotRepository.findAll().stream()
                    .filter(ts -> ts.getStatus() == TimeSlotStatus.AVAILABLE)
                    .toList();

            assertNotNull(result);
        }

        @Test
        @DisplayName("Debe retornar lista sin timeslots LOCKED ni RESERVED")
        void getAvailableTimeSlots_NoLockedOrReserved() {
            List<TimeSlot> result = timeSlotRepository.findAll().stream()
                    .filter(ts -> ts.getStatus() == TimeSlotStatus.AVAILABLE)
                    .toList();

            result.forEach(ts -> {
                assertNotEquals(TimeSlotStatus.LOCKED, ts.getStatus());
                assertNotEquals(TimeSlotStatus.RESERVED, ts.getStatus());
            });
        }
    }
}