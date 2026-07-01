package fr.miage.motus.player.controller;

import fr.miage.motus.common.dto.AuthResponse;
import fr.miage.motus.player.service.PlayerService;
import fr.miage.motus.player.dto.LoginRequest;
import fr.miage.motus.player.dto.PlayerResponse;
import fr.miage.motus.player.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return playerService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return playerService.login(request);
    }

    @GetMapping("/me")
    public PlayerResponse me(@AuthenticationPrincipal String playerId) {
        return playerService.getById(Long.parseLong(playerId));
    }

    @GetMapping("/{id}")
    public PlayerResponse getById(@PathVariable Long id) {
        return playerService.getById(id);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "player-service");
    }
}
