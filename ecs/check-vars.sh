#!/usr/bin/env bash
# Verifica herramientas, credenciales de AWS y variables de entorno de la guía ECS.
#
# Uso (desde mysql-aws-3/):
#   bash ecs/check-vars.sh [N]
# N = número del paso que estás por comenzar (por defecto 0). Cuanto mayor es N,
# más variables se exigen:
#   0 -> variables base          1 -> + IMAGE_TAG (tras el Paso 0)
#   3 -> + VPC_ID, SUBNET1, SUBNET2, SG_ID (tras el Paso 1)
#   6 -> + APP_IP (tras el Paso 5)     9 -> + TOPIC_ARN, NS_ID (antes de la limpieza)
#
# Las variables deben estar exportadas (export): este script corre en un proceso hijo.
set -u

STEP="${1:-0}"
FAIL=0

ok()   { printf '  [OK]    %s\n' "$1"; }
warn() { printf '  [AVISO] %s\n' "$1"; }
bad()  { printf '  [FALTA] %s\n' "$1"; FAIL=1; }

echo "== Herramientas"
for cmd in aws curl git; do
  if command -v "$cmd" >/dev/null 2>&1; then ok "$cmd"; else bad "$cmd no está instalado o no está en el PATH"; fi
done
if command -v docker >/dev/null 2>&1; then ok "docker"; else warn "docker no encontrado (solo necesario en el Paso 0.B / 0.C)"; fi

case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*)
    if [ "${MSYS_NO_PATHCONV:-}" = "1" ]; then ok "MSYS_NO_PATHCONV=1 (Git Bash no altera rutas como /ecs/bdget)"
    else bad "MSYS_NO_PATHCONV no está en 1: ejecuta  export MSYS_NO_PATHCONV=1"; fi ;;
esac

echo "== Credenciales de AWS"
if command -v aws >/dev/null 2>&1 && aws sts get-caller-identity --query Account --output text >/dev/null 2>&1; then
  ok "aws sts get-caller-identity responde (credenciales vigentes)"
else
  bad "AWS rechaza las credenciales: en AWS Academy expiran con la sesión; vuelve a copiarlas (guía 3.1.1)"
fi

echo "== Variables"
need() {  # need NOMBRE
  local name="$1" value="${!1:-}"
  if [ -z "$value" ]; then bad "$name está vacía o no exportada"
  elif [[ "$value" == *$'\r'* ]]; then bad "$name contiene un retorno de carro (\\r): en Git Bash define la función aws() del bloque de arranque y vuelve a ejecutar el paso"
  elif [[ "$value" == *"<"* || "$value" == *tu_* ]]; then bad "$name todavía tiene el texto de ejemplo: $value"
  elif [ "$value" = "None" ]; then bad "$name vale 'None': la consulta de AWS no devolvió nada"
  else ok "$name=$value"; fi
}

for v in AWS_REGION ACCOUNT_ID LAB_ROLE_ARN CLUSTER NAMESPACE DOCKERHUB_USER EMAIL; do need "$v"; done
[ "$STEP" -ge 1 ] && need IMAGE_TAG
[ "$STEP" -ge 3 ] && for v in VPC_ID SUBNET1 SUBNET2 SG_ID; do need "$v"; done
[ "$STEP" -ge 6 ] && need APP_IP
[ "$STEP" -ge 9 ] && for v in TOPIC_ARN NS_ID; do need "$v"; done

if [ -n "${ACCOUNT_ID:-}" ] && ! [[ "$ACCOUNT_ID" =~ ^[0-9]{12}$ ]]; then bad "ACCOUNT_ID debe tener 12 dígitos"; fi
if [ -n "${LAB_ROLE_ARN:-}" ] && [ -n "${ACCOUNT_ID:-}" ] && [[ "$LAB_ROLE_ARN" != arn:aws:iam::${ACCOUNT_ID}:role/* ]]; then
  bad "LAB_ROLE_ARN no corresponde a la cuenta $ACCOUNT_ID"
fi
if [ -n "${EMAIL:-}" ] && [[ "$EMAIL" != *@*.* ]]; then bad "EMAIL no parece un correo válido"; fi

echo
if [ "$FAIL" -eq 0 ]; then echo "Todo listo para continuar con el Paso $STEP."; else echo "Corrige los puntos [FALTA] antes de continuar."; fi
exit "$FAIL"
