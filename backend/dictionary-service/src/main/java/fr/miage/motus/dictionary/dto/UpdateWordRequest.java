package fr.miage.motus.dictionary.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateWordRequest(
        @NotBlank String word,
        boolean secretWord,
        String groupCode
) {}
