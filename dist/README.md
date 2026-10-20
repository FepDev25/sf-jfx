# Distribución y operación — sf-jfx

Esta carpeta contiene las plantillas de empaquetado y actualización para el modelo
**single-tenant auto-instalable** definido en el
[ADR-0001](../docs/adr/0001-modelo-despliegue-single-tenant.md).

> **Estado:** plantillas de referencia. La generación de instaladores nativos debe validarse en
> un entorno de release con `jpackage`, PostgreSQL portátil y los certificados de firma del
> proveedor. El producto no incluye el módulo `dev` ni las semillas.

## Componentes de una instalación

- Aplicación JavaFX (`ui` + `core` + dependencias).
- PostgreSQL local (versión fijada por el instalador).
- Configuración en `~/.sfjfx/sfjfx.properties`.
- Datos en el directorio de PostgreSQL.
- Respaldos en `~/.sfjfx/respaldos`.
- Licencia en `~/.sfjfx/licencia/licencia.properties`.
- Clave pública de licencias embebida en la aplicación (`core/src/main/resources/licencia/public-key.pem`).
- Clave privada de licencias: **fuera de esta instalación**, en custodia del proveedor.
- Registros en `~/.sfjfx/logs/sfjfx-YYYY-MM-DD.log`.
- Actualizaciones descargadas en `~/.sfjfx/actualizaciones`.

## Decisiones de despliegue

| Tema | Decisión |
|---|---|
| Alcance | Solo escritorio. No hay modo servidor ni API. |
| Formato | `app-image` (Windows y Linux) como formato base. |
| PostgreSQL | Incluido, con puerto libre detectado y credenciales aleatorias. |
| Canal de updates | Servidor HTTPS propio con manifiesto firmado. Alternativa `file:` sin internet. |
| Licencia vencida | Modo consulta: sin operación comercial, con datos y consultas intactos. |
| Clave pública | Embebida en el build. La privada se custodia fuera del repositorio. |
| Respaldos programados | Tarea del sistema operativo, con la aplicación cerrada. |

## Requisitos de release

- JDK 21 con `jpackage` y `jlink`.
- Maven 3.9+.
- PostgreSQL portátil de la misma versión que se distribuirá (binarios de `initdb`, `pg_ctl`,
  `pg_dump`, `pg_restore`).
- En Windows: WiX Toolset si se genera MSI; en Linux: `dpkg`/`rpm` si se genera paquete nativo.
- Clave privada de firma de licencias y de paquetes, custodiada fuera del repositorio.
- Certificado de firma de código para Windows/Linux (recomendado).

## Construcción del instalador

El formato de distribución es **`app-image`** para Windows y Linux. Es portable, permite
reemplazar la aplicación en una actualización y no exige herramientas de empaquetado del sistema.

### Linux

```bash
PG_HOME=/opt/postgresql-17 VERSION=0.1.0 ./dist/linux/construir-instalador.sh
```

Genera un `app-image` o paquete `.deb`/`.rpm` según las herramientas disponibles. El script:

1. Compila y prueba el proyecto.
2. Copia `core`, `ui` y las dependencias de runtime.
3. Incluye PostgreSQL portátil desde `PG_HOME`.
4. Excluye explícitamente el módulo `dev`.
5. Invoca `jpackage` con el launcher `sf-jfx`.

> **Pendiente de validación:** el empaquetado con `jpackage` y el arranque de JavaFX desde el
> `app-image` todavía no se han probado en este entorno. Es la primera tarea de la próxima
> sesión.

### Windows

```powershell
$env:PG_HOME = "C:\Program Files\PostgreSQL\17"
$env:VERSION = "0.1.0"
.\dist\windows\construir-instalador.ps1
```

Genera un `app-image` o MSI/EXE. Ajuste `PG_HOME`, la ruta de WiX y el certificado de firma.

> **Pendiente de validación:** el registro de PostgreSQL como servicio en Windows y el arranque de
> JavaFX desde el `app-image` quedan para la próxima sesión.

## Primer arranque

1. El instalador deja PostgreSQL inicializado y el servicio registrado.
2. Al abrir `sf-jfx` por primera vez, aparece el asistente de instalación.
3. El asistente crea empresa, sucursal, bodega, caja y administrador.
4. No se crean credenciales por defecto ni datos de demostración.
5. Para una prueba local, seleccione **Desarrollo local** y ejecute opcionalmente
   `mvn -pl dev exec:java` desde el repositorio. Nunca en un cliente real.

## Respaldo y restauración

- `pg_dump` genera un archivo custom y el servicio ejecuta `pg_restore --list` para verificarlo.
- La UI de **Configuración → Respaldos** permite respaldar, verificar, restaurar y programar.
- Antes de una actualización o un salto mayor de PostgreSQL, el técnico debe generar y verificar
  un respaldo.
- Pruebe la restauración al menos una vez por instalación.

### Respaldo programado (tarea del sistema operativo)

El respaldo no necesita la aplicación abierta: existe un comando que funciona sin sesión de
usuario y deja registro en el historial.

```bash
# Linux (cron): todos los días a la 01:30
30 1 * * * cd /opt/sf-jfx && ./bin/sf-jfx-migraciones respaldo >> /var/log/sfjfx-respaldo.log 2>&1
```

```powershell
# Windows (Task Scheduler): ejecute sf-jfx-migraciones.cmd respaldo todos los días a la 01:30
```

El comando `respaldo` genera el archivo, lo verifica y devuelve código de salida 0 o 1.

### Healthcheck

