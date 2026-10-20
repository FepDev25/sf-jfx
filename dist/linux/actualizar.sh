#!/usr/bin/env bash
# Plantilla de actualizacion remota asistida para Linux.
# Uso: actualizar.sh <paquete.zip> [directorio-app]
# La aplicacion debe estar cerrada. El script respalda, reemplaza jars y aplica migraciones.
set -euo pipefail

PAQUETE="${1:?Indique el paquete .zip descargado}"
APP_DIR="${2:-/opt/sf-jfx}"
CONFIG="${SFJFX_CONFIG:-$HOME/.sfjfx/sfjfx.properties}"
RESPALDOS="${RESPALDOS_DIR:-$HOME/.sfjfx/respaldos}"
MARCA="$(date +%Y%m%d-%H%M%S)"
BACKUP="$RESPALDOS/update-$MARCA"
PG_DUMP="${PG_DUMP:-pg_dump}"
PG_RESTORE="${PG_RESTORE:-pg_restore}"

if [[ ! -f "$PAQUETE" ]]; then
  echo "No existe el paquete: $PAQUETE" >&2
  exit 1
fi
if [[ ! -f "$CONFIG" ]]; then
  echo "No existe la configuracion: $CONFIG" >&2
  exit 1
fi
if [[ ! -d "$APP_DIR/lib/app" ]]; then
  echo "No se encontro la aplicacion en: $APP_DIR/lib/app" >&2
  exit 1
fi
if ! command -v "$PG_DUMP" >/dev/null 2>&1 || ! command -v "$PG_RESTORE" >/dev/null 2>&1; then
  echo "pg_dump/pg_restore no estan en PATH" >&2
  exit 1
fi

DB_URL="$(grep -E '^db\.url=' "$CONFIG" | tail -1 | cut -d= -f2-)"
DB_USER="$(grep -E '^db\.usuario=' "$CONFIG" | tail -1 | cut -d= -f2-)"
DB_PASS="$(grep -E '^db\.password=' "$CONFIG" | tail -1 | cut -d= -f2-)"
if [[ ! "$DB_URL" =~ ^jdbc:postgresql://([^:/]+)(:([0-9]+))?/([^?]+) ]]; then
  echo "db.url no tiene el formato esperado: $DB_URL" >&2
  exit 1
fi
DB_HOST="${BASH_REMATCH[1]}"
DB_PORT="${BASH_REMATCH[3]:-5432}"
DB_NAME="${BASH_REMATCH[4]}"

mkdir -p "$BACKUP"
DUMP="$BACKUP/base-$MARCA.dump"
echo "==> Respaldo previo: $DUMP"
PGPASSWORD="$DB_PASS" "$PG_DUMP" --format=custom --no-owner --no-privileges \
  --host="$DB_HOST" --port="$DB_PORT" --username="$DB_USER" --dbname="$DB_NAME" \
  --file="$DUMP"
"$PG_RESTORE" --list "$DUMP" >/dev/null
echo "==> Respaldo verificado"

STAGING="$(mktemp -d)"
trap 'rm -rf "$STAGING"' EXIT
unzip -q "$PAQUETE" -d "$STAGING"

echo "==> Resguardando jars actuales"
cp -a "$APP_DIR/lib/app/." "$BACKUP/app/"

echo "==> Instalando nuevos jars"
find "$STAGING" -maxdepth 1 -name '*.jar' -exec cp -f {} "$APP_DIR/lib/app/" \;

echo "==> Aplicando migraciones"
if ! "$APP_DIR/runtime/bin/java" -cp "$APP_DIR/lib/app/*" ec.sfjfx.core.arranque.Arranque; then
  echo "==> Migracion fallida. Restaurando jars anteriores." >&2
  cp -a "$BACKUP/app/." "$APP_DIR/lib/app/"
  echo "Restaure la base desde $DUMP antes de reiniciar la aplicacion." >&2
  exit 1
fi

echo "==> Actualizacion completada"
echo "Reinicie la aplicacion: $APP_DIR/bin/sf-jfx"
echo "Respaldo previo: $DUMP"
