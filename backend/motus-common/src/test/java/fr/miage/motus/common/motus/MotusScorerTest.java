package fr.miage.motus.common.motus;

import fr.miage.motus.common.dto.LetterState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MotusScorerTest {

    @Test
    void bienPlaceesEtMalPlacees() {
        var feedback = MotusScorer.score("MAISON", "MAISON");
        assertEquals(LetterState.FIRST, feedback.get(0).state());
        assertEquals(LetterState.WELL_PLACED, feedback.get(1).state());
        assertEquals(LetterState.WELL_PLACED, feedback.get(5).state());
    }

    @Test
    void victoire() {
        assertTrue(MotusScorer.isWin("MAISON", "maison"));
    }
}
