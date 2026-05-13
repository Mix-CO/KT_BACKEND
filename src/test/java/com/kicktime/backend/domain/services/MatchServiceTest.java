package com.kicktime.backend.domain.services;

import com.kicktime.backend.domain.model.*;
import com.kicktime.backend.domain.model.dto.request.CreateMatchRequestDTO;
import com.kicktime.backend.domain.model.dto.request.RecordMatchResultRequestDTO;
import com.kicktime.backend.domain.model.dto.request.ScheduleMatchRequestDTO;
import com.kicktime.backend.domain.model.dto.response.MatchResponseDTO;
import com.kicktime.backend.domain.model.enums.MatchStatus;
import com.kicktime.backend.repository.*;
import com.kicktime.backend.util.mappers.MatchMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchService - Pruebas Unitarias")
class MatchServiceTest {

    @Mock private MatchRepository matchRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private TournamentRepository tournamentRepository;
    @Mock private FieldRepository fieldRepository;
    @Mock private TimeSlotRepository timeSlotRepository;
    @Mock private MatchResultRepository matchResultRepository;
    @Mock private MatchMapper matchMapper;
    @Mock private StandingService standingService;

    @InjectMocks
    private MatchService matchService;

    // ----------------------------------------------------------------
    // Datos de prueba reutilizables
    // ----------------------------------------------------------------

    private Tournament tournament;
    private Team homeTeam;
    private Team awayTeam;
    private Field field;
    private TimeSlot timeSlot;
    private Match match;
    private MatchResponseDTO matchResponseDTO;
    private CreateMatchRequestDTO createRequest;

    @BeforeEach
    void setUp() {
        tournament = Tournament.builder()
                .id(1L)
                .name("Torneo ARSW 2026")
                .build();

        homeTeam = Team.builder()
                .id(1L)
                .name("Equipo Local")
                .tournament(tournament)
                .build();

        awayTeam = Team.builder()
                .id(2L)
                .name("Equipo Visitante")
                .tournament(tournament)
                .build();

        field = new Field();
        field.setNumber(1);

        timeSlot = TimeSlot.builder()
                .id(1L)
                .build();

        match = Match.builder()
                .id(1L)
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .tournament(tournament)
                .status(MatchStatus.SCHEDULED)
                .build();

        matchResponseDTO = MatchResponseDTO.builder()
                .id(1L)
                .homeTeamId(1L)
                .homeTeamName("Equipo Local")
                .awayTeamId(2L)
                .awayTeamName("Equipo Visitante")
                .status(MatchStatus.SCHEDULED)
                .build();

        createRequest = CreateMatchRequestDTO.builder()
                .homeTeamId(1L)
                .awayTeamId(2L)
                .tournamentId(1L)
                .build();
    }

    // ================================================================
    // createMatch
    // ================================================================

    @Nested
    @DisplayName("createMatch")
    class CreateMatchTests {

        @Test
        @DisplayName("Debe crear un partido exitosamente con datos válidos")
        void createMatch_ValidRequest_ReturnsMatchResponseDTO() {
            when(teamRepository.findById(1L)).thenReturn(Optional.of(homeTeam));
            when(teamRepository.findById(2L)).thenReturn(Optional.of(awayTeam));
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
            when(matchRepository.save(any(Match.class))).thenReturn(match);
            when(matchMapper.toDTO(match)).thenReturn(matchResponseDTO);

            MatchResponseDTO result = matchService.createMatch(createRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getHomeTeamName()).isEqualTo("Equipo Local");
            assertThat(result.getAwayTeamName()).isEqualTo("Equipo Visitante");
            assertThat(result.getStatus()).isEqualTo(MatchStatus.SCHEDULED);
            verify(matchRepository).save(any(Match.class));
        }

        @Test
        @DisplayName("Debe asignar estado SCHEDULED automáticamente al crear")
        void createMatch_ShouldAssignScheduledStatusAutomatically() {
            when(teamRepository.findById(1L)).thenReturn(Optional.of(homeTeam));
            when(teamRepository.findById(2L)).thenReturn(Optional.of(awayTeam));
            when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
            when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> {
                Match saved = invocation.getArgument(0);
                assertThat(saved.getStatus()).isEqualTo(MatchStatus.SCHEDULED);
                return match;
            });
            when(matchMapper.toDTO(any())).thenReturn(matchResponseDTO);

