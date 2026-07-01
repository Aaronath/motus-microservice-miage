# Backend — Motus Microservices

Projet Maven multi-modules **Spring Boot 3.2** / **Java 17**, généré et organisé selon les conventions [start.spring.io](https://start.spring.io).

## Build

```bash
cd backend
mvn clean package -DskipTests
```

## Modules

| Module | Port | Description |
|--------|------|-------------|
| `motus-common` | — | JWT, scoring Motus, DTO partagés |
| `player-service` | 8081 | Joueurs, authentification |
| `game-service` | 8082 | Parties Motus |
| `dictionary-service` | 8083 | Dictionnaire et groupes de mots |
| `stats-service` | 8084 | Statistiques et classement |
| `api-gateway` | 8080 | Spring Cloud Gateway |
| `integration-tests` | — | Tests d'intégration API |

## Structure d'un service (ex. `player-service`)

```
player-service/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/fr/miage/motus/player/
    │   │   ├── PlayerServiceApplication.java
    │   │   ├── controller/      # @RestController
    │   │   ├── service/         # logique métier
    │   │   ├── repository/      # Spring Data JPA
    │   │   ├── entity/          # @Entity JPA
    │   │   ├── dto/             # requêtes / réponses API
    │   │   ├── config/          # @Configuration
    │   │   └── security/        # Spring Security (si besoin)
    │   └── resources/
    │       └── application.yml
    └── test/java/...
```

`dictionary-service` ajoute `client/` (appels HTTP externes) et `model/` (objets métier non JPA).

## Docker

Les images Java sont construites depuis ce dossier :

```bash
docker compose build player-service   # depuis la racine du dépôt
```

Voir `docker/Dockerfile.service`.
