package fr.miage.motus.dictionary.dto;

import java.util.List;

public record WordGroupDto(
        String id,
        String name,
        int length,
        String firstLetter,
        int secretWordCount,
        int guessWordCount,
        List<String> secretWords
) {}
