# Plan de migración al homelab y laboratorio de release — sf-jfx

- **Estado:** `PROPUESTO` — pendiente de validación antes de ejecutar.
- **Fecha:** 2026-10-22.
- **Ámbito:** sacar Docker y PostgreSQL de Arch para sf-jfx, migrar sus bases al servidor
  Debian y convertir ese servidor en el banco de pruebas de release (staging).
- **Relacionado:** [`../README.md`](../README.md), [`adr/0001-modelo-despliegue-single-tenant.md`](adr/0001-modelo-despliegue-single-tenant.md),
  [`roadmap.md`](roadmap.md) (Hito 12), [`datos-demo.md`](datos-demo.md) y la guía operativa
  del homelab (documento externo al repositorio).

> **Regla de oro:** el servidor es un **laboratorio de desarrollo/staging**, no producción.
> No se migran datos reales de clientes ni se ofrece disponibilidad. La instalación real sigue
> siendo single-tenant, en la PC del cliente.

---

## 1. Objetivo

1. **Arch sin Docker y sin PostgreSQL para sf-jfx.** Ningún contenedor, volumen ni base local
   del proyecto debe quedar en la PC de desarrollo.
2. **Bases en el Debian**, en el clúster PostgreSQL 17 nativo, con un rol y una base por
   propósito (dev, test, staging).
3. **Pruebas automatizadas sin Docker local.** Los 86 tests se ejecutan en el Debian contra una
   base aislada; Arch solo conserva el código y la UI.
4. **Laboratorio de release.** Validar en el Debian lo que falta de la Fase 2 del Hito 12:
   `app-image`, primer arranque, migraciones desde el jar, respaldo/restauración, healthcheck,
   licencia, modo consulta y canal de actualizaciones.
5. **Preparar el camino a Ubuntu.** Debian 13 es mucho más cercano a Ubuntu que Arch; la
   compatibilidad final con Ubuntu 22.04/24.04 se valida en una fase posterior.

## 2. Decisión de arquitectura

```text
ARCH (solo código + UI; sin Docker ni PostgreSQL de sf-jfx)
  ├─ UI JavaFX dev/demo ── túnel SSH 127.0.0.1:15432 ──▶ sfjfx_dev
  └─ Pruebas ── rsync + ssh ──▶ mvn test en Debian ──▶ sfjfx_test

DEBIAN (homelab, PostgreSQL 17.11 nativo en 127.0.0.1:5432)
  ├─ neonvibe        (intacta, no tocar)
  ├─ desarrollo      (intacta, no tocar)
  ├─ sfjfx_dev       (datos demo/dev; UI desde Arch)
  ├─ sfjfx_test      (tests automatizados; aislada)
  └─ sfjfx_staging   (instalación limpia; release lab)

  AppImage sf-jfx (staging, bajo demanda)
  └─ GUI: ssh -X (validado en LAN y Cloudflare) o noVNC si va lento
```

### Matriz de entornos

| Entorno | Base | Dónde corre | Para qué | Se puede borrar |
|---|---|---|---|---|
| **dev/demo** | `sfjfx_dev` (Debian) | Arch | Trabajo diario y demos | Sí, se regenera con dump/seed |
| **test** | `sfjfx_test` (Debian) | Debian | `mvn test` (86 pruebas) | Sí, se recrea |
| **staging** | `sfjfx_staging` (Debian) | Debian | Validar release, instalador y GUI | Sí, es laboratorio |
| **producción** | Base del cliente | PC del cliente | Operación real | Fuera de este plan |

### Nombres propuestos

| Rol PostgreSQL | Base | Uso |
|---|---|---|
| `sfjfx_dev` | `sfjfx_dev` | Desarrollo/demo desde Arch |
| `sfjfx_test` | `sfjfx_test` | Pruebas automatizadas |
| `sfjfx_staging` | `sfjfx_staging` | Instalación de staging |

Todos los roles: `LOGIN`, **sin** `SUPERUSER`, **sin** `CREATEDB`, **sin** `CREATEROLE`, dueños
solo de su base. Contraseñas aleatorias en archivos `0600`; nunca en Git ni en este documento.

