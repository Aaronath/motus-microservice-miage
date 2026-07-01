package fr.miage.motus.dictionary.repository;

import fr.miage.motus.dictionary.entity.WordGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WordGroupRepository extends JpaRepository<WordGroupEntity, String> {
    List<WordGroupEntity> findByLength(int length);
}
