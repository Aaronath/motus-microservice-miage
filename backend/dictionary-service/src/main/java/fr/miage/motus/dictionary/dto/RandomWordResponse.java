package fr.miage.motus.dictionary.dto;

public record RandomWordResponse(
        String word,
        int length,
        String groupId,
        String groupName,
        String firstLetter
) {
    /** Compatibilité JSON existante */
    public RandomWordResponse(String word, int length) {
        this(word, length, null, null, word.isEmpty() ? null : String.valueOf(word.charAt(0)));
    }
}
