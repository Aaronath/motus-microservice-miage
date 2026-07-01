package fr.miage.motus.stats.dto;

public record LeaderboardEntryDto(Long playerId, String pseudo, int wins, int totalGames, int totalScore) {}
