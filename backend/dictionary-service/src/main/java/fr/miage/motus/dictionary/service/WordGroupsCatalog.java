package fr.miage.motus.dictionary.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.miage.motus.dictionary.model.WordGroup;
import fr.miage.motus.dictionary.entity.WordGroupEntity;
import fr.miage.motus.dictionary.repository.WordGroupRepository;
import fr.miage.motus.dictionary.repository.WordRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Component
public class WordGroupsCatalog {

    private static final Logger log = LoggerFactory.getLogger(WordGroupsCatalog.class);

    private final ObjectMapper objectMapper;
    private final WordGroupRepository groupRepository;
    private final WordRepository wordRepository;
    private List<WordGroup> groups = List.of();
    private Map<Integer, Set<String>> lexiconByLength = Map.of();

    public WordGroupsCatalog(ObjectMapper objectMapper, WordGroupRepository groupRepository,
                             WordRepository wordRepository) {
        this.objectMapper = objectMapper;
        this.groupRepository = groupRepository;
        this.wordRepository = wordRepository;
    }

    @PostConstruct
    void load() throws Exception {
        if (wordRepository.count() > 0 && groupRepository.count() > 0) {
            reloadFromDatabase();
        } else {
            loadGroupsFromJson();
        }
    }

    public void reloadFromDatabase() {
        if (wordRepository.count() == 0) {
            return;
        }
        Map<Integer, Set<String>> lexicon = new HashMap<>();
        wordRepository.findAll().forEach(w ->
                lexicon.computeIfAbsent(w.getLength(), k -> new HashSet<>()).add(w.getWord()));
        this.lexiconByLength = lexicon.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> Set.copyOf(e.getValue())));

        if (groupRepository.count() == 0) {
            this.groups = List.of();
            log.info("Lexique BDD : {} longueurs, {} mots", lexiconByLength.size(),
                    lexiconByLength.values().stream().mapToInt(Set::size).sum());
            return;
        }

        List<WordGroup> loaded = new ArrayList<>();
        for (WordGroupEntity ge : groupRepository.findAll()) {
            List<String> secrets = filterWordsInLexicon(
                    wordRepository.findByGroupCodeAndSecretWordTrue(ge.getId()).stream()
                            .map(w -> w.getWord()).toList(),
                    ge.getLength());
            List<String> guesses = wordRepository.findByGroupCodeOrderByWordAsc(ge.getId()).stream()
                    .map(w -> w.getWord())
                    .filter(w -> lexiconByLength.getOrDefault(ge.getLength(), Set.of()).contains(w))
                    .toList();
            if (secrets.isEmpty()) {
                continue;
            }
            loaded.add(new WordGroup(ge.getId(), ge.getName(), ge.getLength(), ge.getFirstLetter(), secrets, guesses));
        }
        this.groups = List.copyOf(loaded);
        log.info("Catalogue BDD : {} groupes, lexique {} mots",
                groups.size(), lexiconByLength.values().stream().mapToInt(Set::size).sum());
    }

    private void loadGroupsFromJson() throws Exception {
        var resource = new ClassPathResource("word-groups.json");
        JsonNode root = objectMapper.readTree(resource.getInputStream());
        if (!root.has("groups")) {
            log.warn("word-groups.json sans section groups");
            return;
        }
        List<WordGroup> loaded = new ArrayList<>();
        for (JsonNode node : root.get("groups")) {
            String id = node.get("id").asText();
            String name = node.get("name").asText();
            int length = node.get("length").asInt();
            String firstLetter = node.get("firstLetter").asText();
            List<String> secrets = readWordList(node, "secretWords", length, firstLetter);
            if (secrets.isEmpty()) {
                secrets = readWordList(node, "words", length, firstLetter);
            }
            List<String> guesses = readWordList(node, "guesses", length, firstLetter);
            if (secrets.size() >= 6) {
                loaded.add(new WordGroup(id, name, length, firstLetter, secrets, guesses));
            }
        }
        this.groups = List.copyOf(loaded);
        log.info("Catalogue JSON : {} groupes (lexique chargé après import BDD)", groups.size());
    }

    private List<String> filterWordsInLexicon(List<String> words, int length) {
        Set<String> lexicon = lexiconByLength.getOrDefault(length, Set.of());
        if (lexicon.isEmpty()) {
            return words.stream().distinct().toList();
        }
        return words.stream().filter(lexicon::contains).distinct().toList();
    }

    private List<String> readWordList(JsonNode node, String field, int length, String firstLetter) {
        if (!node.has(field)) {
            return List.of();
        }
        List<String> words = new ArrayList<>();
        for (JsonNode w : node.get(field)) {
            String word = WordNormalizer.normalize(w.asText());
            if (WordNormalizer.isValidMotusWord(word, length, length) && word.charAt(0) == firstLetter.charAt(0)) {
                words.add(word);
            }
        }
        return words.stream().distinct().toList();
    }

    public List<WordGroup> allGroups() {
        return groups;
    }

    public Optional<WordGroup> randomGroup(int length) {
        List<WordGroup> candidates = groups.stream().filter(g -> g.length() == length).toList();
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(candidates.get(ThreadLocalRandom.current().nextInt(candidates.size())));
    }

    public Optional<RandomPick> randomWord(int length) {
        return randomGroup(length).flatMap(g -> {
            List<String> secrets = filterWordsInLexicon(g.secretWords(), length);
            if (secrets.isEmpty()) {
                return Optional.empty();
            }
            String word = secrets.get(ThreadLocalRandom.current().nextInt(secrets.size()));
            return Optional.of(new RandomPick(word, g.id(), g.name(), g.firstLetter()));
        });
    }

    public boolean contains(String word) {
        String n = WordNormalizer.normalize(word);
        if (n.isEmpty()) {
            return false;
        }
        Set<String> lexicon = lexiconByLength.get(n.length());
        return lexicon != null && lexicon.contains(n);
    }

    public Set<String> lexiconForLength(int length) {
        return lexiconByLength.getOrDefault(length, Set.of());
    }

    public record RandomPick(String word, String groupId, String groupName, String firstLetter) {}
}
