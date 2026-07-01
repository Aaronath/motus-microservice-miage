#!/usr/bin/env bash
# Déploiement Minikube complet — voir docs/MINIKUBE.md
set -euo pipefail
cd "$(dirname "$0")/.."

COMPOSE_PROJECT="${COMPOSE_PROJECT_NAME:-microservices_miage}"

if ! command -v minikube >/dev/null 2>&1; then
  echo "Erreur : minikube n'est pas installé." >&2
  exit 1
fi

if ! minikube status >/dev/null 2>&1; then
  echo "Démarrage de Minikube..."
  minikube start --cpus=4 --memory=8192
fi

echo "Construction des images dans le daemon Docker de Minikube..."
eval "$(minikube docker-env)"
docker compose build

echo "Tag des images motus/*:1.0..."
for svc in dictionary-service player-service game-service stats-service api-gateway frontend; do
  docker tag "${COMPOSE_PROJECT}-${svc}:latest" "motus/${svc}:1.0"
done

echo "Application des manifests Kubernetes..."
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/postgres.yaml
kubectl apply -f k8s/services.yaml
kubectl apply -f k8s/frontend.yaml

echo "Attente du démarrage des bases PostgreSQL..."
for db in postgres-players postgres-dictionary postgres-games postgres-stats; do
  kubectl rollout status "deployment/${db}" -n motus --timeout=180s
done

echo "Attente du démarrage des microservices..."
for svc in dictionary-service player-service stats-service game-service api-gateway frontend; do
  kubectl rollout status "deployment/${svc}" -n motus --timeout=300s
done

echo ""
echo "=== Déploiement Minikube terminé ==="
echo "Frontend  : $(minikube service frontend -n motus --url)"
echo "API Gateway : $(minikube service api-gateway -n motus --url)"
minikube service list -n motus
