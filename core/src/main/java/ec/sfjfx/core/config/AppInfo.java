package ec.sfjfx.core.config;

/** Identidad y version de la aplicacion. */
public final class AppInfo {

    public static final String NOMBRE = "sf-jfx";

    private AppInfo() {
    }

    public static String version() {
        String propiedad = System.getProperty("sfjfx.version");
        if (propiedad != null && !propiedad.isBlank()) {
            return propiedad.trim();
        }
        Package paquete = AppInfo.class.getPackage();
        if (paquete != null && paquete.getImplementationVersion() != null
                && !paquete.getImplementationVersion().isBlank()) {
            return paquete.getImplementationVersion();
        }
        return "0.1.0-SNAPSHOT";
    }
}
