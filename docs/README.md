# docs/ — sf-jfx

Documentación de diseño del sistema de facturación para supermercados.

| Documento | Contenido |
|-----------|-----------|
| [`decisiones.md`](decisiones.md) | Bitácora de decisiones por fase (stack, alcance, entidades, UI). Fuente de verdad de "qué se decidió". |
| [`modelo-datos.md`](modelo-datos.md) | Entidades y atributos del core, agrupados por módulo. |
| [`vistas-ui.md`](vistas-ui.md) | Inventario de pantallas necesarias y quién las usa. |
| [`roadmap.md`](roadmap.md) | Arquitectura de módulos Maven, hitos de construcción y pendientes. |
| [`datos-demo.md`](datos-demo.md) | Credenciales, contenido de la semilla masiva y escenarios de prueba. |
| [`plan-migracion-homelab.md`](plan-migracion-homelab.md) | Plan para salir de Docker en Arch, migrar las bases al homelab y montar el laboratorio de release. |
| [`adr/`](adr/) | Decisiones de arquitectura (ADR). La primera define el modelo de despliegue. |
| [`dist/`](../dist/) | Plantillas de empaquetado, actualización y operación (Linux/Windows). |
| [`mockups/`](mockups/) | Prototipos visuales HTML de las pantallas clave. |

Estado: **Hitos 0 a 11 cerrados**. Alcance funcional congelado. El **Hito 12 (despliegue
auto-instalable)** está documentado en el [ADR-0001](adr/0001-modelo-despliegue-single-tenant.md):
**Fases 0 y 1 completadas; Fase 2 en curso** (base operativa, respaldos, licencia, modo consulta,
logs, healthcheck y updates; empaquetado nativo pendiente de release); Fase 3 pendiente.

El laboratorio de release en Debian validó el `app-image` Linux, el primer arranque, el
healthcheck, respaldo/restauración, migraciones desde el jar, licencia y modo consulta; el
detalle y los pendientes están en [`plan-migracion-homelab.md`](plan-migracion-homelab.md).

Lo que queda abierto está en
[roadmap.md → Pendientes para la próxima sesión](roadmap.md#pendientes-para-la-próxima-sesión).

Para probar la aplicación: [`datos-demo.md`](datos-demo.md) (usuarios, datos y reinicio del entorno).
