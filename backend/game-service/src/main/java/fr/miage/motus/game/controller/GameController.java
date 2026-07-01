package fr.miage.motus.game.controller;

import fr.miage.motus.common.security.JwtService;
import fr.miage.motus.game.entity.GameStatus;
import fr.miage.motus.game.service.GamePlayService;
import fr.miage.motus.game.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GamePlayService gamePlayService;
    private final JwtService jwtService;

    public GameController(GamePlayService gamePlayService, JwtService jwtService) {
        this.gamePlayService = gamePlayService;
        this.jwtService = jwtService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GameResponse create(@Valid @RequestBody(required = false) CreateGameRequest request,
                               HttpServletRequest http) {
        var claims = requireAuth(http);
        Long playerId = jwtService.getPlayerId(token(http));
        String pseudo = claims.get("pseudo", String.class);
        return gamePlayService.createGame(playerId, pseudo, request != null ? request : new CreateGameRequest(null, null));
    }

    @GetMapping("/{id}")
    public GameResponse get(@PathVariable Long id, HttpServletRequest http) {
        var claims = requireAuth(http);
        Long playerId = jwtService.getPlayerId(token(http));
        boolean admin = "ADMIN".equals(claims.get("role", String.class));
        return gamePlayService.getGame(id, playerId, admin);
    }

    @PostMapping("/{id}/abandon")
    public GameResponse abandon(@PathVariable Long id, HttpServletRequest http) {
        requireAuth(http);
        Long playerId = jwtService.getPlayerId(token(http));
        return gamePlayService.abandonGame(id, playerId);
    }

    @PostMapping("/{id}/guesses")
    public GuessResponse guess(@PathVariable Long id, @Valid @RequestBody GuessRequest request,
                               HttpServletRequest http) {
        requireAuth(http);
        Long playerId = jwtService.getPlayerId(token(http));
        return gamePlayService.submitGuess(id, playerId, request);
    }

    @GetMapping("/history/me")
    public List<GameSummaryDto> myHistory(HttpServletRequest http) {
        requireAuth(http);
        Long playerId = jwtService.getPlayerId(token(http));
        return gamePlayService.playerHistory(playerId);
    }

    @DeleteMapping("/admin/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void adminDelete(@PathVariable Long id, HttpServletRequest http) {
        requireAdmin(http);
        gamePlayService.deleteGame(id);
    }

    @GetMapping("/admin/search")
    public List<AdminGameDto> adminSearch(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) GameStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            HttpServletRequest http) {
        requireAdmin(http);
        return gamePlayService.adminSearch(q, status, from, to);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "game-service");
    }

    private io.jsonwebtoken.Claims requireAuth(HttpServletRequest http) {
        String t = token(http);
        if (t == null || !jwtService.isValid(t)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié");
        }
        return jwtService.parseClaims(t);
    }

    private void requireAdmin(HttpServletRequest http) {
        var claims = requireAuth(http);
        if (!"ADMIN".equals(claims.get("role", String.class))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rôle administrateur requis");
        }
    }

    private String token(HttpServletRequest http) {
        String h = http.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            return h.substring(7);
        }
        return null;
    }
}
