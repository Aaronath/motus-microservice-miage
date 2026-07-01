package fr.miage.motus.game.dto;

import fr.miage.motus.common.dto.LetterFeedback;

import java.util.List;

public record GuessResponse(
        int attemptNumber,
        boolean valid,
        List<LetterFeedback> letters,
        String gameStatus,
        int remainingAttempts,
        String secretWord
) {}
