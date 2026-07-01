package fr.miage.motus.it;

import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

/**
 * Tests d'intégration « client » : nécessitent la stack démarrée (docker-compose up).
 * mvn test -pl integration-tests -Dtest=MotusApiIntegrationTest
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled("Activer lorsque l'API Gateway tourne sur localhost:8080")
class MotusApiIntegrationTest {

    private static final String BASE = "http://localhost:8080";
    private static RestClient client;
    private static String token;
    private static Long gameId;

    @BeforeAll
    static void setup() {
        client = RestClient.builder().baseUrl(BASE).build();
    }

    @Test
    @Order(1)
    void registerAndLogin() {
        String pseudo = "test_" + UUID.randomUUID().toString().substring(0, 8);
        var register = Map.of("pseudo", pseudo, "email", pseudo + "@test.fr", "password", "secret12");
        ResponseEntity<Map> reg = client.post()
                .uri("/api/players/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(register)
                .retrieve()
                .toEntity(Map.class);
        Assertions.assertEquals(HttpStatus.CREATED, reg.getStatusCode());
        token = (String) reg.getBody().get("token");
        Assertions.assertNotNull(token);
    }

    @Test
    @Order(2)
    void createGameAndGuess() {
        Assertions.assertNotNull(token);
        var headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<Map> game = client.post()
                .uri("/api/games")
                .headers(h -> h.addAll(headers))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("wordLength", 7, "maxAttempts", 6))
                .retrieve()
                .toEntity(Map.class);
        Assertions.assertEquals(HttpStatus.CREATED, game.getStatusCode());
        gameId = ((Number) game.getBody().get("id")).longValue();
        String first = (String) game.getBody().get("firstLetter");
        Assertions.assertNotNull(first);

        String attempt = first + "ATION".substring(0, 6);
        if (attempt.length() > 7) attempt = attempt.substring(0, 7);

        client.post()
                .uri("/api/games/{id}/guesses", gameId)
                .headers(h -> h.addAll(headers))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("word", attempt))
                .retrieve()
                .toEntity(Map.class);
    }

    @Test
    @Order(3)
    void leaderboard() {
        var list = client.get()
                .uri("/api/stats/leaderboard")
                .retrieve()
                .toEntity(java.util.List.class);
        Assertions.assertNotNull(list.getBody());
    }
}
