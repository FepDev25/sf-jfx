package ec.sfjfx.dev;

import ec.sfjfx.core.config.AppConfig;

/**
 * Guardia de entorno para las semillas de desarrollo.
 *
 * <p>Las semillas no forman parte del producto instalable y nunca deben
 * ejecutarse en produccion. Esta clase centraliza el rechazo para que
 * {@link SemillaDesarrollo} y {@link SemillaDemo} no dependan de que el
 * operador recuerde la restriccion.
 */
final class GuardiaSemilla {

    private GuardiaSemilla() {
    }

    static void verificarEntorno() {
        if (AppConfig.esProduccion()) {
            throw new IllegalStateException(
                    "Semillas deshabilitadas: app.entorno=" + AppConfig.entorno()
                    + ". Ejecute esta herramienta solo en un entorno de desarrollo/local."
                    + " Las semillas nunca deben usarse en una instalacion de cliente.");
        }
    }
}
