#!/usr/bin/env bash
# Déploiement Minikube — voir docs/MINIKUBE.md
set -euo pipefail
cd "$(dirname "$0")/.."

eval "$(minikube docker-env)"
docker compose build
minikube image load $(docker compose images -q dictionary-service) 2>/dev/null || true

kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/postgres.yaml
kubectl apply -f k8s/services.yaml

echo "Déploiement lancé. Accès gateway : minikube service api-gateway -n motus --url"
