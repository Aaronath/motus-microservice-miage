package fr.miage.motus.dictionary.service;

import fr.miage.motus.dictionary.entity.WordEntity;
import fr.miage.motus.dictionary.entity.WordGroupEntity;
import fr.miage.motus.dictionary.repository.WordGroupRepository;
import fr.miage.motus.dictionary.repository.WordRepository;
import fr.miage.motus.dictionary.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DictionaryAdminService {

    private final WordGroupRepository groupRepository;
    private final WordRepository wordRepository;
    private final WordGroupsCatalog catalog;

    public DictionaryAdminService(WordGroupRepository groupRepository, WordRepository wordRepository,
                                  WordGroupsCatalog catalog) {
        this.groupRepository = groupRepository;
        this.wordRepository = wordRepository;
        this.catalog = catalog;
    }

    @Transactional(readOnly = true)
    public List<WordGroupDto> listGroups() {
        return groupRepository.findAll().stream().map(this::toGroupDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminWordSearchDto> searchWords(String q) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        String needle = q.trim();
        Map<String, String> groupNames = groupRepository.findAll().stream()
                .collect(Collectors.toMap(WordGroupEntity::getId, WordGroupEntity::getName, (a, b) -> a));
        return wordRepository.findByWordContainingIgnoreCase(needle).stream()
                .limit(80)
                .map(w -> {
                    String code = w.getGroupCode();
                    String name = (code == null || code.isBlank())
                            ? "Sans groupe"
                            : groupNames.getOrDefault(code, code);
                    return new AdminWordSearchDto(w.getId(), w.getWord(), w.isSecretWord(), code, name);
                })
                .toList();
    }

    @Transactional
    public WordGroupDto createGroup(CreateGroupRequest req) {
        String id = (req.id() != null && !req.id().isBlank())
                ? req.id().trim()
                : uniqueGroupId(req.name());
        if (groupRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Groupe déjà existant");
        }
        String letter = req.firstLetter().toUpperCase().substring(0, 1);
        WordGroupEntity g = new WordGroupEntity(id, req.name().trim(), req.length(), letter);
        groupRepository.save(g);
        catalog.reloadFromDatabase();
        return toGroupDto(g);
    }

    @Transactional
    public WordGroupDto updateGroup(String id, UpdateGroupRequest req) {
        WordGroupEntity g = groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Groupe introuvable"));
        g.setName(req.name().trim());
        catalog.reloadFromDatabase();
        return toGroupDto(g);
    }

    @Transactional
    public void deleteGroup(String id) {
        if (!groupRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Groupe introuvable");
        }
        wordRepository.findByGroupCodeOrderByWordAsc(id).forEach(w -> wordRepository.delete(w));
        groupRepository.deleteById(id);
        catalog.reloadFromDatabase();
    }

    @Transactional(readOnly = true)
    public AdminWordDto getWord(Long wordId) {
        return wordRepository.findById(wordId)
                .map(this::toWordDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mot introuvable"));
    }

    @Transactional(readOnly = true)
    public List<AdminWordDto> listUngroupedWords() {
        return wordRepository.findUngroupedOrderByWordAsc().stream().map(this::toWordDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminWordDto> listWords(String groupId) {
        ensureGroup(groupId);
        return wordRepository.findByGroupCodeOrderByWordAsc(groupId).stream().map(this::toWordDto).toList();
    }

    @Transactional
    public AdminWordDto addWord(String groupId, AddWordRequest req) {
        WordGroupEntity g = ensureGroup(groupId);
        String word = WordNormalizer.normalize(req.word());
        validateWordForGroup(word, g);
        if (wordRepository.existsByWordIgnoreCase(word)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mot déjà présent");
        }
        WordEntity entity = new WordEntity(word, groupId, req.secretWord());
        entity = wordRepository.save(entity);
        catalog.reloadFromDatabase();
        return toWordDto(entity);
    }

    @Transactional
    public AdminWordDto updateWord(Long wordId, UpdateWordRequest req) {
        WordEntity entity = wordRepository.findById(wordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mot introuvable"));
        String word = WordNormalizer.normalize(req.word());
        String groupCode = resolveGroupCode(req.groupCode(), entity.getGroupCode());

        if (groupCode != null && !groupCode.isBlank()) {
            validateWordForGroup(word, ensureGroup(groupCode));
            entity.setGroupCode(groupCode);
        } else {
            entity.setGroupCode(null);
        }

        if (!entity.getWord().equalsIgnoreCase(word) && wordRepository.existsByWordIgnoreCase(word)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mot déjà présent");
        }
        entity.setWord(word);
        entity.setSecretWord(req.secretWord());
        entity = wordRepository.save(entity);
        catalog.reloadFromDatabase();
        return toWordDto(entity);
    }

    private static String resolveGroupCode(String requested, String current) {
        if (requested == null) {
            return current;
        }
        String trimmed = requested.trim();
        if (trimmed.isEmpty() || "__ungrouped__".equals(trimmed)) {
            return null;
        }
        return trimmed;
    }

    @Transactional
    public void deleteWord(Long wordId) {
        if (!wordRepository.existsById(wordId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mot introuvable");
        }
        wordRepository.deleteById(wordId);
        catalog.reloadFromDatabase();
    }

    private WordGroupEntity ensureGroup(String groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Groupe introuvable"));
    }

    private void validateWordForGroup(String word, WordGroupEntity g) {
        if (word.length() != g.getLength()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le mot doit avoir " + g.getLength() + " lettres");
        }
        if (word.charAt(0) != g.getFirstLetter().charAt(0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le mot doit commencer par " + g.getFirstLetter());
        }
    }

    private WordGroupDto toGroupDto(WordGroupEntity g) {
        long secrets = wordRepository.findByGroupCodeAndSecretWordTrue(g.getId()).size();
        long total = wordRepository.findByGroupCodeOrderByWordAsc(g.getId()).size();
        return new WordGroupDto(g.getId(), g.getName(), g.getLength(), g.getFirstLetter(),
                (int) secrets, (int) total, List.of());
    }

    private AdminWordDto toWordDto(WordEntity w) {
        return new AdminWordDto(w.getId(), w.getWord(), w.getLength(), w.getGroupCode(), w.isSecretWord());
    }

    private String uniqueGroupId(String name) {
        String base = slugify(name);
        String id = base;
        int n = 1;
        while (groupRepository.existsById(id)) {
            id = base + "-" + n++;
        }
        return id;
    }

    private static String slugify(String name) {
        String base = name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (base.isBlank()) {
            return "groupe";
        }
        return base.length() > 50 ? base.substring(0, 50) : base;
    }
}