```bash
./bin/sf-jfx-migraciones salud    # código 0 si todo está correcto, 1 si hay problemas
```

Informe version, entorno, PostgreSQL, esquema (incluye migraciones pendientes), instalación,
licencia y modo consulta. Sirve para monitorización y para que el instalador valide el arranque.

## Actualización remota asistida

1. El proveedor publica un manifiesto firmado y un paquete `.zip`.
2. En **Configuración → Actualizaciones**, el técnico consulta el canal.
3. La aplicación verifica la firma del manifiesto y descarga el paquete.
4. La aplicación verifica el SHA-256 del paquete.
5. El técnico cierra la aplicación y ejecuta `actualizar.sh`/`actualizar.ps1`.
6. El script respalda, reemplaza la aplicación, aplica migraciones y permite rollback.

Manifiesto (`actualizacion.manifiesto`):

```properties
version=0.2.0
url=https://descargas.example.com/sf-jfx-0.2.0.zip
sha256=<sha256 hexadecimal>
notas=Correcciones y mejoras
firma=<firma RSA SHA-256 en Base64 del payload version/url/sha256/notas>
```

La firma se calcula sobre:

```text
version=<version>
url=<url>
sha256=<sha256>
notas=<notas>
```

La clave pública se configura con `actualizacion.clave_publica` o, por defecto, se reutiliza
`licencia.clave_publica`.

> **Importante:** el canal solo descarga y verifica. La aplicación del paquete es una acción
> explícita del técnico mediante el instalador/script. No hay actualización desatendida por flota.

## Licencias

La licencia es perpetua por instalación y el mantenimiento anual habilita updates y soporte.

### Generar claves (una sola vez, custodia offline)

```bash
mvn -pl core install -DskipTests
mvn -pl dev exec:java -Dexec.mainClass=ec.sfjfx.dev.LicenciaEmisor \
  -Dexec.args="generar-claves /ruta/public-key.pem /ruta/privada/private-key.pem"
```

- `public-key.pem` va embebida en el build (`core/src/main/resources/licencia/public-key.pem`).
- `private-key.pem` **nunca** se versiona ni se distribuye.

### Emitir una licencia

```bash
mvn -pl dev exec:java -Dexec.mainClass=ec.sfjfx.dev.LicenciaEmisor \
  -Dexec.args="emitir /ruta/privada/private-key.pem /ruta/licencia.properties \
  <codigoInstalacion> 'Cliente S.A.' 1790011678001 SUPERMERCADO 2 4 2027-12-31"
```

El `codigoInstalacion` se copia desde **Configuración → Licencia**. La licencia se carga desde la
misma pantalla.

### Modo consulta

| Situación | Operación comercial | Consulta y datos |
|---|---|---|
| Licencia vigente | Sí | Sí |
| Mantenimiento vencido, en gracia | Sí | Sí |
| Mantenimiento vencido, pasada la gracia | **No** | Sí |
| Sin licencia, en periodo de prueba | Sí | Sí |
| Sin licencia, agotado el periodo de prueba | **No** | Sí |

El modo consulta bloquea ventas, compras, comprobantes, devoluciones y movimientos de stock. La
aplicación sigue mostrando la información y no borra ni oculta datos. Los períodos se ajustan
con `licencia.gracia.dias` y `licencia.prueba.dias` (30 por defecto).

## Actualización mayor de PostgreSQL

No mezcle la actualización de la aplicación con un salto mayor de PostgreSQL:

1. Respalde y verifique.
2. Detenga el servicio.
3. Instale la nueva versión de PostgreSQL y ejecute `pg_upgrade` o `pg_dump`/`pg_restore`.
4. Verifique la base y arranque la aplicación.
5. Si falla, restaure el respaldo y vuelva a la versión anterior.

## Pendientes para la próxima sesión

Detalle completo en [`../docs/roadmap.md`](../docs/roadmap.md#pendientes-para-la-próxima-sesión).
Resumen de lo que falta para cerrar la Fase 2:

1. Generar el `app-image` de Linux con `jpackage` y validar que JavaFX arranca desde él.
2. Empaquetar PostgreSQL portátil con el script de primer arranque (`initdb`, usuario de sistema,
   unidad `systemd`) y verificar la instalación en una máquina limpia.
3. Replicar el proceso en Windows y registrar PostgreSQL como servicio.
4. Publicar el canal HTTPS con manifiesto firmado y ejecutar una actualización de punta a punta
   con su rollback.
5. Firmar los paquetes (código de Windows y clave de actualizaciones).
6. Decidir el comportamiento ante migraciones *forward-only* incompatibles con la versión
   anterior: restaurar la base o aplicar una migración correctiva.

---

## Checklist de puesta en marcha

- [ ] Instalador nativo generado y firmado.
- [ ] PostgreSQL incluido, inicializado y como servicio (con usuario de sistema en Linux).
- [ ] Puerto libre detectado y credenciales aleatorias en `sfjfx.properties` (permisos 0600).
- [ ] `app.entorno=prod`.
- [ ] Asistente de primer arranque completado.
- [ ] Administrador con contraseña y PIN propios.
- [ ] Respaldo inicial generado y verificado.
- [ ] Restauración de prueba realizada.
- [ ] Tarea del sistema operativo para el respaldo programado configurada.
- [ ] Healthcheck (`salud`) devolviendo 0.
- [ ] Registro con rotación activo en `~/.sfjfx/logs`.
- [ ] Licencia cargada y código de instalación registrado.
- [ ] Canal de actualizaciones configurado (si aplica).
- [ ] Procedimiento de rollback probado.
