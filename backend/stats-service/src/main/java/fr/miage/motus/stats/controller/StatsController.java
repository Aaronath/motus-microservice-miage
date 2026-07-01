package fr.miage.motus.stats.controller;

import fr.miage.motus.stats.service.StatsService;
import fr.miage.motus.stats.dto.GlobalStatsDto;
import fr.miage.motus.stats.dto.LeaderboardEntryDto;
import fr.miage.motus.stats.dto.PlayerStatsResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @PostMapping("/results")
    @ResponseStatus(HttpStatus.CREATED)
    public void record(@RequestBody Map<String, Object> payload) {
        statsService.record(payload);
    }

    @GetMapping("/players/{playerId}")
    public PlayerStatsResponse player(@PathVariable Long playerId) {
        return statsService.playerStats(playerId);
    }

    @GetMapping("/leaderboard")
    public List<LeaderboardEntryDto> leaderboard() {
        return statsService.leaderboard();
    }

    @GetMapping("/global")
    public GlobalStatsDto global() {
        return statsService.global();
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "stats-service");
    }
}
