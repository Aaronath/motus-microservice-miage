package fr.miage.motus.game.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.miage.motus.common.dto.LetterFeedback;
import fr.miage.motus.common.motus.MotusScorer;
import fr.miage.motus.game.client.DictionaryClient;
import fr.miage.motus.game.client.StatsClient;
import fr.miage.motus.game.entity.Game;
import fr.miage.motus.game.entity.GameStatus;
import fr.miage.motus.game.entity.Guess;
import fr.miage.motus.game.repository.GameRepository;
import fr.miage.motus.game.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GamePlayService {

    private final GameRepository gameRepository;
    private final DictionaryClient dictionaryClient;
    private final StatsClient statsClient;
    private final ObjectMapper objectMapper;

    public GamePlayService(GameRepository gameRepository, DictionaryClient dictionaryClient,
                           StatsClient statsClient, ObjectMapper objectMapper) {
        this.gameRepository = gameRepository;
        this.dictionaryClient = dictionaryClient;
        this.statsClient = statsClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public GameResponse createGame(Long playerId, String pseudo, CreateGameRequest req) {
        int length = req.wordLength() != null ? req.wordLength() : 7;
        int maxAttempts = req.maxAttempts() != null ? req.maxAttempts() : 6;
        if (length < 5 || length > 9) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La longueur du mot doit être entre 5 et 9");
        }
        String secret = dictionaryClient.randomWord(length);
        Game game = new Game(playerId, pseudo, secret, maxAttempts);
        game = gameRepository.save(game);
        return toResponse(game, false);
    }

    @Transactional(readOnly = true)
    public GameResponse getGame(Long id, Long requesterId, boolean admin) {
        Game game = findGame(id);
        if (!admin && !game.getPlayerId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        boolean reveal = admin || game.getStatus() != GameStatus.IN_PROGRESS;
        return toResponse(game, reveal);
    }

    @Transactional
    public GuessResponse submitGuess(Long gameId, Long playerId, GuessRequest req) {
        Game game = findGame(gameId);
        if (!game.getPlayerId().equals(playerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce n'est pas votre partie");
        }
        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Partie terminée");
        }

        String word = req.word().trim().toUpperCase();
        if (word.length() != game.getWordLength()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le mot doit contenir " + game.getWordLength() + " lettres");
        }
        if (word.charAt(0) != game.getFirstLetter()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La première lettre doit être '" + game.getFirstLetter() + "'");
        }
        if (!dictionaryClient.wordExists(word)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mot absent du dictionnaire");
        }

        List<LetterFeedback> feedback = MotusScorer.score(game.getSecretWord(), word);
        String feedbackJson = serialize(feedback);
        int attempt = game.getAttemptsUsed() + 1;
        game.addGuess(new Guess(word, attempt, feedbackJson));

        boolean won = MotusScorer.isWin(game.getSecretWord(), word);
        if (won) {
            game.markWon();
        } else if (game.getAttemptsUsed() >= game.getMaxAttempts()) {
            game.markLost();
        }
        gameRepository.save(game);

        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            publishStats(game);
        }

        return new GuessResponse(
                attempt,
                true,
                feedback,
                game.getStatus().name(),
                game.remainingAttempts(),
                won ? game.getSecretWord() : null
        );
    }

    @Transactional(readOnly = true)
    public List<GameSummaryDto> playerHistory(Long playerId) {
        return gameRepository.findByPlayerIdOrderByStartedAtDesc(playerId).stream()
                .map(g -> new GameSummaryDto(g.getId(), g.getStatus().name(), g.getAttemptsUsed(),
                        g.getMaxAttempts(), g.getWordLength(), g.getStartedAt(), g.getFinishedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminGameDto> adminSearch(String q, GameStatus status, LocalDateTime from, LocalDateTime to) {
        String trimmed = (q != null && !q.isBlank()) ? q.trim() : null;
        Long gameId = null;
        if (trimmed != null && trimmed.matches("\\d+")) {
            gameId = Long.parseLong(trimmed);
        }
        List<Game> games = trimmed == null
                ? gameRepository.findAllForAdmin(status, from, to)
                : gameRepository.searchByText(trimmed, gameId, status, from, to);
        return games.stream()
                .map(g -> new AdminGameDto(g.getId(), g.getPlayerId(), g.getPlayerPseudo(),
                        g.getSecretWord(), g.getStatus().name(), g.getAttemptsUsed(),
                        g.getMaxAttempts(), g.getStartedAt(), g.getFinishedAt()))
                .toList();
    }

    @Transactional
    public GameResponse abandonGame(Long gameId, Long playerId) {
        Game game = findGame(gameId);
        if (!game.getPlayerId().equals(playerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce n'est pas votre partie");
        }
        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Partie déjà terminée");
        }
        game.markAbandoned();
        gameRepository.save(game);
        publishStats(game);
        return toResponse(game, true);
    }

    @Transactional
    public void deleteGame(Long id) {
        if (!gameRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Partie introuvable");
        }
        gameRepository.deleteById(id);
    }

    private void publishStats(Game game) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("gameId", game.getId());
        payload.put("playerId", game.getPlayerId());
        payload.put("playerPseudo", game.getPlayerPseudo());
        payload.put("won", game.getStatus() == GameStatus.WON);
        payload.put("status", game.getStatus().name());
        payload.put("attempts", game.getAttemptsUsed());
        payload.put("maxAttempts", game.getMaxAttempts());
        payload.put("wordLength", game.getWordLength());
        payload.put("firstLetter", String.valueOf(game.getFirstLetter()));
        payload.put("secretWord", game.getSecretWord());
        payload.put("finishedAt", game.getFinishedAt().toString());
        try {
            statsClient.recordResult(payload);
        } catch (Exception ignored) {
            // stats best-effort
        }
    }

    private Game findGame(Long id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Partie introuvable"));
    }

    private GameResponse toResponse(Game game, boolean revealSecret) {
        List<GuessDto> guesses = game.getGuesses().stream()
                .map(g -> new GuessDto(g.getAttemptNumber(), g.getProposedWord(), deserialize(g.getFeedbackJson())))
                .toList();
        return new GameResponse(
                game.getId(),
                game.getPlayerId(),
                game.getPlayerPseudo(),
                game.getWordLength(),
                String.valueOf(game.getFirstLetter()),
                game.getMaxAttempts(),
                game.getAttemptsUsed(),
                game.getStatus().name(),
                game.getStartedAt(),
                game.getFinishedAt(),
                revealSecret ? game.getSecretWord() : null,
                guesses
        );
    }

    private String serialize(List<LetterFeedback> feedback) {
        try {
            return objectMapper.writeValueAsString(feedback);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<LetterFeedback> deserialize(String json) {
        try {
            return List.of(objectMapper.readValue(json, LetterFeedback[].class));
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
