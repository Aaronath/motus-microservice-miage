package fr.miage.motus.stats.repository;

import fr.miage.motus.stats.entity.GameResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GameResultRepository extends JpaRepository<GameResult, Long> {

    Optional<GameResult> findByGameId(Long gameId);

    List<GameResult> findByPlayerIdOrderByFinishedAtDesc(Long playerId);

    @Query("""
        SELECT g.playerId as playerId, g.playerPseudo as pseudo,
               SUM(CASE WHEN g.won = true THEN 1 ELSE 0 END) as wins,
               COUNT(g) as totalGames,
               SUM(g.score) as totalScore
        FROM GameResult g
        GROUP BY g.playerId, g.playerPseudo
        ORDER BY totalScore DESC, wins DESC
        """)
    List<LeaderboardProjection> leaderboard();

    interface LeaderboardProjection {
        Long getPlayerId();
        String getPseudo();
        Long getWins();
        Long getTotalGames();
        Long getTotalScore();
    }
}
