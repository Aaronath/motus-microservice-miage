package fr.miage.motus.game.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "guesses")
public class Guess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(nullable = false, length = 20)
    private String proposedWord;

    @Column(nullable = false)
    private int attemptNumber;

    @Column(nullable = false, length = 2000)
    private String feedbackJson;

    @Column(nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();

    protected Guess() {}

    public Guess(String proposedWord, int attemptNumber, String feedbackJson) {
        this.proposedWord = proposedWord.toUpperCase();
        this.attemptNumber = attemptNumber;
        this.feedbackJson = feedbackJson;
    }

    public void setGame(Game game) { this.game = game; }

    public Long getId() { return id; }
    public String getProposedWord() { return proposedWord; }
    public int getAttemptNumber() { return attemptNumber; }
    public String getFeedbackJson() { return feedbackJson; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
}