            matchService.createMatch(createRequest);

            verify(matchRepository).save(any(Match.class));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el equipo local no existe")
        void createMatch_HomeTeamNotFound_ThrowsException() {
            when(teamRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchService.createMatch(createRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Home team not found");

            verify(matchRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el equipo visitante no existe")
        void createMatch_AwayTeamNotFound_ThrowsException() {
            when(teamRepository.findById(1L)).thenReturn(Optional.of(homeTeam));
            when(teamRepository.findById(2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchService.createMatch(createRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Away team not found");

            verify(matchRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el torneo no existe")
        void createMatch_TournamentNotFound_ThrowsException() {
            when(teamRepository.findById(1L)).thenReturn(Optional.of(homeTeam));
            when(teamRepository.findById(2L)).thenReturn(Optional.of(awayTeam));
            when(tournamentRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchService.createMatch(createRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Tournament not found");

            verify(matchRepository, never()).save(any());
        }
    }

    // ================================================================
    // getMatch
    // ================================================================

    @Nested
    @DisplayName("getMatch")
    class GetMatchTests {

        @Test
        @DisplayName("Debe retornar el partido cuando existe el ID")
        void getMatch_ExistingId_ReturnsMatchResponseDTO() {
            when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
            when(matchMapper.toDTO(match)).thenReturn(matchResponseDTO);

            MatchResponseDTO result = matchService.getMatch(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(matchRepository).findById(1L);
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando no existe el ID")
        void getMatch_NonExistingId_ThrowsException() {
            when(matchRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchService.getMatch(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Match not found");

            verify(matchMapper, never()).toDTO(any());
        }
    }

    // ================================================================
    // getMatchesByTournament
    // ================================================================

    @Nested
    @DisplayName("getMatchesByTournament")
    class GetMatchesByTournamentTests {

        @Test
        @DisplayName("Debe retornar lista de partidos de un torneo")
        void getMatchesByTournament_WithData_ReturnsList() {
            when(matchRepository.findByTournament_Id(1L)).thenReturn(List.of(match));
            when(matchMapper.toDTO(match)).thenReturn(matchResponseDTO);

            List<MatchResponseDTO> result = matchService.getMatchesByTournament(1L);

            assertThat(result).isNotNull().hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
            verify(matchRepository).findByTournament_Id(1L);
        }

        @Test
        @DisplayName("Debe retornar lista vacía cuando no hay partidos en el torneo")
        void getMatchesByTournament_Empty_ReturnsEmptyList() {
            when(matchRepository.findByTournament_Id(1L)).thenReturn(List.of());

            List<MatchResponseDTO> result = matchService.getMatchesByTournament(1L);

            assertThat(result).isNotNull().isEmpty();
            verify(matchRepository).findByTournament_Id(1L);
        }
    }

    // ================================================================
    // scheduleMatch
    // ================================================================

    @Nested
    @DisplayName("scheduleMatch")
    class ScheduleMatchTests {

        @Test
        @DisplayName("Debe programar el partido asignando campo y franja horaria")
        void scheduleMatch_ValidRequest_ReturnsConfirmedMatch() {
            ScheduleMatchRequestDTO request = ScheduleMatchRequestDTO.builder()
                    .fieldId(1L)
                    .timeSlotId(1L)
                    .build();

            MatchResponseDTO confirmedResponse = MatchResponseDTO.builder()
                    .id(1L)
                    .status(MatchStatus.CONFIRMED)
                    .fieldId(1L)
                    .timeSlotId(1L)
                    .build();

            Match updatedMatch = Match.builder()
                    .id(1L)
                    .homeTeam(homeTeam)
                    .awayTeam(awayTeam)
                    .tournament(tournament)
                    .field(field)
                    .timeSlot(timeSlot)
                    .status(MatchStatus.CONFIRMED)
                    .build();

            when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
            when(fieldRepository.findById(1L)).thenReturn(Optional.of(field));
            when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(timeSlot));
            when(matchRepository.save(any(Match.class))).thenReturn(updatedMatch);
            when(matchMapper.toDTO(updatedMatch)).thenReturn(confirmedResponse);

            MatchResponseDTO result = matchService.scheduleMatch(1L, request);

            assertThat(result.getStatus()).isEqualTo(MatchStatus.CONFIRMED);
            assertThat(result.getFieldId()).isEqualTo(1L);
            assertThat(result.getTimeSlotId()).isEqualTo(1L);
            verify(matchRepository).save(any(Match.class));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el partido no existe")
        void scheduleMatch_MatchNotFound_ThrowsException() {
            ScheduleMatchRequestDTO request = ScheduleMatchRequestDTO.builder()
                    .fieldId(1L)
                    .timeSlotId(1L)
                    .build();

            when(matchRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchService.scheduleMatch(99L, request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Match not found");

            verify(matchRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el campo no existe")
        void scheduleMatch_FieldNotFound_ThrowsException() {
            ScheduleMatchRequestDTO request = ScheduleMatchRequestDTO.builder()
                    .fieldId(99L)
                    .timeSlotId(1L)
                    .build();

            when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
            when(fieldRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchService.scheduleMatch(1L, request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Field not found");

            verify(matchRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el timeSlot no existe")
        void scheduleMatch_TimeSlotNotFound_ThrowsException() {
            ScheduleMatchRequestDTO request = ScheduleMatchRequestDTO.builder()
                    .fieldId(1L)
                    .timeSlotId(99L)
                    .build();

            when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
            when(fieldRepository.findById(1L)).thenReturn(Optional.of(field));
            when(timeSlotRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchService.scheduleMatch(1L, request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("TimeSlot not found");

            verify(matchRepository, never()).save(any());
        }
    }

    // ================================================================
    // recordMatchResult
    // ================================================================

    @Nested
    @DisplayName("recordMatchResult")
    class RecordMatchResultTests {

        @Test
        @DisplayName("Debe registrar resultado con victoria del equipo local")
        void recordMatchResult_HomeWins_SetsHomeTeamAsWinner() {
            RecordMatchResultRequestDTO request = RecordMatchResultRequestDTO.builder()
                    .homeScore(3)
                    .awayScore(1)
                    .build();

            MatchResponseDTO playedResponse = MatchResponseDTO.builder()
                    .id(1L)
                    .status(MatchStatus.PLAYED)
                    .homeScore(3)
                    .awayScore(1)
                    .build();

            when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
            when(matchResultRepository.save(any(MatchResult.class))).thenAnswer(i -> i.getArgument(0));
            when(matchRepository.save(any(Match.class))).thenReturn(match);
            when(matchMapper.toDTO(any(Match.class))).thenReturn(playedResponse);
            doNothing().when(standingService).updateStandingsAfterMatch(any(Match.class));

            MatchResponseDTO result = matchService.recordMatchResult(1L, request);

            assertThat(result.getStatus()).isEqualTo(MatchStatus.PLAYED);
            assertThat(result.getHomeScore()).isEqualTo(3);
            assertThat(result.getAwayScore()).isEqualTo(1);
            verify(matchResultRepository).save(any(MatchResult.class));
            verify(standingService).updateStandingsAfterMatch(any(Match.class));
        }

        @Test
        @DisplayName("Debe registrar resultado con victoria del equipo visitante")
        void recordMatchResult_AwayWins_SetsAwayTeamAsWinner() {
            RecordMatchResultRequestDTO request = RecordMatchResultRequestDTO.builder()
                    .homeScore(0)
                    .awayScore(2)
                    .build();

            MatchResponseDTO playedResponse = MatchResponseDTO.builder()
                    .id(1L)
                    .status(MatchStatus.PLAYED)
                    .homeScore(0)
                    .awayScore(2)
                    .build();

            when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
            when(matchResultRepository.save(any(MatchResult.class))).thenAnswer(invocation -> {
                MatchResult saved = invocation.getArgument(0);
                assertThat(saved.getWinner()).isEqualTo(awayTeam);
                return saved;
            });
            when(matchRepository.save(any(Match.class))).thenReturn(match);
            when(matchMapper.toDTO(any(Match.class))).thenReturn(playedResponse);
            doNothing().when(standingService).updateStandingsAfterMatch(any(Match.class));

            MatchResponseDTO result = matchService.recordMatchResult(1L, request);

            assertThat(result.getStatus()).isEqualTo(MatchStatus.PLAYED);
            verify(matchResultRepository).save(any(MatchResult.class));
        }

        @Test
        @DisplayName("Debe registrar empate con winner null")
        void recordMatchResult_Draw_SetsWinnerAsNull() {
            RecordMatchResultRequestDTO request = RecordMatchResultRequestDTO.builder()
                    .homeScore(1)
                    .awayScore(1)
                    .build();

            MatchResponseDTO playedResponse = MatchResponseDTO.builder()
                    .id(1L)
                    .status(MatchStatus.PLAYED)
                    .homeScore(1)
                    .awayScore(1)
                    .build();

            when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
            when(matchResultRepository.save(any(MatchResult.class))).thenAnswer(invocation -> {
                MatchResult saved = invocation.getArgument(0);
                assertThat(saved.getWinner()).isNull();
                return saved;
            });
            when(matchRepository.save(any(Match.class))).thenReturn(match);
            when(matchMapper.toDTO(any(Match.class))).thenReturn(playedResponse);
            doNothing().when(standingService).updateStandingsAfterMatch(any(Match.class));

            MatchResponseDTO result = matchService.recordMatchResult(1L, request);

            assertThat(result.getStatus()).isEqualTo(MatchStatus.PLAYED);
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el partido no existe")
        void recordMatchResult_MatchNotFound_ThrowsException() {
            RecordMatchResultRequestDTO request = RecordMatchResultRequestDTO.builder()
                    .homeScore(2)
                    .awayScore(0)
                    .build();

            when(matchRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchService.recordMatchResult(99L, request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Match not found");

            verify(matchResultRepository, never()).save(any());
            verify(standingService, never()).updateStandingsAfterMatch(any());
        }

        @Test
        @DisplayName("Debe marcar el partido como PLAYED al registrar resultado")
        void recordMatchResult_ShouldMarkMatchAsPlayed() {
            RecordMatchResultRequestDTO request = RecordMatchResultRequestDTO.builder()
                    .homeScore(2)
                    .awayScore(1)
                    .build();

            when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
            when(matchResultRepository.save(any(MatchResult.class))).thenAnswer(i -> i.getArgument(0));
            when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> {
                Match saved = invocation.getArgument(0);
                assertThat(saved.getStatus()).isEqualTo(MatchStatus.PLAYED);
                return saved;
            });
            when(matchMapper.toDTO(any(Match.class))).thenReturn(matchResponseDTO);
            doNothing().when(standingService).updateStandingsAfterMatch(any(Match.class));

            matchService.recordMatchResult(1L, request);

            verify(matchRepository).save(any(Match.class));
        }
    }

    // ================================================================
    // deleteMatch
    // ================================================================

    @Nested
    @DisplayName("deleteMatch")
    class DeleteMatchTests {

        @Test
        @DisplayName("Debe eliminar el partido exitosamente cuando existe")
        void deleteMatch_ExistingId_DeletesSuccessfully() {
            when(matchRepository.existsById(1L)).thenReturn(true);
            doNothing().when(matchRepository).deleteById(1L);

            assertThatCode(() -> matchService.deleteMatch(1L))
                    .doesNotThrowAnyException();

            verify(matchRepository).existsById(1L);
            verify(matchRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el partido no existe al eliminar")
        void deleteMatch_NonExistingId_ThrowsException() {
            when(matchRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> matchService.deleteMatch(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Match not found");

            verify(matchRepository, never()).deleteById(any());
        }
    }
}
