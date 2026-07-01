package fr.miage.motus.common.motus;

import fr.miage.motus.common.dto.LetterFeedback;
import fr.miage.motus.common.dto.LetterState;

import java.util.ArrayList;
import java.util.List;

/**
 * Algorithme Motus : bien placées d'abord, puis mal placées (gestion des doublons).
 */
public final class MotusScorer {

    private MotusScorer() {}

    public static List<LetterFeedback> score(String secret, String guess) {
        String s = secret.toUpperCase();
        String g = guess.toUpperCase();
        int n = s.length();
        if (g.length() != n) {
            throw new IllegalArgumentException("La proposition doit avoir la même longueur que le mot mystère");
        }

        LetterState[] states = new LetterState[n];
        boolean[] secretUsed = new boolean[n];
        boolean[] guessUsed = new boolean[n];

        for (int i = 0; i < n; i++) {
            if (i == 0) {
                states[i] = LetterState.FIRST;
                if (g.charAt(i) == s.charAt(i)) {
                    secretUsed[i] = true;
                    guessUsed[i] = true;
                }
            } else if (g.charAt(i) == s.charAt(i)) {
                states[i] = LetterState.WELL_PLACED;
                secretUsed[i] = true;
                guessUsed[i] = true;
            }
        }

        for (int i = 0; i < n; i++) {
            if (guessUsed[i] || i == 0) {
                continue;
            }
            char c = g.charAt(i);
            int match = -1;
            for (int j = 0; j < n; j++) {
                if (!secretUsed[j] && s.charAt(j) == c) {
                    match = j;
                    break;
                }
            }
            if (match >= 0) {
                states[i] = LetterState.MISPLACED;
                secretUsed[match] = true;
                guessUsed[i] = true;
            } else {
                states[i] = LetterState.ABSENT;
            }
        }

        List<LetterFeedback> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            result.add(new LetterFeedback(g.charAt(i), i, states[i]));
        }
        return result;
    }

    public static boolean isWin(String secret, String guess) {
        return secret.equalsIgnoreCase(guess);
    }
}
