#!/usr/bin/env bash
# Plantilla de construccion de instalador para Linux.
# Requiere JDK 21 con jpackage/jlink, Maven y una distribucion portatil de PostgreSQL.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
VERSION="${VERSION:-0.1.0}"
PG_HOME="${PG_HOME:?Defina PG_HOME con la distribucion portatil de PostgreSQL}"
DEST="${DEST:-$ROOT/build/dist}"
STAGE="$DEST/stage"

rm -rf "$DEST"
mkdir -p "$STAGE/input" "$STAGE/postgres"

echo "==> Compilando y probando"
mvn -q -f "$ROOT/pom.xml" clean package

echo "==> Copiando artefactos de runtime (sin el modulo dev)"
cp "$ROOT"/core/target/core-*.jar "$STAGE/input/"
cp "$ROOT"/ui/target/ui-*.jar "$STAGE/input/"
mvn -q -f "$ROOT/ui/pom.xml" dependency:copy-dependencies \
  -DincludeScope=runtime -DoutputDirectory="$STAGE/input"

echo "==> Incluyendo PostgreSQL desde $PG_HOME"
cp -a "$PG_HOME"/. "$STAGE/postgres/"

UI_JAR="$(basename "$(find "$STAGE/input" -maxdepth 1 -name 'ui-*.jar' | head -1)")"

echo "==> Generando app-image"
jpackage \
  --type app-image \
  --name sf-jfx \
  --app-version "$VERSION" \
  --input "$STAGE/input" \
  --main-jar "$UI_JAR" \
  --main-class ec.sfjfx.ui.App \
  --dest "$DEST" \
  --java-options "-Dfile.encoding=UTF-8" \
  --java-options "-Dsfjfx.version=$VERSION"

echo "==> App image: $DEST/sf-jfx"
echo "PostgreSQL portatil incluido en: $DEST/sf-jfx/lib/app/postgres"
echo "Registre el servicio PostgreSQL y ejecute el primer arranque segun dist/README.md."

# Opcional: paquete .deb si estan disponibles las herramientas.
# jpackage --type deb --name sf-jfx --app-version "$VERSION" \
#   --input "$STAGE/input" --main-jar "$UI_JAR" --main-class ec.sfjfx.ui.App \
#   --dest "$DEST" --linux-shortcut