## 3. Estado de partida (verificado)

| Elemento | Estado |
|---|---|
| BD local sf-jfx | Docker `sfjfx-db`, 26 MB, 91 tablas, semilla demo v1, 1 empresa, 8 usuarios, 46 ventas, 37 productos |
| Docker local | 42 contenedores, 91 volúmenes, 8,73 GB; sf-jfx es una parte mínima. **No hacer prune global** |
| Servidor | Debian 13, PostgreSQL 17.11, 2 vCPU, 3,7 GiB RAM (~2,2 GiB disponibles), HDD, 542 GB libres |
| Toolchain servidor | JDK 21.0.12.1, Maven 3.9.9, `jpackage`, GTK3, XFCE instalado; LightDM inactivo |
| Túnel | `127.0.0.1:15432` → Debian `127.0.0.1:5432`, activo |
| IPv6 loopback | **Corregido y persistido:** `lo.disable_ipv6=0`, `::1/128` presente; `ssh -X` funciona en LAN y Cloudflare |
| Glibc | Arch 2.44 vs Debian 2.41 → el `app-image` debe construirse en Debian, no en Arch |
| Licencias | Clave privada en `~/.sfjfx/licencias/private-key.pem` (Arch); debe quedar en `0600` |
| Pruebas | 86 verdes con PostgreSQL; sin BD, 39 de integración se saltan en silencio |

## 4. Roles y responsabilidades

| Rol | Quién | Puede ejecutar | Host |
|---|---|---|---|
| **Agente** | Asistente | Lectura, dumps, rsync, builds, migraciones, edits de repo, comandos sin `sudo` | Arch y Debian (SSH como `felipep`) |
| **Usuario (sudo)** | Felipe | Crear roles/bases, instalar paquetes, cambiar configuración del sistema, desinstalar Docker | Debian |
| **Usuario (GUI)** | Felipe | Validar visualmente la UI, el asistente de instalación y el POS | Arch / Debian |
| **Usuario (decisiones)** | Felipe | Aprobar borrados, nombres, alcance y cambios de infraestructura | — |

> El agente **no** ejecuta `sudo`, `DROP DATABASE`, `docker system prune` ni borrados de datos
> sin confirmación explícita.

### Resumen de ejecución

| Fase | Yo, en Arch | Yo, en Debian (SSH) | Tú, en Arch | Tú, en Debian (sudo/GUI) |
|---|---|---|---|---|
| 0 — Preparación | `chmod 600`, dump, rsync | build con Maven | — | — |
| 1 — Bases | — | restore, seed, verificación | — | crear roles y bases |
| 2 — Tests | — | config y `mvn test` | — | — |
| 3 — Dev/demo | config y `javafx:run` | verificación de datos | smoke test de la UI | — |
| 4 — Retiro Docker | `docker compose down -v` | — | confirmar el borrado | — |
| 5 — Staging | lanzar por `ssh -X` | build y configuración | ver la ventana | completar el asistente |
| 6 — Validaciones | emitir licencia de staging | healthcheck, backups, restore, modo consulta | aprobar la licencia | — |
| 7 — Ubuntu | — | — | — | futura |
| 8 — Docker global | — | — | — | futura |

### Precondiciones

- `ssh` al Debian funciona por LAN y por Cloudflare; `ssh -X ... true` devuelve 0.
- El túnel `127.0.0.1:15432` está activo y `pg_isready` responde.
- Felipe tiene acceso `sudo` en el Debian para roles, bases y paquetes.
- El servidor conserva JDK 21, Maven 3.9.9 y `jpackage`; la Fase 0 no instala nada.
- El repo está limpio y este plan se commitea antes de ejecutar.
- Hay espacio para el dump y para el `app-image`; `neonvibe` y `desarrollo` están sanas.
- Se acepta que el Debian es staging/laboratorio, no producción.

## 5. Fases de ejecución

### Fase 0 — Preparación y seguridad `[ ]`

**Objetivo:** dejar un respaldo verificado y el entorno listo, sin tocar las bases del servidor.

