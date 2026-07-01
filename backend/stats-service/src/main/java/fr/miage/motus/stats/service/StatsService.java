package fr.miage.motus.stats.service;

import fr.miage.motus.stats.entity.GameResult;
import fr.miage.motus.stats.repository.GameResultRepository;
import fr.miage.motus.stats.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class StatsService {

    private final GameResultRepository repository;

    public StatsService(GameResultRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(Map<String, Object> payload) {
        Long gameId = ((Number) payload.get("gameId")).longValue();
        if (repository.findByGameId(gameId).isPresent()) {
            return;
        }
        String status = payload.containsKey("status")
                ? (String) payload.get("status")
                : (Boolean.TRUE.equals(payload.get("won")) ? "WON" : "LOST");
        GameResult result = GameResult.from(
                gameId,
                ((Number) payload.get("playerId")).longValue(),
                (String) payload.get("playerPseudo"),
                Boolean.TRUE.equals(payload.get("won")),
                status,
                ((Number) payload.get("attempts")).intValue(),
                ((Number) payload.get("maxAttempts")).intValue(),
                ((Number) payload.get("wordLength")).intValue(),
                (String) payload.get("firstLetter"),
                (String) payload.get("secretWord"),
                LocalDateTime.parse((String) payload.get("finishedAt"))
        );
        repository.save(result);
    }

    @Transactional(readOnly = true)
    public PlayerStatsResponse playerStats(Long playerId) {
        var results = repository.findByPlayerIdOrderByFinishedAtDesc(playerId);
        long wins = results.stream().filter(GameResult::isWon).count();
        int totalScore = results.stream().mapToInt(GameResult::getScore).sum();
        var history = results.stream()
                .map(r -> new ResultLineDto(
                        r.getGameId(),
                        r.getOutcome(),
                        r.isWon(),
                        r.getAttempts(),
                        r.getMaxAttempts(),
                        r.getWordLength(),
                        r.getFirstLetter(),
                        r.getSecretWord(),
                        r.getScore(),
                        r.getFinishedAt()))
                .toList();
        return new PlayerStatsResponse(playerId, results.size(), wins, totalScore, history);
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryDto> leaderboard() {
        return repository.leaderboard().stream()
                .map(p -> new LeaderboardEntryDto(
                        p.getPlayerId(),
                        p.getPseudo(),
                        p.getWins().intValue(),
                        p.getTotalGames().intValue(),
                        p.getTotalScore().intValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public GlobalStatsDto global() {
        long total = repository.count();
        long wins = repository.findAll().stream().filter(GameResult::isWon).count();
        return new GlobalStatsDto(total, wins, total - wins);
    }
}
