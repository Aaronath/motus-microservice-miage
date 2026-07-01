package fr.miage.motus.dictionary.repository;

import fr.miage.motus.dictionary.entity.WordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WordRepository extends JpaRepository<WordEntity, Long> {

    Optional<WordEntity> findByWordIgnoreCase(String word);

    boolean existsByWordIgnoreCase(String word);

    @Query(value = "SELECT * FROM words WHERE length = :len ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<WordEntity> findRandomByLength(@Param("len") int length);

    List<WordEntity> findByWordContainingIgnoreCase(String fragment);

    long countByLength(int length);

    List<WordEntity> findByGroupCodeOrderByWordAsc(String groupCode);

    List<WordEntity> findByGroupCodeAndSecretWordTrue(String groupCode);

    @Query("SELECT w FROM WordEntity w WHERE w.groupCode IS NULL OR w.groupCode = '' ORDER BY w.word ASC")
    List<WordEntity> findUngroupedOrderByWordAsc();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "TRUNCATE TABLE words RESTART IDENTITY", nativeQuery = true)
    void truncateAll();
}
