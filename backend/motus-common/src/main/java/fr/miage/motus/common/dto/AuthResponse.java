package fr.miage.motus.common.dto;

public record AuthResponse(String token, String type, Long playerId, String pseudo, String role) {
    public AuthResponse(String token, Long playerId, String pseudo, String role) {
        this(token, "Bearer", playerId, pseudo, role);
    }
}
