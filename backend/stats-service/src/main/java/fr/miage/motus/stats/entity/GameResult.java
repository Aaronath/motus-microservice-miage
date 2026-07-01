package fr.miage.motus.stats.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_results", indexes = {
        @Index(columnList = "playerId"),
        @Index(columnList = "finishedAt")
})
public class GameResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long gameId;

    @Column(nullable = false)
    private Long playerId;

    @Column(nullable = false)
    private String playerPseudo;

    @Column(nullable = false)
    private boolean won;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private int maxAttempts;

    @Column(nullable = false)
    private int wordLength;

    @Column(nullable = false, length = 1)
    private String firstLetter;

    @Column(length = 20)
    private String secretWord;

    @Column(nullable = false, length = 20)
    private String outcome = "LOST";

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private LocalDateTime finishedAt;

    protected GameResult() {}

    public static GameResult from(Long gameId, Long playerId, String pseudo, boolean won,
                                  String outcome, int attempts, int maxAttempts, int wordLength,
                                  String firstLetter, String secretWord, LocalDateTime finishedAt) {
        GameResult r = new GameResult();
        r.gameId = gameId;
        r.playerId = playerId;
        r.playerPseudo = pseudo;
        r.won = won;
        r.outcome = outcome != null ? outcome : (won ? "WON" : "LOST");
        r.attempts = attempts;
        r.maxAttempts = maxAttempts;
        r.wordLength = wordLength;
        r.firstLetter = firstLetter != null && !firstLetter.isBlank()
                ? firstLetter.substring(0, 1).toUpperCase() : "?";
        r.secretWord = secretWord;
        r.finishedAt = finishedAt;
        r.score = computeScore(won, attempts, maxAttempts);
        return r;
    }

    private static int computeScore(boolean won, int attempts, int maxAttempts) {
        if (!won) return 0;
        return Math.max(0, (maxAttempts - attempts + 1) * 100);
    }

    public Long getId() { return id; }
    public Long getGameId() { return gameId; }
    public Long getPlayerId() { return playerId; }
    public String getPlayerPseudo() { return playerPseudo; }
    public boolean isWon() { return won; }
    public int getAttempts() { return attempts; }
    public int getMaxAttempts() { return maxAttempts; }
    public int getWordLength() { return wordLength; }
    public String getFirstLetter() { return firstLetter; }
    public String getSecretWord() { return secretWord; }
    public String getOutcome() { return outcome; }
    public int getScore() { return score; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
}
