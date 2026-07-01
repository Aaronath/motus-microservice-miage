package fr.miage.motus.player.service;

import fr.miage.motus.common.dto.AuthResponse;
import fr.miage.motus.common.security.JwtService;
import fr.miage.motus.player.entity.Player;
import fr.miage.motus.player.entity.PlayerRole;
import fr.miage.motus.player.repository.PlayerRepository;
import fr.miage.motus.player.dto.LoginRequest;
import fr.miage.motus.player.dto.PlayerResponse;
import fr.miage.motus.player.dto.RegisterRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public PlayerService(PlayerRepository playerRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.playerRepository = playerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (playerRepository.existsByPseudoIgnoreCase(request.pseudo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pseudo déjà utilisé");
        }
        if (playerRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email déjà utilisé");
        }
        PlayerRole role = "MIAGE-ADMIN-2026".equals(request.adminCode()) ? PlayerRole.ADMIN : PlayerRole.JOUEUR;
        Player player = new Player(
                request.pseudo().trim(),
                request.email().trim().toLowerCase(),
                passwordEncoder.encode(request.password()),
                role
        );
        player = playerRepository.save(player);
        String token = jwtService.generateToken(player.getId(), player.getPseudo(), player.getRole().name());
        return new AuthResponse(token, player.getId(), player.getPseudo(), player.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        Player player = playerRepository.findByPseudoIgnoreCase(request.pseudo())
                .or(() -> playerRepository.findByEmailIgnoreCase(request.pseudo()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides"));
        if (!passwordEncoder.matches(request.password(), player.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides");
        }
        String token = jwtService.generateToken(player.getId(), player.getPseudo(), player.getRole().name());
        return new AuthResponse(token, player.getId(), player.getPseudo(), player.getRole().name());
    }

    public PlayerResponse getById(Long id) {
        return playerRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Joueur introuvable"));
    }

    public List<PlayerResponse> listAll() {
        return playerRepository.findAllByOrderByIdAsc().stream().map(this::toResponse).toList();
    }

    public PlayerResponse updateRole(Long id, String role) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Joueur introuvable"));
        try {
            player.setRole(PlayerRole.valueOf(role.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rôle invalide (JOUEUR ou ADMIN)");
        }
        return toResponse(playerRepository.save(player));
    }

    public void deletePlayer(Long id) {
        if (!playerRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Joueur introuvable");
        }
        playerRepository.deleteById(id);
    }

    private PlayerResponse toResponse(Player p) {
        return new PlayerResponse(p.getId(), p.getPseudo(), p.getEmail(), p.getRole().name(), p.getRegisteredAt());
    }
}
