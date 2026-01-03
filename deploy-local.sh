#!/bin/bash
set -e
docker-compose --env-file .env-example up -d postgres redis minio app 
docker build -t distributed-image-processing:latest .

kubectl apply -f k8s/infrastructure.yaml
kubectl apply -f k8s/worker.yaml