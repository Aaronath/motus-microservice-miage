package fr.miage.motus.player.repository;

import fr.miage.motus.player.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    List<Player> findAllByOrderByIdAsc();
    Optional<Player> findByPseudoIgnoreCase(String pseudo);
    Optional<Player> findByEmailIgnoreCase(String email);
    boolean existsByPseudoIgnoreCase(String pseudo);
    boolean existsByEmailIgnoreCase(String email);
}
