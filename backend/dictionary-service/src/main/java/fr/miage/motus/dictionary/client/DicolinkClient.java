package fr.miage.motus.dictionary.client;

import fr.miage.motus.dictionary.config.DictionaryProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Optional;

/**
 * API Dicolink (optionnelle) — nécessite une clé : https://www.dicolink.com/api/inscription
 * Sans clé, le service s'appuie uniquement sur la liste importée en base.
 */
@Component
public class DicolinkClient {

    private static final Logger log = LoggerFactory.getLogger(DicolinkClient.class);
    private static final String BASE = "https://api.dicolink.com/v1";

    private final RestClient restClient;
    private final DictionaryProperties properties;

    public DicolinkClient(DictionaryProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().baseUrl(BASE).build();
    }

    public boolean isEnabled() {
        return properties.dicolinkApiKey() != null && !properties.dicolinkApiKey().isBlank();
    }

    public boolean existsInDictionary(String word) {
        if (!isEnabled()) {
            return false;
        }
        try {
            restClient.get()
                    .uri("/mot/{mot}?api_key={key}", word.toLowerCase(), properties.dicolinkApiKey())
                    .retrieve()
                    .body(Map.class);
            return true;
        } catch (RestClientException e) {
            return false;
        }
    }

    public Optional<String> randomWord(int length) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = restClient.get()
                    .uri("/mots/motauhasard?minlong={min}&maxlong={max}&api_key={key}",
                            length, length, properties.dicolinkApiKey())
                    .retrieve()
                    .body(Map.class);
            if (body != null && body.get("mot") instanceof String mot) {
                return Optional.of(mot);
            }
        } catch (RestClientException e) {
            log.warn("Dicolink indisponible : {}", e.getMessage());
        }
        return Optional.empty();
    }
}
