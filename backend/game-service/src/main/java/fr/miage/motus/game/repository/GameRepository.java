package fr.miage.motus.game.repository;

import fr.miage.motus.game.entity.Game;
import fr.miage.motus.game.entity.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface GameRepository extends JpaRepository<Game, Long> {

    List<Game> findByPlayerIdOrderByStartedAtDesc(Long playerId);

    @Query("SELECT g FROM Game g WHERE (:status IS NULL OR g.status = :status) " +
           "AND g.startedAt >= COALESCE(:from, g.startedAt) " +
           "AND g.startedAt <= COALESCE(:to, g.startedAt) " +
           "ORDER BY g.startedAt DESC")
    List<Game> findAllForAdmin(@Param("status") GameStatus status,
                               @Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to);

    @Query("SELECT g FROM Game g WHERE (:status IS NULL OR g.status = :status) " +
           "AND (LOWER(g.playerPseudo) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "     OR LOWER(g.secretWord) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "     OR (:gameId IS NOT NULL AND g.id = :gameId)) " +
           "AND g.startedAt >= COALESCE(:from, g.startedAt) " +
           "AND g.startedAt <= COALESCE(:to, g.startedAt) " +
           "ORDER BY g.startedAt DESC")
    List<Game> searchByText(@Param("q") String q,
                            @Param("gameId") Long gameId,
                            @Param("status") GameStatus status,
                            @Param("from") LocalDateTime from,
                            @Param("to") LocalDateTime to);
}
