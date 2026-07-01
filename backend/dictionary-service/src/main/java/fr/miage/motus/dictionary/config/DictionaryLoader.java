package fr.miage.motus.dictionary.config;

import fr.miage.motus.dictionary.service.DictionaryImportService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(DictionaryProperties.class)
public class DictionaryLoader implements CommandLineRunner {

    private final DictionaryImportService importService;

    public DictionaryLoader(DictionaryImportService importService) {
        this.importService = importService;
    }

    @Override
    public void run(String... args) {
        if (!importService.needsImport()) {
            return;
        }
        try {
            importService.importDictionary();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Échec du chargement du dictionnaire", e);
        }
    }
}
