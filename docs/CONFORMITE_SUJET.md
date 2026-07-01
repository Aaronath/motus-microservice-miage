# Conformité au cahier des charges Motus

## Gérer les joueurs

| Exigence | Statut | Détail |
|----------|--------|--------|
| S'enregistrer pour jouer | OK | `POST /api/players/register` + écran inscription |
| Retrouver l'historique des **résultats** | OK | Onglet **Historique** → `GET /api/stats/players/{id}` (parties gagnées/perdues, essais, score, date) |

## Gérer des parties de Motus

| Exigence | Statut | Détail |
|----------|--------|--------|
| Initialiser une partie | OK | `POST /api/games` (longueur 5–9 au choix) |
| Mot mystère aléatoire (dictionnaire) | OK | `dictionary-service` + validation lexique |
| Propositions jusqu'au max d'essais | OK | 6 essais par défaut |
| Vérifier existence dans le dictionnaire | OK | Avant chaque essai |
| Calcul bien placé / mal placé | OK | `MotusScorer` |
| Résultat au joueur | OK | Grille + statut partie |

## Suivre le score et historiser

| Exigence | Statut | Détail |
|----------|--------|--------|
| Enregistrer résultat (gagné/perdu, essais…) | OK | `POST /api/stats/results` en fin de partie |
| Statistiques globales | OK | Bannière sur onglet **Classement** → `GET /api/stats/global` |
| Classement des joueurs | OK | `GET /api/stats/leaderboard` |
| Résultats des différentes parties (par joueur) | OK | Historique stats par joueur |

## Administrer le jeu

| Exigence | Statut | Détail |
|----------|--------|--------|
| Lister / rechercher les parties (a minima) | OK | Admin → **Parties** : filtres ID joueur + statut → `GET /api/games/admin/search` |

## Technique : JPA et bases de données

Oui. Chaque microservice métier utilise **Spring Data JPA** et **PostgreSQL** (une base par service) :

| Service | Base | Entités JPA (ex.) |
|---------|------|-------------------|
| player-service | `motus_players` | `Player` |
| game-service | `motus_games` | `Game`, `Guess` |
| dictionary-service | `motus_dictionary` | `WordEntity`, `WordGroupEntity` |
| stats-service | `motus_stats` | `GameResult` |

`ddl-auto: update` en dev ; schéma créé/mis à jour au démarrage.
