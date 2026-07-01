package fr.miage.motus.dictionary.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "motus.dictionary")
public record DictionaryProperties(
        /** lexique | groups | both (lexique+groups) | remote (deprecated) */
        String source,
        String wordsUrl,
        /** Classpath or filesystem path to lexique-motus CSV/CSV.GZ */
        String lexiquePath,
        int minLength,
        int maxLength,
        int minWordsPerLength,
        boolean forceReload,
        String dicolinkApiKey
) {}
