package fr.miage.motus.game.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class DictionaryClient {

    private final RestClient restClient;

    public DictionaryClient(@Value("${motus.dictionary.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public String randomWord(int length) {
        var response = restClient.get()
                .uri("/api/dictionary/words/random?length={length}", length)
                .retrieve()
                .body(Map.class);
        if (response == null || !response.containsKey("word")) {
            throw new IllegalStateException("Impossible de tirer un mot aléatoire");
        }
        return (String) response.get("word");
    }

    public boolean wordExists(String word) {
        var response = restClient.get()
                .uri("/api/dictionary/words/{word}/exists", word)
                .retrieve()
                .body(Map.class);
        return response != null && Boolean.TRUE.equals(response.get("exists"));
    }
}
