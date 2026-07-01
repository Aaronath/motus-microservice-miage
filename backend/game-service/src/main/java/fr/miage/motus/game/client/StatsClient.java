package fr.miage.motus.game.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class StatsClient {

    private final RestClient restClient;

    public StatsClient(@Value("${motus.stats.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public void recordResult(Map<String, Object> payload) {
        restClient.post()
                .uri("/api/stats/results")
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }
}
