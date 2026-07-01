package fr.miage.motus.dictionary.security;

import fr.miage.motus.common.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AdminAuth {

    private final JwtService jwtService;

    public AdminAuth(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public void requireAdmin(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null || !jwtService.isValid(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié");
        }
        if (!"ADMIN".equals(jwtService.getRole(token))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rôle administrateur requis");
        }
    }

    private String extractToken(HttpServletRequest request) {
        String h = request.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            return h.substring(7);
        }
        return null;
    }
}
