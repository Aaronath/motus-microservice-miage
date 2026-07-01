# Déploiement Minikube

## Prérequis

```bash
minikube start
kubectl version --client
```

## Étapes

1. **Construire les images dans le daemon Docker de Minikube**

```bash
eval $(minikube docker-env)
docker compose build
```

2. **Taguer les images** (exemple)

```bash
docker tag microservices_miage-dictionary-service:latest motus/dictionary-service:1.0
docker tag microservices_miage-player-service:latest motus/player-service:1.0
docker tag microservices_miage-game-service:latest motus/game-service:1.0
docker tag microservices_miage-stats-service:latest motus/stats-service:1.0
docker tag microservices_miage-api-gateway:latest motus/api-gateway:1.0
```

3. **Appliquer les manifests**

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/postgres.yaml
kubectl apply -f k8s/services.yaml
```

4. **Accéder à l'API**

```bash
minikube service api-gateway -n motus --url
```

## Notes

- Les déploiements PostgreSQL dans `k8s/` sont simplifiés (une instance players en exemple). Pour la production du projet, dupliquer les manifests pour chaque base ou utiliser un chart Helm.
- `imagePullPolicy: IfNotPresent` permet d'utiliser les images locales Minikube.
