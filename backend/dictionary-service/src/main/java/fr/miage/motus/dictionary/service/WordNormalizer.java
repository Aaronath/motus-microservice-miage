package fr.miage.motus.dictionary.service;

import java.text.Normalizer;

public final class WordNormalizer {

    private WordNormalizer() {}

    public static String normalize(String word) {
        if (word == null || word.isBlank()) {
            return "";
        }
        String nfd = Normalizer.normalize(word.trim(), Normalizer.Form.NFD);
        String withoutAccents = nfd.replaceAll("\\p{M}", "");
        return withoutAccents.toUpperCase();
    }

    public static boolean isValidMotusWord(String word, int minLen, int maxLen) {
        String w = normalize(word);
        return w.length() >= minLen && w.length() <= maxLen && w.matches("[A-Z]+");
    }
}
