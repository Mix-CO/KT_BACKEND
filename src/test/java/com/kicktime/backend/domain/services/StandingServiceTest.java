package com.kicktime.backend.domain.services;

import com.kicktime.backend.domain.model.*;
import com.kicktime.backend.domain.model.dto.request.InitializeStandingsRequestDTO;
import com.kicktime.backend.domain.model.dto.response.StandingResponseDTO;
import com.kicktime.backend.repository.StandingRepository;
import com.kicktime.backend.repository.TeamRepository;
import com.kicktime.backend.repository.TournamentRepository;
import com.kicktime.backend.util.mappers.StandingMapper;

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
class StandingServiceTest {

    @Mock private StandingRepository standingRepository;
    @Mock private TournamentRepository tournamentRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private StandingMapper standingMapper;

    @InjectMocks
    private StandingService standingService;

    private Tournament tournament;
    private Team homeTeam;
    private Team awayTeam;
    private Standing homeStanding;
    private Standing awayStanding;
    private Match match;
    private MatchResult result;

    @BeforeEach
    void setUp() {
        tournament = Tournament.builder()
                .id(1L)
                .name("Liga Universitaria 2025")
                .build();

        homeTeam = Team.builder()
                .id(10L)
                .name("Los Tigres")
                .tournament(tournament)
                .build();

        awayTeam = Team.builder()
                .id(20L)
                .name("Los Leones")
                .tournament(tournament)
                .build();

        homeStanding = Standing.builder()
                .id(100L)
                .team(homeTeam)
                .tournament(tournament)
                .played(0).wins(0).draws(0).losses(0)
                .goalsFor(0).goalsAgainst(0).points(0)
                .build();

        awayStanding = Standing.builder()
                .id(200L)
                .team(awayTeam)
                .tournament(tournament)
                .played(0).wins(0).draws(0).losses(0)
                .goalsFor(0).goalsAgainst(0).points(0)
                .build();

        result = MatchResult.builder()
                .id(1L)
                .homeScore(0)
                .awayScore(0)
                .build();

        match = Match.builder()
                .id(1L)
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .result(result)
                .tournament(tournament)
                .build();
    }

    // ─── initializeStandings ──────────────────────────────────────────────────

    @Test
    void initializeStandings_success_savesOneStandingPerTeam() {
        InitializeStandingsRequestDTO request =
                InitializeStandingsRequestDTO.builder().tournamentId(1L).build();

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(teamRepository.findByTournamentId(1L)).thenReturn(List.of(homeTeam, awayTeam));

        standingService.initializeStandings(request);

        verify(standingRepository, times(2)).save(any(Standing.class));
    }

    @Test
    void initializeStandings_eachStandingCreatedWithZeroValues() {
        InitializeStandingsRequestDTO request =
                InitializeStandingsRequestDTO.builder().tournamentId(1L).build();

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(teamRepository.findByTournamentId(1L)).thenReturn(List.of(homeTeam));

        standingService.initializeStandings(request);

        ArgumentCaptor<Standing> captor = ArgumentCaptor.forClass(Standing.class);
        verify(standingRepository).save(captor.capture());

        Standing saved = captor.getValue();
        assertThat(saved.getPlayed()).isZero();
        assertThat(saved.getWins()).isZero();
        assertThat(saved.getDraws()).isZero();
        assertThat(saved.getLosses()).isZero();
        assertThat(saved.getGoalsFor()).isZero();
        assertThat(saved.getGoalsAgainst()).isZero();
        assertThat(saved.getPoints()).isZero();
        assertThat(saved.getTeam()).isEqualTo(homeTeam);
        assertThat(saved.getTournament()).isEqualTo(tournament);
    }