| # | Paso | Ejecuta | Host |
|---|---|---|---|
| 0.1 | `chmod 600 ~/.sfjfx/licencias/private-key.pem` | Agente | Arch |
| 0.2 | Levantar `sfjfx-db` solo para el dump y volver a detenerlo | Agente | Arch |
| 0.3 | `pg_dump -Fc` de `sfjfx` a `~/respaldos/sfjfx-pre-migracion-20261022.dump` y verificar con `pg_restore --list` | Agente | Arch |
| 0.4 | `rsync` del repo a `~/sf-jfx` en Debian (sin `target/` ni `.git/`) y copia del dump a `~/sf-jfx-respaldos/` | Agente | Arch → Debian |
| 0.5 | `mvn -q -DskipTests install` en Debian para validar toolchain, instalar `core` y cachear dependencias | Agente | Debian |
| 0.6 | Verificar `df -h`, `free -h` y que `neonvibe`/`desarrollo` siguen sanas | Agente | Debian |

**Criterio de aceptación:** dump no vacío y listable; build correcto; ninguna base modificada.

**Rollback:** no aplica.

---

### Fase 1 — Bases en el Debian `[ ]`

**Objetivo:** crear las tres bases y restaurar/sembrar los datos iniciales.

| # | Paso | Ejecuta | Host |
|---|---|---|---|
| 1.1 | Generar contraseñas aleatorias y dejarlas en archivos `0600` (sin mostrarlas en el chat) | Agente | Arch / Debian |
| 1.2 | Crear roles `sfjfx_dev`, `sfjfx_test`, `sfjfx_staging` y sus bases | **Usuario (sudo)** | Debian |
| 1.3 | Restaurar el dump en `sfjfx_dev` con `pg_restore --no-owner --no-privileges` | Agente | Debian |
| 1.4 | Crear la configuración de `sfjfx_test` (`0600`) y aplicar migraciones + semilla demo (`app.entorno=dev`; la semilla crea `admin/admin`) | Agente | Debian |
| 1.5 | Dejar `sfjfx_staging` vacía (la usará el asistente) | Agente | Debian |
| 1.6 | Verificar conteos, tablas y que `neonvibe`/`desarrollo` no cambiaron | Agente | Debian |

**Comandos de referencia (los ejecuta el usuario con `sudo`):**

```bash
# En Debian, como felipep:
sudo -u postgres psql -c "CREATE ROLE sfjfx_dev LOGIN PASSWORD '<password>';"
sudo -u postgres psql -c "CREATE DATABASE sfjfx_dev OWNER sfjfx_dev;"
sudo -u postgres psql -c "REVOKE CONNECT ON DATABASE sfjfx_dev FROM PUBLIC;"
sudo -u postgres psql -c "GRANT CONNECT ON DATABASE sfjfx_dev TO sfjfx_dev;"
# Repetir para sfjfx_test y sfjfx_staging.
```

Restauración y siembra (las ejecuta el agente, sin `sudo`):

```bash
# En Debian, como felipep:
pg_restore -h 127.0.0.1 -p 5432 -U sfjfx_dev -d sfjfx_dev \
  --no-owner --no-privileges ~/sf-jfx-respaldos/sfjfx-pre-migracion-20261022.dump

# Migraciones + semilla demo en sfjfx_test (config en ~/.sfjfx/sfjfx.properties)
cd ~/sf-jfx
mvn -q -pl core exec:java
mvn -q -pl dev exec:java
```

**Criterio de aceptación:** `sfjfx_dev` con 91 tablas y los conteos del dump; `sfjfx_test` con
`admin/admin`; `neonvibe` y `desarrollo` intactas; PostgreSQL sigue escuchando solo en localhost.

**Rollback:** `DROP DATABASE`/`DROP ROLE` de las bases nuevas, solo con aprobación. No se toca
ninguna base existente.

---

### Fase 2 — Runner de pruebas sin Docker local `[ ]`

**Objetivo:** que los 86 tests corran en el servidor contra `sfjfx_test`, sin PostgreSQL local.

