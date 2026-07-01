# Rapport de projet — Motus Microservices

**Master 2 MIAGE SITN — Apprentissage 2025-2026**  
**Module :** Applications Web orientées Services — M. Menceur  
**Remise (PDF + lien GitHub) :** 4 juillet 2026 au plus tard — mouloud.menceur@gmail.com  
**Soutenance :** 7 juillet 2026, 8h30–13h30 (20 min : 10 min présentation + 10 min questions individuelles)  
**Dépôt GitHub :** [github.com/Aaronath/motus-microservice-miage](https://github.com/Aaronath/motus-microservice-miage)

---

## 1. Binôme

| Étudiant 1 | Étudiant 2 |
|------------|------------|
| **Halioua Nathan** | **Trabelsi Yacob** |

### Répartition du travail

| Domaine | Nathan Halioua | Yacob Trabelsi |
|---------|:--------------:|:--------------:|
| Conception de l'architecture microservices | ✓ | ✓ |
| Services Spring Boot (backend) | ✓ | |
| Interface React (frontend) | | ✓ |
| Docker Compose et déploiement Minikube | ✓ | |
| Recette fonctionnelle et conformité au sujet | | ✓ |
| Rédaction du rapport et de la documentation | ✓ | ✓ |

---

## 2. Compilation et exécution

Voir **`README.md`** (racine) et **`docs/readme.txt`** (résumé rapide).

**Résumé :**

**Docker Compose :**

```bash
./scripts/build-all.sh
docker compose up --build -d
# Frontend : http://localhost:3000
# API Gateway : http://localhost:8080
```

**Minikube** (exigence énoncé §2.2.3) :

```bash
brew install minikube   # si nécessaire
./scripts/deploy-minikube.sh
# Frontend : minikube service frontend -n motus --url
# API Gateway : minikube service api-gateway -n motus --url
```

Documentation détaillée : `docs/MINIKUBE.md`.

Compte administrateur : code `MIAGE-ADMIN-2026` à l'inscription.

---

## 3. Documentation technique

### 3.1 Schéma d'architecture

Architecture en **5 composants backend** + **1 client React** :

- **api-gateway** : point d'entrée, routage, CORS
- **player-service** : gestion des joueurs et JWT
- **dictionary-service** : dictionnaire français, validation des mots
- **game-service** : parties, propositions, algorithme Motus
- **stats-service** : historique, scores, classement

Chaque service métier possède sa **base PostgreSQL** (principe *database per service*).

Schéma détaillé : `docs/ARCHITECTURE.md`.

### 3.2 Choix techniques

| Aspect | Choix | Justification |
|--------|-------|---------------|
| Framework | Spring Boot 3.2 | Stack cours, écosystème mature |
| Persistance | Spring Data JPA | Exigence énoncé |
| SGBD | PostgreSQL 16 | Relationnel, Docker/K8s |
| API | REST JSON | Interopérabilité, simplicité démo |
| Auth | JWT (jjwt) | Stateless, adapté microservices |
| Gateway | Spring Cloud Gateway | Routage centralisé |
| Client | React + Vite | Démo visuelle convaincante |
| Conteneurs | Docker Compose | Déploiement reproductible |
| Orchestration | Minikube / Kubernetes | Namespace `motus`, manifests `k8s/`, script `deploy-minikube.sh` |

### 3.3 Diagramme de classes métier

Entités principales :

- **Joueur** (id, pseudo, email, rôle, dateInscription)
- **Partie** (joueur, motMystère, essaisMax, statut, dates)
- **Proposition** (mot, numéroEssai, feedback lettres)
- **RésultatPartie** (gagnée, nombreEssais, score)
- **MotDictionnaire** (mot, longueur)

Relations : un Joueur joue plusieurs Parties ; une Partie contient plusieurs Propositions ; une Partie produit un RésultatPartie.

Diagramme Mermaid : `docs/CLASS_DIAGRAM.md`.

### 3.4 Règles du jeu implémentées

1. Première lettre toujours affichée (`FIRST`)
2. Proposition refusée si longueur incorrecte, mauvaise 1ère lettre, ou mot absent du dictionnaire
3. Scoring : bien placées puis mal placées (gestion des doublons)
4. Victoire si mot exact ; défaite si essais épuisés
5. Longueur des mots : 5 à 9 lettres (lexique Lexique383)

### 3.5 APIs REST

Documentation complète : `docs/API.md`.  
Conformité au cahier des charges : `docs/CONFORMITE_SUJET.md`.

### 3.6 Déploiement Minikube

Conformément à l'énoncé (§2.2.3), l'application est déployable sur **Minikube** en plus de Docker Compose :

- **4 déploiements PostgreSQL** et **10 pods** (4 BDD + 5 services + frontend)
- **ConfigMap** : JWT, URLs inter-services, paramètres dictionnaire
- **NodePort** : frontend (30000), api-gateway (30080)
- Script automatisé : `./scripts/deploy-minikube.sh` — voir `docs/MINIKUBE.md`

---

## 4. Bilan du projet

### Ce que nous avons apprécié

Nous avons particulièrement apprécié de **mettre en pratique les notions du cours** — découpage en microservices, persistance JPA, API Gateway — sur un cas concret et ludique. L'implémentation de l'**algorithme Motus** avec un retour visuel immédiat (grille colorée, animations) a donné une dimension tangible au projet. Enfin, disposer de **deux modes de déploiement** (Docker Compose pour le développement, Minikube pour l'orchestration Kubernetes) nous a permis une démonstration fiable en soutenance.

### Ce que nous avons appris

Ce projet nous a permis d'approfondir le **découpage d'un domaine en bounded contexts** (joueurs, parties, dictionnaire, statistiques), chacun avec sa propre base de données. Nous avons mis en œuvre la **communication inter-services** via `RestClient`, une **authentification JWT stateless** et le **déploiement sur Minikube** (manifests Kubernetes, ConfigMap, NodePort, images locales). Nous avons consolidé nos compétences en **Spring Data JPA**, **Docker Compose** et **orchestration Kubernetes**.

### Ce que nous avons moins aimé

La **multiplication des bases PostgreSQL** et des fichiers de configuration associés a rendu le débogage local parfois fastidieux. Le **premier démarrage de Minikube** a posé des contraintes de mémoire (allocation Docker Desktop) et un temps de build important. Enfin, la **gestion des erreurs distribuées** entre cinq services reste perfectible.

### Points plus difficiles

Les principales difficultés ont concerné l'**orchestration de quatre bases PostgreSQL** et la cohérence des variables d'environnement entre Docker Compose et Kubernetes. L'**import du lexique** (~47 000 mots Lexique383) au démarrage du `dictionary-service` a nécessité compression et indexation. Le **déploiement Minikube** (tag d'images locales, ConfigMap, readiness des pods) a demandé un travail de configuration distinct de Docker Compose.

### Réussites

Nous sommes satisfaits d'avoir livré une **application jouable de bout en bout** et un **déploiement Minikube complet** (4 PostgreSQL, 5 microservices, frontend) conforme à l'énoncé. Le **panneau d'administration** dépasse le minimum exigé. Le dictionnaire s'appuie sur **Lexique383**, des **tests unitaires** couvrent le scoring, et une **documentation structurée** (architecture, API, Minikube, conformité) accompagne le code.

### Pistes d'amélioration

À terme, un **service discovery** (Eureka ou Consul) éviterait les URLs codées en dur, et un **bus de messages** (Kafka) pourrait découpler l'enregistrement des statistiques. Un **pipeline CI/CD** avec tests d'intégration automatisés renforcerait la qualité. Des **probes de santé** (liveness/readiness) sur tous les microservices amélioreraient la résilience sous Kubernetes.

---

*Rapport PDF (5 pages max) — remise au 4 juillet 2026 — mouloud.menceur@gmail.com (+ lien GitHub)*
