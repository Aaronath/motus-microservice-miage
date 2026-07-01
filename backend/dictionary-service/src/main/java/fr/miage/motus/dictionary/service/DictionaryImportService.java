package fr.miage.motus.dictionary.service;

import fr.miage.motus.dictionary.config.DictionaryProperties;
import fr.miage.motus.dictionary.entity.WordEntity;
import fr.miage.motus.dictionary.model.WordGroup;
import fr.miage.motus.dictionary.entity.WordGroupEntity;
import fr.miage.motus.dictionary.repository.WordGroupRepository;
import fr.miage.motus.dictionary.repository.WordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

@Service
public class DictionaryImportService {

    private static final Logger log = LoggerFactory.getLogger(DictionaryImportService.class);
    private static final int BATCH_SIZE = 500;
    private static final long MIN_LEXIQUE_WORDS = 10_000;

    private final WordRepository wordRepository;
    private final WordGroupRepository groupRepository;
    private final DictionaryProperties properties;
    private final WordGroupsCatalog catalog;
    private final RestClient restClient;

    public DictionaryImportService(WordRepository wordRepository,
                                   WordGroupRepository groupRepository,
                                   DictionaryProperties properties,
                                   WordGroupsCatalog catalog) {
        this.wordRepository = wordRepository;
        this.groupRepository = groupRepository;
        this.properties = properties;
        this.catalog = catalog;
        this.restClient = RestClient.builder().build();
    }

    public boolean needsImport() {
        if (properties.forceReload()) {
            return true;
        }
        if (wordRepository.count() == 0 || groupRepository.count() == 0) {
            return true;
        }
        if (useLexique() && wordRepository.count() < MIN_LEXIQUE_WORDS) {
            return true;
        }
        return false;
    }

    @Transactional
    public int importDictionary() {
        wordRepository.truncateAll();
        groupRepository.deleteAll();
        Map<String, WordEntity> unique = new LinkedHashMap<>();

        if (useLexique()) {
            importFromLexique(unique);
        }
        if (useRemote()) {
            loadFromRemoteUrl(properties.wordsUrl()).forEach(w ->
                    unique.putIfAbsent(w, new WordEntity(w)));
        }
        if (useGroups()) {
            importFromCatalog(unique);
        }

        List<WordEntity> entities = new ArrayList<>(unique.values());
        for (int i = 0; i < entities.size(); i += BATCH_SIZE) {
            wordRepository.saveAll(entities.subList(i, Math.min(i + BATCH_SIZE, entities.size())));
        }
        catalog.reloadFromDatabase();
        log.info("Dictionnaire importé : {} mots, {} groupes (source={})",
                entities.size(), groupRepository.count(), properties.source());
        return entities.size();
    }

    private void importFromLexique(Map<String, WordEntity> unique) {
        String path = properties.lexiquePath() != null && !properties.lexiquePath().isBlank()
                ? properties.lexiquePath()
                : "classpath:data/lexique-motus-5-9.csv.gz";
        int added = 0;
        try (BufferedReader reader = openLexiqueReader(resolveLexiqueResource(path))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first && line.toLowerCase().startsWith("word")) {
                    first = false;
                    continue;
                }
                first = false;
                String[] parts = line.split(",", 2);
                if (parts.length < 1) {
                    continue;
                }
                String w = WordNormalizer.normalize(parts[0]);
                if (WordNormalizer.isValidMotusWord(w, properties.minLength(), properties.maxLength())) {
                    if (!unique.containsKey(w)) {
                        unique.put(w, new WordEntity(w));
                        added++;
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Import Lexique383 impossible : " + path, e);
        }
        log.info("Import lexique : {} mots depuis {}", added, path);
    }

    private Resource resolveLexiqueResource(String path) {
        if (path.startsWith("classpath:")) {
            return new ClassPathResource(path.substring("classpath:".length()));
        }
        return new FileSystemResource(path);
    }

    private BufferedReader openLexiqueReader(Resource resource) throws Exception {
        var in = resource.getInputStream();
        if (resource.getFilename() != null && resource.getFilename().endsWith(".gz")) {
            in = new GZIPInputStream(in);
        }
        return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    private void importFromCatalog(Map<String, WordEntity> unique) {
        for (WordGroup group : catalog.allGroups()) {
            groupRepository.save(new WordGroupEntity(
                    group.id(), group.name(), group.length(), group.firstLetter()));
            for (String word : group.secretWords()) {
                unique.put(word, new WordEntity(word, group.id(), true));
            }
            for (String word : group.guesses()) {
                unique.putIfAbsent(word, new WordEntity(word, group.id(), false));
            }
        }
        log.info("Import groupes : {} mots uniques, {} groupes", unique.size(), groupRepository.count());
    }

    private boolean useLexique() {
        String s = source();
        return "lexique".equalsIgnoreCase(s) || "both".equalsIgnoreCase(s);
    }

    private boolean useGroups() {
        String s = source();
        return "groups".equalsIgnoreCase(s) || "both".equalsIgnoreCase(s);
    }

    private boolean useRemote() {
        String s = source();
        return "remote".equalsIgnoreCase(s) || "both".equalsIgnoreCase(s);
    }

    private String source() {
        return properties.source() == null ? "both" : properties.source();
    }

    private Set<String> loadFromRemoteUrl(String url) {
        java.util.Set<String> words = new java.util.LinkedHashSet<>();
        try {
            byte[] body = restClient.get().uri(url).retrieve().body(byte[].class);
            if (body == null) {
                return words;
            }
            try (var reader = new BufferedReader(new InputStreamReader(
                    new java.io.ByteArrayInputStream(body), StandardCharsets.UTF_8))) {
                reader.lines().forEach(line -> {
                    String w = WordNormalizer.normalize(line);
                    if (WordNormalizer.isValidMotusWord(w, properties.minLength(), properties.maxLength())) {
                        words.add(w);
                    }
                });
            }
        } catch (Exception e) {
            log.warn("Import distant ignoré : {}", e.getMessage());
        }
        return words;
    }
}