| # | Paso | Ejecuta | Host |
|---|---|---|---|
| 2.1 | Verificar `/home/felipep/.sfjfx/sfjfx.properties` (creado en 1.4) apuntando a `sfjfx_test`, con `app.entorno=dev` y `db.pool.maximo=5`, permisos `0600` | Agente | Debian |
| 2.2 | Ejecutar `mvn -q test` en el repo sincronizado | Agente | Debian |
| 2.3 | Confirmar 86 tests, 0 fallos, 0 errores, 0 saltos | Agente | Debian |
| 2.4 | Documentar el comando de pruebas remotas | Agente | Repo |

**Criterio de aceptación:** 86/86 verdes en Debian; ninguna prueba toca `sfjfx_dev` ni
`sfjfx_staging`.

**Rollback:** recrear `sfjfx_test` desde la semilla.

---

### Fase 3 — Dev/demo desde Arch `[ ]`

**Objetivo:** usar la UI de Arch contra `sfjfx_dev` por el túnel, sin base local.

| # | Paso | Ejecuta | Host |
|---|---|---|---|
| 3.1 | Configurar `~/.sfjfx/sfjfx.properties` (o `SFJFX_CONFIG`) con `127.0.0.1:15432/sfjfx_dev`, pool máximo 5 | Agente | Arch |
| 3.2 | `mvn -pl core install -DskipTests` y `mvn -pl ui javafx:run` | Agente | Arch |
| 3.3 | Smoke test: login `admin/admin`, abrir POS, agregar producto, cobrar, ver reportes | Usuario (GUI) | Arch |
| 3.4 | Verificar que la escritura persiste y que `sfjfx_dev` es la base usada | Agente | Arch / Debian |

**Criterio de aceptación:** la UI funciona igual que con Docker local; no hay PostgreSQL en Arch.

**Rollback:** volver temporalmente al Docker local (hasta la Fase 4) o restaurar el dump.

---

### Fase 4 — Retiro de Docker de sf-jfx en Arch `[ ]`

**Objetivo:** eliminar el contenedor y el volumen de sf-jfx, sin tocar otros proyectos.

| # | Paso | Ejecuta | Host |
|---|---|---|---|
| 4.1 | Confirmar dump verificado y UI/tests funcionando | Agente + Usuario | — |
| 4.2 | `docker compose down -v` para sf-jfx | Agente (con confirmación) | Arch |
| 4.3 | Verificar que no queda contenedor ni volumen de sf-jfx | Agente | Arch |
| 4.4 | **No** borrar `postgres:17-alpine` si otros proyectos lo usan | Agente | Arch |
| 4.5 | Actualizar README/docs y decidir el destino de `docker-compose.yml` | Agente + Usuario | Repo |

**Criterio de aceptación:** `docker ps -a` y `docker volume ls` sin rastros de sf-jfx; UI y tests
siguen funcionando contra el Debian.

**Rollback:** `docker compose up -d` y restaurar el dump; el volumen se conserva hasta la
confirmación final.

---

### Fase 5 — Staging y release lab `[ ]`

**Objetivo:** construir y ejecutar la aplicación como instalación real en el Debian.

| # | Paso | Ejecuta | Host |
|---|---|---|---|
| 5.1 | Construir `app-image` con `jpackage` en Debian (no en Arch) | Agente | Debian |
| 5.2 | Crear un `user.home` aislado para staging (`~/sfjfx-staging-home`) | Agente | Debian |
| 5.3 | Configurar `sfjfx_staging` y `app.entorno=prod` para probar el asistente real (es el flag de entorno de la app, no producción real) | Agente | Debian |
| 5.4 | Lanzar la UI con `ssh -X` desde Arch | Agente + Usuario (GUI) | Arch → Debian |
| 5.5 | Completar el asistente: empresa, RUC, sucursal, bodega, caja, administrador | Usuario (GUI) | Debian |
| 5.6 | Validar login, POS, reportes y persistencia en `sfjfx_staging` | Usuario (GUI) | Debian |
| 5.7 | Si `ssh -X` va lento, evaluar noVNC o xrdp como fallback | Agente + Usuario | Debian |

**Nota:** el `app-image` debe construirse en Debian por la diferencia de glibc. El script
`dist/linux/construir-instalador.sh` todavía tiene pendiente incluir PostgreSQL dentro del
`app-image`; para staging usaremos el PostgreSQL nativo y dejamos el empaquetado completo para
la fase de release.

