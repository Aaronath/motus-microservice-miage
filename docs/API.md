# API REST — Motus

Base URL (via gateway) : `http://localhost:8080`

## Authentification

Header : `Authorization: Bearer <token>`

---

## player-service — `/api/players`

| Méthode | Route | Auth | Description |
|---------|-------|------|-------------|
| POST | `/register` | Non | Inscription `{ pseudo, email, password, adminCode? }` |
| POST | `/login` | Non | Connexion `{ pseudo, password }` → token JWT |
| GET | `/me` | Oui | Profil du joueur connecté |
| GET | `/{id}` | Non | Détail joueur |

## dictionary-service — `/api/dictionary`

| Méthode | Route | Description |
|---------|-------|-------------|
| GET | `/words/random?length=7` | Mot aléatoire (longueur 5–9) |
| GET | `/words/{word}/exists` | Validation dictionnaire (Lexique383 local) |
| GET | `/words/search?q=MA` | Recherche dans le lexique |
| GET | `/stats` | Effectifs par longueur (`source: lexique383`) |

## game-service — `/api/games`

| Méthode | Route | Auth | Description |
|---------|-------|------|-------------|
| POST | `/` | Oui | Nouvelle partie `{ wordLength?, maxAttempts? }` |
| GET | `/{id}` | Oui | État partie + grille |
| POST | `/{id}/guesses` | Oui | Proposition `{ word }` |
| GET | `/history/me` | Oui | Historique du joueur |
| GET | `/admin/search` | Admin | Filtres `q` (pseudo, mot secret, n° partie), `status`, `from`, `to` |

### Exemple réponse guess

```json
{
  "attemptNumber": 2,
  "valid": true,
  "letters": [
    { "letter": "M", "position": 0, "state": "FIRST" },
    { "letter": "A", "position": 1, "state": "WELL_PLACED" }
  ],
  "gameStatus": "IN_PROGRESS",
  "remainingAttempts": 4,
  "secretWord": null
}
```

**États des lettres** : `FIRST`, `WELL_PLACED`, `MISPLACED`, `ABSENT`

## stats-service — `/api/stats`

| Méthode | Route | Description |
|---------|-------|-------------|
| POST | `/results` | Enregistrement fin de partie (interne) |
| GET | `/players/{id}` | Statistiques joueur |
| GET | `/leaderboard` | Classement |
| GET | `/global` | Stats globales |
