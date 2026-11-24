#!/usr/bin/env bash
# Genera los JSON de ECS (task definitions, servicios, dashboard) a partir de las plantillas
# de ecs/*.tpl.json, reemplazando las variables ${...}. No necesita envsubst ni jq.
#
# Uso (desde mysql-aws-3/):
#   export AWS_REGION LAB_ROLE_ARN DOCKERHUB_USER IMAGE_TAG CLUSTER NAMESPACE \
#          SUBNET1 SUBNET2 SG_ID
#   ./ecs/render.sh            # deja los archivos listos en ecs/out/
set -euo pipefail

cd "$(dirname "$0")"
mkdir -p out

for var in AWS_REGION LAB_ROLE_ARN DOCKERHUB_USER IMAGE_TAG CLUSTER NAMESPACE SUBNET1 SUBNET2 SG_ID; do
  if [ -z "${!var:-}" ]; then
    echo "ERROR: falta la variable $var" >&2
    exit 1
  fi
done

for tpl in *.tpl.json; do
  out="out/${tpl%.tpl.json}.json"
  sed \
    -e "s|\${AWS_REGION}|${AWS_REGION}|g" \
    -e "s|\${LAB_ROLE_ARN}|${LAB_ROLE_ARN}|g" \
    -e "s|\${DOCKERHUB_USER}|${DOCKERHUB_USER}|g" \
    -e "s|\${IMAGE_TAG}|${IMAGE_TAG}|g" \
    -e "s|\${CLUSTER}|${CLUSTER}|g" \
    -e "s|\${NAMESPACE}|${NAMESPACE}|g" \
    -e "s|\${SUBNET1}|${SUBNET1}|g" \
    -e "s|\${SUBNET2}|${SUBNET2}|g" \
    -e "s|\${SG_ID}|${SG_ID}|g" \
    "$tpl" > "$out"
  echo "generado ecs/$out"
done