    @Test
    void initializeStandings_tournamentNotFound_throwsException() {
        InitializeStandingsRequestDTO request =
                InitializeStandingsRequestDTO.builder().tournamentId(99L).build();

        when(tournamentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> standingService.initializeStandings(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Tournament not found");

        verify(standingRepository, never()).save(any());
    }

    @Test
    void initializeStandings_noTeamsInTournament_savesNothing() {
        InitializeStandingsRequestDTO request =
                InitializeStandingsRequestDTO.builder().tournamentId(1L).build();

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(teamRepository.findByTournamentId(1L)).thenReturn(List.of());

        standingService.initializeStandings(request);

        verify(standingRepository, never()).save(any());
    }

    // ─── getStandingsByTournament ─────────────────────────────────────────────

    @Test
    void getStandingsByTournament_returnsMappedListOrderedByPoints() {
        StandingResponseDTO dto1 = StandingResponseDTO.builder()
                .id(100L).teamId(10L).points(6).build();
        StandingResponseDTO dto2 = StandingResponseDTO.builder()
                .id(200L).teamId(20L).points(3).build();

        when(standingRepository.findByTournamentIdOrderByPointsDesc(1L))
                .thenReturn(List.of(homeStanding, awayStanding));
        when(standingMapper.toDTO(homeStanding)).thenReturn(dto1);
        when(standingMapper.toDTO(awayStanding)).thenReturn(dto2);

        List<StandingResponseDTO> result = standingService.getStandingsByTournament(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPoints()).isEqualTo(6);
        assertThat(result.get(1).getPoints()).isEqualTo(3);
        verify(standingMapper, times(2)).toDTO(any(Standing.class));
    }

    @Test
    void getStandingsByTournament_returnsEmptyList_whenNoStandings() {
        when(standingRepository.findByTournamentIdOrderByPointsDesc(1L))
                .thenReturn(List.of());

        List<StandingResponseDTO> result = standingService.getStandingsByTournament(1L);

        assertThat(result).isEmpty();
        verify(standingMapper, never()).toDTO(any());
    }

    // ─── updateStandingsAfterMatch ────────────────────────────────────────────

    @Test
    void updateStandings_nullResult_doesNothing() {
        match.setResult(null);

        standingService.updateStandingsAfterMatch(match);

        verify(standingRepository, never()).save(any());
    }

    @Test
    void updateStandings_homeWin_updatesPointsCorrectly() {
        result.setHomeScore(3);
        result.setAwayScore(1);

        when(standingRepository.findByTeamIdAndTournamentId(10L, 1L))
                .thenReturn(Optional.of(homeStanding));
        when(standingRepository.findByTeamIdAndTournamentId(20L, 1L))
                .thenReturn(Optional.of(awayStanding));

        standingService.updateStandingsAfterMatch(match);

        assertThat(homeStanding.getPoints()).isEqualTo(3);
        assertThat(homeStanding.getWins()).isEqualTo(1);
        assertThat(homeStanding.getLosses()).isZero();

        assertThat(awayStanding.getPoints()).isZero();
        assertThat(awayStanding.getLosses()).isEqualTo(1);
        assertThat(awayStanding.getWins()).isZero();
    }

    @Test
    void updateStandings_awayWin_updatesPointsCorrectly() {
        result.setHomeScore(0);
        result.setAwayScore(2);

        when(standingRepository.findByTeamIdAndTournamentId(10L, 1L))
                .thenReturn(Optional.of(homeStanding));
        when(standingRepository.findByTeamIdAndTournamentId(20L, 1L))
                .thenReturn(Optional.of(awayStanding));

        standingService.updateStandingsAfterMatch(match);

        assertThat(awayStanding.getPoints()).isEqualTo(3);
        assertThat(awayStanding.getWins()).isEqualTo(1);
        assertThat(awayStanding.getLosses()).isZero();

        assertThat(homeStanding.getPoints()).isZero();
        assertThat(homeStanding.getLosses()).isEqualTo(1);
        assertThat(homeStanding.getWins()).isZero();
    }

    @Test
    void updateStandings_draw_updatesPointsCorrectly() {
        result.setHomeScore(1);
        result.setAwayScore(1);

        when(standingRepository.findByTeamIdAndTournamentId(10L, 1L))
                .thenReturn(Optional.of(homeStanding));
        when(standingRepository.findByTeamIdAndTournamentId(20L, 1L))
                .thenReturn(Optional.of(awayStanding));

        standingService.updateStandingsAfterMatch(match);

        assertThat(homeStanding.getPoints()).isEqualTo(1);
        assertThat(homeStanding.getDraws()).isEqualTo(1);
        assertThat(awayStanding.getPoints()).isEqualTo(1);
        assertThat(awayStanding.getDraws()).isEqualTo(1);
    }

    @Test
    void updateStandings_homeStandingNotFound_createsNewStanding() {
        result.setHomeScore(1);
        result.setAwayScore(0);

        Standing newHomeStanding = Standing.builder()
                .team(homeTeam)
                .tournament(tournament)
                .played(0).wins(0).draws(0).losses(0)
                .goalsFor(0).goalsAgainst(0).points(0)
                .build();

        when(standingRepository.findByTeamIdAndTournamentId(10L, 1L))
                .thenReturn(Optional.empty());
        when(standingRepository.save(any(Standing.class))).thenReturn(newHomeStanding);
        when(standingRepository.findByTeamIdAndTournamentId(20L, 1L))
                .thenReturn(Optional.of(awayStanding));

        standingService.updateStandingsAfterMatch(match);

        // Verifica que se llamó save para crear el standing del home y luego para guardar ambos
        verify(standingRepository, atLeast(2)).save(any(Standing.class));
    }

    @Test
    void updateStandings_awayStandingNotFound_createsNewStanding() {
        result.setHomeScore(1);
        result.setAwayScore(0);

        Standing newAwayStanding = Standing.builder()
                .team(awayTeam)
                .tournament(tournament)
                .played(0).wins(0).draws(0).losses(0)
                .goalsFor(0).goalsAgainst(0).points(0)
                .build();

        when(standingRepository.findByTeamIdAndTournamentId(10L, 1L))
                .thenReturn(Optional.of(homeStanding));
        when(standingRepository.findByTeamIdAndTournamentId(20L, 1L))
                .thenReturn(Optional.empty());
        when(standingRepository.save(any(Standing.class))).thenReturn(newAwayStanding);

        standingService.updateStandingsAfterMatch(match);

        verify(standingRepository, atLeast(2)).save(any(Standing.class));
    }

    @Test
    void updateStandings_goalsUpdatedCorrectly_onHomeWin() {
        result.setHomeScore(3);
        result.setAwayScore(1);

        when(standingRepository.findByTeamIdAndTournamentId(10L, 1L))
                .thenReturn(Optional.of(homeStanding));
        when(standingRepository.findByTeamIdAndTournamentId(20L, 1L))
                .thenReturn(Optional.of(awayStanding));

        standingService.updateStandingsAfterMatch(match);

        assertThat(homeStanding.getGoalsFor()).isEqualTo(3);
        assertThat(homeStanding.getGoalsAgainst()).isEqualTo(1);
        assertThat(awayStanding.getGoalsFor()).isEqualTo(1);
        assertThat(awayStanding.getGoalsAgainst()).isEqualTo(3);
        assertThat(homeStanding.getPlayed()).isEqualTo(1);
        assertThat(awayStanding.getPlayed()).isEqualTo(1);
    }

    @Test
    void updateStandings_bothStandingsSavedAfterUpdate() {
        result.setHomeScore(2);
        result.setAwayScore(0);

        when(standingRepository.findByTeamIdAndTournamentId(10L, 1L))
                .thenReturn(Optional.of(homeStanding));
        when(standingRepository.findByTeamIdAndTournamentId(20L, 1L))
                .thenReturn(Optional.of(awayStanding));

        standingService.updateStandingsAfterMatch(match);

        verify(standingRepository).save(homeStanding);
        verify(standingRepository).save(awayStanding);
    }
}