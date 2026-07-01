package fr.miage.motus.dictionary.dto;

public record AdminWordSearchDto(
        Long id,
        String word,
        boolean secretWord,
        String groupCode,
        String groupName
) {}
