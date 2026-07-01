package fr.miage.motus.game.dto;

import fr.miage.motus.common.dto.LetterFeedback;

import java.util.List;

public record GuessDto(int attemptNumber, String word, List<LetterFeedback> letters) {}
