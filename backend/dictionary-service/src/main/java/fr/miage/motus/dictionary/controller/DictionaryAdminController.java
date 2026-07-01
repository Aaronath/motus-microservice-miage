package fr.miage.motus.dictionary.controller;

import fr.miage.motus.dictionary.security.AdminAuth;
import fr.miage.motus.dictionary.service.DictionaryAdminService;
import fr.miage.motus.dictionary.service.DictionaryImportService;
import fr.miage.motus.dictionary.service.DictionaryQueryService;
import fr.miage.motus.dictionary.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dictionary/admin")
public class DictionaryAdminController {

    private final DictionaryAdminService adminService;
    private final DictionaryImportService importService;
    private final DictionaryQueryService queryService;
    private final AdminAuth adminAuth;

    public DictionaryAdminController(DictionaryAdminService adminService,
                                     DictionaryImportService importService,
                                     DictionaryQueryService queryService,
                                     AdminAuth adminAuth) {
        this.adminService = adminService;
        this.importService = importService;
        this.queryService = queryService;
        this.adminAuth = adminAuth;
    }

    @GetMapping("/groups")
    public List<WordGroupDto> listGroups(HttpServletRequest http) {
        adminAuth.requireAdmin(http);
        return adminService.listGroups();
    }

    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public WordGroupDto createGroup(@Valid @RequestBody CreateGroupRequest req, HttpServletRequest http) {
        adminAuth.requireAdmin(http);
        return adminService.createGroup(req);
    }

    @PutMapping("/groups/{id}")
    public WordGroupDto updateGroup(@PathVariable String id, @Valid @RequestBody UpdateGroupRequest req,
                                    HttpServletRequest http) {
        adminAuth.requireAdmin(http);
        return adminService.updateGroup(id, req);
    }

    @DeleteMapping("/groups/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(@PathVariable String id, HttpServletRequest http) {
        adminAuth.requireAdmin(http);
        adminService.deleteGroup(id);
    }

    @GetMapping("/words/search")
    public List<AdminWordSearchDto> searchWords(@RequestParam String q, HttpServletRequest http) {
        adminAuth.requireAdmin(http);
        return adminService.searchWords(q);
    }

    @GetMapping("/words/ungrouped")
    public List<AdminWordDto> listUngroupedWords(HttpServletRequest http) {
        adminAuth.requireAdmin(http);
        return adminService.listUngroupedWords();
    }

    @GetMapping("/words/{wordId}")
    public AdminWordDto getWord(@PathVariable Long wordId, HttpServletRequest http) {
        adminAuth.requireAdmin(http);
        return adminService.getWord(wordId);
    }

    @GetMapping("/groups/{id}/words")
    public List<AdminWordDto> listWords(@PathVariable String id, HttpServletRequest http) {
        adminAuth.requireAdmin(http);
        return adminService.listWords(id);
    }

    @PostMapping("/groups/{id}/words")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminWordDto addWord(@PathVariable String id, @Valid @RequestBody AddWordRequest req,
                                HttpServletRequest http) {
        adminAuth.requireAdmin(http);
        return adminService.addWord(id, req);
    }

    @PutMapping("/words/{wordId}")
    public AdminWordDto updateWord(@PathVariable Long wordId, @Valid @RequestBody UpdateWordRequest req,
                                   HttpServletRequest http) {
        adminAuth.requireAdmin(http);
        return adminService.updateWord(wordId, req);
    }

    @DeleteMapping("/words/{wordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWord(@PathVariable Long wordId, HttpServletRequest http) {
        adminAuth.requireAdmin(http);
        adminService.deleteWord(wordId);
    }

    @PostMapping("/reload")
    public ResponseEntity<Map<String, Object>> reload(HttpServletRequest http) {
        adminAuth.requireAdmin(http);
        int count = importService.importDictionary();
        return ResponseEntity.ok(Map.of("imported", count, "stats", queryService.stats()));
    }
}
