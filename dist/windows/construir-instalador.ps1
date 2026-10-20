# Plantilla de construccion de instalador para Windows.
# Requiere JDK 21 con jpackage/jlink, Maven, PostgreSQL portatil y WiX Toolset para MSI.
$ErrorActionPreference = "Stop"

$Root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$Version = if ($env:VERSION) { $env:VERSION } else { "0.1.0" }
$PgHome = if ($env:PG_HOME) { $env:PG_HOME } else { "C:\Program Files\PostgreSQL\17" }
$Dest = if ($env:DEST) { $env:DEST } else { Join-Path $Root "build\dist" }
$Stage = Join-Path $Dest "stage"

if (-not (Test-Path $PgHome)) {
    throw "No existe PG_HOME: $PgHome"
}

Remove-Item -Recurse -Force $Dest -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path (Join-Path $Stage "input") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $Stage "postgres") | Out-Null

Write-Host "==> Compilando y probando"
& mvn -q -f (Join-Path $Root "pom.xml") clean package
if ($LASTEXITCODE -ne 0) { throw "Fallo Maven" }

Write-Host "==> Copiando artefactos de runtime (sin el modulo dev)"
Copy-Item (Join-Path $Root "core\target\core-*.jar") (Join-Path $Stage "input")
Copy-Item (Join-Path $Root "ui\target\ui-*.jar") (Join-Path $Stage "input")
& mvn -q -f (Join-Path $Root "ui\pom.xml") dependency:copy-dependencies `
    -DincludeScope=runtime "-DoutputDirectory=$Stage\input"
if ($LASTEXITCODE -ne 0) { throw "Fallo la copia de dependencias" }

Write-Host "==> Incluyendo PostgreSQL desde $PgHome"
Copy-Item -Recurse -Force (Join-Path $PgHome "*") (Join-Path $Stage "postgres")

$UiJar = (Get-ChildItem (Join-Path $Stage "input") -Filter "ui-*.jar" | Select-Object -First 1).Name

Write-Host "==> Generando app-image"
& jpackage `
    --type app-image `
    --name sf-jfx `
    --app-version $Version `
    --input (Join-Path $Stage "input") `
    --main-jar $UiJar `
    --main-class ec.sfjfx.ui.App `
    --dest $Dest `
    --java-options "-Dfile.encoding=UTF-8" `
    --java-options "-Dsfjfx.version=$Version"
if ($LASTEXITCODE -ne 0) { throw "Fallo jpackage" }

Write-Host "==> App image: $Dest\sf-jfx"
Write-Host "PostgreSQL portatil incluido en: $Dest\sf-jfx\lib\app\postgres"

# Opcional: MSI/EXE si WiX esta instalado.
# & jpackage --type msi --name sf-jfx --app-version $Version `
#   --input (Join-Path $Stage "input") --main-jar $UiJar --main-class ec.sfjfx.ui.App `
#   --dest $Dest --win-menu --win-shortcut
