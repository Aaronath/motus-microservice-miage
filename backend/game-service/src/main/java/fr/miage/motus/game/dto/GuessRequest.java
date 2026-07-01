package fr.miage.motus.game.dto;

import jakarta.validation.constraints.NotBlank;

public record GuessRequest(@NotBlank String word) {}
