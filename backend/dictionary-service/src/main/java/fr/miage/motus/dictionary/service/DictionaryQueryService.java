package fr.miage.motus.dictionary.service;

import fr.miage.motus.dictionary.client.DicolinkClient;
import fr.miage.motus.dictionary.repository.WordRepository;
import fr.miage.motus.dictionary.dto.RandomWordResponse;
import fr.miage.motus.dictionary.dto.WordExistsResponse;
import fr.miage.motus.dictionary.dto.WordGroupDto;
import fr.miage.motus.dictionary.config.DictionaryProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DictionaryQueryService {

    private final WordRepository wordRepository;
    private final WordGroupsCatalog catalog;
    private final DicolinkClient dicolinkClient;
    private final DictionaryProperties properties;

    public DictionaryQueryService(WordRepository wordRepository,
                                  WordGroupsCatalog catalog,
                                  DicolinkClient dicolinkClient,
                                  DictionaryProperties properties) {
        this.wordRepository = wordRepository;
        this.catalog = catalog;
        this.dicolinkClient = dicolinkClient;
        this.properties = properties;
    }

    public RandomWordResponse randomWord(int length) {
        if (length < properties.minLength() || length > properties.maxLength()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Longueur supportée : entre " + properties.minLength() + " et " + properties.maxLength() + " lettres");
        }
        return catalog.randomWord(length)
                .map(p -> new RandomWordResponse(
                        p.word(), p.word().length(), p.groupId(), p.groupName(), String.valueOf(p.firstLetter())))
                .or(() -> wordRepository.findRandomByLength(length)
                        .map(w -> new RandomWordResponse(w.getWord(), w.getLength(), w.getGroupCode(), null,
                                String.valueOf(w.getWord().charAt(0)))))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucun mot de " + length + " lettres disponible"));
    }

    public WordExistsResponse exists(String word) {
        String normalized = WordNormalizer.normalize(word);
        boolean exists = catalog.contains(normalized)
                || wordRepository.existsByWordIgnoreCase(normalized);
        return new WordExistsResponse(normalized, exists);
    }

    public List<String> search(String q) {
        String n = WordNormalizer.normalize(q);
        if (n.length() < 2) {
            return List.of();
        }
        return wordRepository.findByWordContainingIgnoreCase(n).stream()
                .map(w -> w.getWord())
                .sorted()
                .limit(50)
                .toList();
    }

    public List<WordGroupDto> listGroups() {
        return catalog.allGroups().stream()
                .map(g -> new WordGroupDto(
                        g.id(), g.name(), g.length(), g.firstLetter(),
                        g.secretWords().size(), g.guesses().size(),
                        g.secretWords()))
                .toList();
    }

    public Map<String, Object> stats() {
        int min = properties.minLength();
        int max = properties.maxLength();
        Map<String, Long> byLength = java.util.stream.IntStream.rangeClosed(min, max)
                .boxed()
                .collect(Collectors.toMap(
                        i -> String.valueOf(i),
                        wordRepository::countByLength));
        Map<String, Integer> lexiconSizes = new java.util.HashMap<>();
        for (int len = min; len <= max; len++) {
            lexiconSizes.put(String.valueOf(len), catalog.lexiconForLength(len).size());
        }
        return Map.of(
                "totalWords", wordRepository.count(),
                "byLength", byLength,
                "groupCount", catalog.allGroups().size(),
                "lexiconByLength", lexiconSizes,
                "source", "lexique383",
                "dicolinkEnabled", dicolinkClient.isEnabled()
        );
    }

    /** Vérification externe (admin / debug), hors chemin de jeu. */
    public boolean existsViaDicolink(String word) {
        return dicolinkClient.existsInDictionary(WordNormalizer.normalize(word));
    }
}
