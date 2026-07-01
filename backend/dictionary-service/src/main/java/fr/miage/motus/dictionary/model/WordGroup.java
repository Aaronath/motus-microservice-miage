package fr.miage.motus.dictionary.model;

import java.util.List;

public record WordGroup(
        String id,
        String name,
        int length,
        String firstLetter,
        List<String> secretWords,
        List<String> guesses
) {
    /** Mots utilisés pour tirer le mot mystère */
    public List<String> words() {
        return secretWords;
    }
}
