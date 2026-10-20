# Plantilla de actualizacion remota asistida para Windows.
# Uso: .\actualizar.ps1 -Paquete C:\ruta\sf-jfx.zip [-AppDir "C:\Program Files\sf-jfx"]
param(
    [Parameter(Mandatory = $true)] [string] $Paquete,
    [string] $AppDir = "C:\Program Files\sf-jfx"
)
$ErrorActionPreference = "Stop"

$Config = if ($env:SFJFX_CONFIG) { $env:SFJFX_CONFIG } else { Join-Path $HOME ".sfjfx\sfjfx.properties" }
$Respaldos = if ($env:RESPALDOS_DIR) { $env:RESPALDOS_DIR } else { Join-Path $HOME ".sfjfx\respaldos" }
$Marca = Get-Date -Format "yyyyMMdd-HHmmss"
$Backup = Join-Path $Respaldos "update-$Marca"
$AppLib = Join-Path $AppDir "lib\app"
$Java = Join-Path $AppDir "runtime\bin\java.exe"

if (-not (Test-Path $Paquete)) { throw "No existe el paquete: $Paquete" }
if (-not (Test-Path $Config)) { throw "No existe la configuracion: $Config" }
if (-not (Test-Path $AppLib)) { throw "No se encontro la aplicacion en: $AppLib" }
if (-not (Get-Command pg_dump -ErrorAction SilentlyContinue)) { throw "pg_dump no esta en PATH" }
if (-not (Get-Command pg_restore -ErrorAction SilentlyContinue)) { throw "pg_restore no esta en PATH" }

function Leer-Clave([string] $clave) {
    $linea = Get-Content $Config | Where-Object { $_ -match "^$([regex]::Escape($clave))=" } | Select-Object -Last 1
    if (-not $linea) { return "" }
    return ($linea -split "=", 2)[1]
}

$DbUrl = Leer-Clave "db.url"
$DbUser = Leer-Clave "db.usuario"
$DbPass = Leer-Clave "db.password"
if ($DbUrl -notmatch "^jdbc:postgresql://([^:/]+)(:([0-9]+))?/([^?]+)") {
    throw "db.url no tiene el formato esperado: $DbUrl"
}
$DbHost = $Matches[1]
$DbPort = if ($Matches[3]) { $Matches[3] } else { "5432" }
$DbName = $Matches[4]

New-Item -ItemType Directory -Force -Path $Backup | Out-Null
$Dump = Join-Path $Backup "base-$Marca.dump"
Write-Host "==> Respaldo previo: $Dump"
$env:PGPASSWORD = $DbPass
& pg_dump --format=custom --no-owner --no-privileges --host=$DbHost --port=$DbPort `
    --username=$DbUser --dbname=$DbName --file=$Dump
if ($LASTEXITCODE -ne 0) { throw "Fallo pg_dump" }
& pg_restore --list $Dump | Out-Null
if ($LASTEXITCODE -ne 0) { throw "El respaldo no pudo verificarse" }
Write-Host "==> Respaldo verificado"

$Staging = Join-Path $env:TEMP "sfjfx-update-$Marca"
New-Item -ItemType Directory -Force -Path $Staging | Out-Null
Expand-Archive -Path $Paquete -DestinationPath $Staging -Force

Write-Host "==> Resguardando jars actuales"
$BackupApp = Join-Path $Backup "app"
New-Item -ItemType Directory -Force -Path $BackupApp | Out-Null
Copy-Item -Recurse -Force (Join-Path $AppLib "*") $BackupApp

Write-Host "==> Instalando nuevos jars"
Get-ChildItem $Staging -Filter "*.jar" | Copy-Item -Destination $AppLib -Force

Write-Host "==> Aplicando migraciones"
& $Java -cp "$AppLib\*" ec.sfjfx.core.arranque.Arranque
if ($LASTEXITCODE -ne 0) {
    Write-Host "==> Migracion fallida. Restaurando jars anteriores." -ForegroundColor Red
    Copy-Item -Recurse -Force (Join-Path $BackupApp "*") $AppLib
    throw "Restaure la base desde $Dump antes de reiniciar la aplicacion."
}

Remove-Item -Recurse -Force $Staging -ErrorAction SilentlyContinue
Write-Host "==> Actualizacion completada"
Write-Host "Reinicie la aplicacion: $(Join-Path $AppDir 'sf-jfx.exe')"
Write-Host "Respaldo previo: $Dump"
