# Motus — Microservices MIAGE SITN 2025-2026

Application de gestion du jeu **Motus** en architecture microservices (Spring Boot 3, JPA, PostgreSQL, API Gateway, React).

## Binôme

**Halioua Nathan** / **Trabelsi Yacob** — Master 2 MIAGE SITN 2025-2026

## Prérequis

- Java 17+
- Maven 3.9+
- Docker & Docker Compose
- Node.js 20+ (frontend en local)
- Minikube + kubectl (déploiement K8s, optionnel)

## Démarrage rapide (Docker)

```bash
# Compiler le backend
./scripts/build-all.sh
# ou : cd backend && mvn clean package -DskipTests

# Lancer toute la stack (BDD + microservices + gateway + frontend)
docker compose up --build -d

# Interface web : http://localhost:3000
# API Gateway  : http://localhost:8080
```

**Comptes démo**

| Rôle | Inscription | Code admin |
|------|-------------|------------|
| Joueur | Créer un compte | — |
| Admin | Créer un compte | `MIAGE-ADMIN-2026` |

## Démarrage en développement (local)

1. Démarrer les bases : `docker compose up db-players db-dictionary db-games db-stats -d`
2. Lancer chaque service depuis `backend/` (IDE ou terminal) :
   - `dictionary-service` → 8083
   - `player-service` → 8081
   - `stats-service` → 8084
   - `game-service` → 8082
   - `api-gateway` → 8080
3. Frontend : `cd frontend && npm install && npm run dev` → http://localhost:3000

```bash
cd backend && mvn clean package -DskipTests
```

## Dictionnaire

Le lexique jouable (~42 000 mots, **5 à 9 lettres**) provient de **[Lexique383](http://www.lexique.org/)** (licence [CC-BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/)) : noms, adjectifs, verbes et adverbes courants, sans noms propres, normalisés A–Z sans accents.

- **`data/lexique-motus-5-9.csv`** : généré par `python3 scripts/build-lexique-motus.py` (télécharge `Lexique383.tsv` une fois dans `data/`)
- **`word-groups.json`** : 27 familles thématiques (`secretWords` + propositions par lettre), sans dupliquer tout le lexique
- Import au démarrage : `DICTIONARY_SOURCE=both` (lexique + groupes), fichier embarqué `classpath:data/lexique-motus-5-9.csv.gz`

```bash
python3 scripts/build-lexique-motus.py
python3 scripts/build-word-groups.py   # nécessite le CSV ci-dessus
docker compose up --build -d
```

- API : `GET /api/dictionary/stats`, `GET /api/dictionary/words/{mot}/exists`
- Rechargement admin : `POST /api/dictionary/admin/reload`

## Tests

```bash
# Tests unitaires (scoring Motus, etc.)
mvn test

# Tests d'intégration API (stack démarrée sur :8080)
# Retirer @Disabled dans MotusApiIntegrationTest puis :
cd backend && mvn test -pl integration-tests
```

## Architecture

| Service | Port | Rôle |
|---------|------|------|
| api-gateway | 8080 | Routage, CORS |
| player-service | 8081 | Joueurs, JWT |
| game-service | 8082 | Parties, propositions, Motus |
| dictionary-service | 8083 | Dictionnaire, tirage mot |
| stats-service | 8084 | Scores, classement |
| frontend | 3000 | Client React |

Documentation détaillée : [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md), [`docs/API.md`](docs/API.md), [`docs/RAPPORT_PROJET.md`](docs/RAPPORT_PROJET.md).

## Minikube

Voir [`docs/MINIKUBE.md`](docs/MINIKUBE.md) et `./scripts/deploy-minikube.sh`.

## Structure du projet

```
Microservices_MIAGE/
├── backend/                    # Maven multi-modules Spring Boot
│   ├── pom.xml
│   ├── motus-common/
│   ├── player-service/
│   ├── game-service/
│   ├── dictionary-service/
│   ├── stats-service/
│   ├── api-gateway/
│   ├── integration-tests/
│   └── docker/                 # Dockerfile des services Java
├── frontend/                   # React + Vite
├── k8s/
├── docs/
├── scripts/
└── docker-compose.yml
```

Chaque service suit le squelette **start.spring.io** : `controller`, `service`, `repository`, `entity`, `dto`, `config` — voir [`backend/README.md`](backend/README.md).

## Rendu

- Rapport PDF (5 pages max) : exporter `docs/RAPPORT_PROJET.md`
- Dépôt GitHub : [github.com/Aaronath/motus-microservice-miage](https://github.com/Aaronath/motus-microservice-miage)
- Remise : **4 juillet 2026** — mouloud.menceur@gmail.com
