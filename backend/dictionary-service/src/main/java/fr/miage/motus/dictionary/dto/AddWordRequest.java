package fr.miage.motus.dictionary.dto;

import jakarta.validation.constraints.NotBlank;

public record AddWordRequest(@NotBlank String word, boolean secretWord) {}
