package fr.miage.motus.dictionary.controller;

import fr.miage.motus.dictionary.service.DictionaryQueryService;
import fr.miage.motus.dictionary.dto.RandomWordResponse;
import fr.miage.motus.dictionary.dto.WordExistsResponse;
import fr.miage.motus.dictionary.dto.WordGroupDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dictionary")
public class DictionaryController {

    private final DictionaryQueryService queryService;

    public DictionaryController(DictionaryQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/words/random")
    public RandomWordResponse randomWord(@RequestParam(defaultValue = "7") int length) {
        return queryService.randomWord(length);
    }

    @GetMapping("/words/{word}/exists")
    public WordExistsResponse exists(@PathVariable String word) {
        return queryService.exists(word);
    }

    @GetMapping("/words/search")
    public List<String> search(@RequestParam String q) {
        return queryService.search(q);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return queryService.stats();
    }

    @GetMapping("/groups")
    public List<WordGroupDto> groups() {
        return queryService.listGroups();
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "dictionary-service");
    }
}
