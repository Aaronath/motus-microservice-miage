package fr.miage.motus.player.controller;

import fr.miage.motus.player.service.PlayerService;
import fr.miage.motus.player.dto.PlayerResponse;
import fr.miage.motus.player.dto.UpdatePlayerRoleRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players/admin")
@PreAuthorize("hasRole('ADMIN')")
public class PlayerAdminController {

    private final PlayerService playerService;

    public PlayerAdminController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping
    public List<PlayerResponse> list() {
        return playerService.listAll();
    }

    @PatchMapping("/{id}/role")
    public PlayerResponse updateRole(@PathVariable Long id, @Valid @RequestBody UpdatePlayerRoleRequest req) {
        return playerService.updateRole(id, req.role());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        playerService.deletePlayer(id);
    }
}