**Criterio de aceptación:** el `app-image` arranca desde el servidor, el asistente crea la
instalación y la UI es usable por `ssh -X`.

**Rollback:** borrar `sfjfx_staging` y el directorio de staging; no afecta a dev/test.

---

### Fase 6 — Validaciones de la Fase 2 del roadmap `[ ]`

**Objetivo:** cerrar con evidencia los pendientes de release del Hito 12.

| # | Validación | Ejecuta | Host |
|---|---|---|---|
| 6.1 | `Arranque salud` desde el jar/runtime instalado; código de salida 0 | Agente | Debian |
| 6.2 | Respaldo desde UI y CLI; verificar con `pg_restore --list` | Agente | Debian |
| 6.3 | Restauración real en una base temporal (crear con autorización); comparar tablas y conteos | Agente | Debian |
| 6.4 | Migraciones desde el jar instalado sobre una base vacía temporal (`V1`–`V9`) | Agente | Debian |
| 6.5 | Licencia de staging: emitir en Arch con la clave privada, copiar solo el archivo firmado | Agente (con aprobación) | Arch → Debian |
| 6.6 | Modo consulta: licencia vencida o prueba agotada; confirmar bloqueo de ventas y consulta disponible | Agente + Usuario | Debian |
| 6.7 | Canal de actualización con `file:` y manifiesto firmado; luego HTTPS si aplica | Agente | Debian |
| 6.8 | Actualizar roadmap/ADR con los resultados | Agente | Repo |

**Criterio de aceptación:** healthcheck 0; respaldo/restauración verificados; migraciones desde
el jar; licencia y modo consulta funcionando; manifiesto de updates verificado.

**Rollback:** restaurar `sfjfx_staging` desde el respaldo; retirar la licencia de prueba.

---

### Fase 7 — Compatibilidad con Ubuntu `[ ]` (futura)

**Objetivo:** que el release real funcione en Ubuntu, no solo en Debian.

- Construir/probar en Ubuntu 24.04 y, si se soporta, Ubuntu 22.04 (contenedor o VM en el
  servidor; no en Arch).
- Verificar glibc, GTK, fuentes, `jpackage` y PostgreSQL incluido.
- Definir la versión mínima soportada y ajustar `dist/`.
- **No** forma parte de la sesión actual; se planifica después de validar staging en Debian.

---

### Fase 8 — Salida global de Docker de Arch `[ ]` (futura, fuera de alcance)

**Objetivo:** quitar Docker de Arch, no solo sf-jfx.

- Inventariar los 42 contenedores y 91 volúmenes, mapearlos a proyectos.
- Migrar/archivar cada proyecto (dump, datos, configuración) con el mismo patrón.
- Validar y luego desinstalar Docker/containerd y limpiar `/var/lib/docker`.
- **No** se ejecuta en esta sesión ni se hace `docker system prune` global sin inventario.

## 6. Seguridad y reglas de operación

1. **Secretos:** contraseñas en archivos `0600`; nunca en Git, en este documento ni en capturas.
2. **Clave privada de licencias:** se queda en Arch, con permisos `0600`; **nunca** se copia al
   Debian. Al servidor solo viaja el archivo de licencia firmado.
3. **Base de datos:** PostgreSQL sigue en `127.0.0.1:5432`. El acceso desde Arch es por el túnel
   SSH existente; no se abre `5432` a la red ni a internet.
4. **No tocar** `neonvibe`, `desarrollo`, Jellyfin, Hermes, Cloudflare ni la configuración del
   clúster (`pg_hba.conf`, `listen_addresses`, `max_connections`, `shared_buffers`).
5. **No hacer** `docker system prune --volumes`, `docker volume prune`, `DROP DATABASE` ni
   borrados de datos sin confirmación explícita.
6. **Pool pequeño** en dev/staging (`db.pool.maximo=5`) por RAM y por `max_connections=100`.
7. **GUI:** `ssh -X` funciona en LAN y por Cloudflare. Si se usa VNC/xrdp, escuchar solo en
   `127.0.0.1` y acceder por túnel SSH; nunca publicarlo.
