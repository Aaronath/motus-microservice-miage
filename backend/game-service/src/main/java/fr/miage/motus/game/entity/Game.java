package fr.miage.motus.game.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "games")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long playerId;

    @Column(nullable = false)
    private String playerPseudo;

    @Column(nullable = false, length = 20)
    private String secretWord;

    @Column(nullable = false)
    private int wordLength;

    @Column(nullable = false)
    private int maxAttempts;

    @Column(nullable = false)
    private int attemptsUsed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus status = GameStatus.IN_PROGRESS;

    @Column(nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    private LocalDateTime finishedAt;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("attemptNumber ASC")
    private List<Guess> guesses = new ArrayList<>();

    protected Game() {}

    public Game(Long playerId, String playerPseudo, String secretWord, int maxAttempts) {
        this.playerId = playerId;
        this.playerPseudo = playerPseudo;
        this.secretWord = secretWord.toUpperCase();
        this.wordLength = this.secretWord.length();
        this.maxAttempts = maxAttempts;
        this.attemptsUsed = 0;
    }

    public Long getId() { return id; }
    public Long getPlayerId() { return playerId; }
    public String getPlayerPseudo() { return playerPseudo; }
    public String getSecretWord() { return secretWord; }
    public int getWordLength() { return wordLength; }
    public int getMaxAttempts() { return maxAttempts; }
    public int getAttemptsUsed() { return attemptsUsed; }
    public GameStatus getStatus() { return status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public List<Guess> getGuesses() { return guesses; }

    public char getFirstLetter() { return secretWord.charAt(0); }

    public int remainingAttempts() { return maxAttempts - attemptsUsed; }

    public void addGuess(Guess guess) {
        guesses.add(guess);
        guess.setGame(this);
        attemptsUsed++;
    }

    public void markWon() {
        status = GameStatus.WON;
        finishedAt = LocalDateTime.now();
    }

    public void markLost() {
        status = GameStatus.LOST;
        finishedAt = LocalDateTime.now();
    }

    public void markAbandoned() {
        status = GameStatus.ABANDONED;
        finishedAt = LocalDateTime.now();
    }
}
