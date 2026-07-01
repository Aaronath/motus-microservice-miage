package fr.miage.motus.player.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "players")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String pseudo;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlayerRole role = PlayerRole.JOUEUR;

    @Column(nullable = false)
    private LocalDateTime registeredAt = LocalDateTime.now();

    protected Player() {}

    public Player(String pseudo, String email, String passwordHash, PlayerRole role) {
        this.pseudo = pseudo;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role != null ? role : PlayerRole.JOUEUR;
    }

    public Long getId() { return id; }
    public String getPseudo() { return pseudo; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public PlayerRole getRole() { return role; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }

    public void setRole(PlayerRole role) {
        this.role = role != null ? role : PlayerRole.JOUEUR;
    }
}