8. **Cambios de infraestructura:** el usuario con `sudo` es quien crea roles/bases, instala
   paquetes y desinstala Docker.

## 7. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| El túnel o Cloudflare caen y la UI de Arch no conecta | Fallback por LAN (`192.168.1.59`) o restaurar el dump en una base temporal |
| Latencia Cloudflare + HDD en listados/reportes | Pool pequeño; pruebas y cargas pesadas en el servidor, no por el túnel |
| RAM insuficiente para JavaFX + GUI + servicios existentes | Staging bajo demanda; `-Xmx384m`/`512m`; monitorear `free -h` y swap |
| Glibc distinta entre Arch y Debian | Construir el `app-image` en Debian; validar Ubuntu en Fase 7 |
| Los tests contaminan `sfjfx_test` | Base dedicada y recreable; nunca usar `sfjfx_dev` para tests |
| Pérdida de la BD demo al borrar Docker | Dump verificado antes de `docker compose down -v`; conservarlo 30 días |
| Exposición accidental de VNC/RDP | Bind a `127.0.0.1` + túnel SSH; sin reglas públicas |
| Cambios en el clúster afectan `neonvibe` | Solo crear roles/bases nuevas; no tocar configuración global |

## 8. Criterios de aceptación del plan

- [ ] Arch no tiene contenedor, volumen ni PostgreSQL de sf-jfx.
- [ ] `sfjfx_dev`, `sfjfx_test` y `sfjfx_staging` existen en el Debian con roles de mínima
      autoridad.
- [ ] `mvn test` corre en el Debian con 86/86 verdes y 0 saltos.
- [ ] La UI de Arch funciona contra `sfjfx_dev` por el túnel.
- [ ] El `app-image` de staging arranca en Debian y el asistente crea una instalación limpia.
- [ ] `Arranque salud` devuelve 0 desde el artefacto instalado.
- [ ] Respaldo, restauración y migraciones desde el jar están verificados.
- [ ] Licencia y modo consulta se validan de extremo a extremo.
- [ ] `neonvibe` y `desarrollo` siguen intactas.
- [ ] La documentación refleja el nuevo flujo sin Docker local.

## 9. Decisiones pendientes antes de ejecutar

| # | Decisión | Propuesta |
|---|---|---|
| 1 | ¿Staging corre como `felipep` con `user.home` aislado o como usuario de sistema `sfjfx`? | `felipep` + home aislado; menos `sudo` y suficiente para el laboratorio |
| 2 | ¿Se elimina `docker-compose.yml` o queda como opción para otros desarrolladores? | Mantenerlo como opción documentada, sin usarlo en este Arch |
| 3 | ¿Los tests se ejecutan en Debian o desde Arch contra el túnel? | En Debian: sin latencia y valida el entorno tipo Ubuntu |
| 4 | ¿Licencia de staging? | Primero periodo de prueba; luego emitir una licencia de staging con la clave privada de Arch |
| 5 | ¿Nombres `sfjfx_dev` / `sfjfx_test` / `sfjfx_staging`? | Confirmar antes de crear roles y bases |

## 10. Registro de avance

| Fase | Estado | Fecha | Notas |
|---|---|---|---|
| 0 — Preparación y seguridad | `[ ]` | — | — |
| 1 — Bases en Debian | `[ ]` | — | — |
| 2 — Tests sin Docker | `[ ]` | — | — |
| 3 — Dev/demo desde Arch | `[ ]` | — | — |
| 4 — Retiro de Docker sf-jfx | `[ ]` | — | — |
| 5 — Staging y release lab | `[ ]` | — | — |
| 6 — Validaciones Fase 2 | `[ ]` | — | — |
| 7 — Compatibilidad Ubuntu | `[ ]` | — | Futura |
| 8 — Salida global de Docker | `[ ]` | — | Futura, fuera de alcance |

---

**Siguiente paso:** validar este documento y las cinco decisiones pendientes. Después se ejecuta
la Fase 0 y se reporta el dump verificado y el build en Debian antes de pedir el primer `sudo`.
