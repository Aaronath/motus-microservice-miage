package fr.miage.motus.stats.dto;

import java.util.List;

public record PlayerStatsResponse(
        Long playerId, long totalGames, long wins, int totalScore, List<ResultLineDto> history
) {}
