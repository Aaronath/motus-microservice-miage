# Déploiement Minikube

Conformément à l'énoncé du projet (conteneurisation Docker + déploiement Minikube).

## Prérequis

```bash
# macOS (Homebrew)
brew install minikube

minikube start --cpus=4 --memory=8192
kubectl version --client
docker compose version
```

## Déploiement en une commande

```bash
./scripts/deploy-minikube.sh
```

Le script :
1. Démarre Minikube si nécessaire
2. Construit les images dans le daemon Docker de Minikube
3. Les tague `motus/<service>:1.0`
4. Applique les manifests `k8s/` (4 PostgreSQL + 5 microservices + frontend)
5. Attend que tous les pods soient prêts

## Accès

```bash
minikube service frontend -n motus --url      # interface web (NodePort 30000)
minikube service api-gateway -n motus --url   # API REST (NodePort 30080)
```

Compte admin (inscription) : code `MIAGE-ADMIN-2026`.

## Déploiement manuel

```bash
eval $(minikube docker-env)
docker compose build

docker tag microservices_miage-dictionary-service:latest motus/dictionary-service:1.0
docker tag microservices_miage-player-service:latest motus/player-service:1.0
docker tag microservices_miage-game-service:latest motus/game-service:1.0
docker tag microservices_miage-stats-service:latest motus/stats-service:1.0
docker tag microservices_miage-api-gateway:latest motus/api-gateway:1.0
docker tag microservices_miage-frontend:latest motus/frontend:1.0

kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/postgres.yaml
kubectl apply -f k8s/services.yaml
kubectl apply -f k8s/frontend.yaml
```

## Architecture K8s

| Composant | Service K8s | Port NodePort |
|-----------|-------------|---------------|
| frontend | `frontend` | 30000 |
| api-gateway | `api-gateway` | 30080 |
| player-service | `player-service` | — (ClusterIP) |
| game-service | `game-service` | — |
| dictionary-service | `dictionary-service` | — |
| stats-service | `stats-service` | — |
| PostgreSQL ×4 | `postgres-*` | — |

## Dépannage

```bash
kubectl get pods -n motus
kubectl logs deployment/dictionary-service -n motus
kubectl rollout restart deployment/game-service -n motus
```
