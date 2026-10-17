package ec.sfjfx.core.servicio;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import ec.sfjfx.core.modelo.Cliente;
import ec.sfjfx.core.modelo.Proveedor;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.Csv;
import ec.sfjfx.core.util.LectorTabla;
import ec.sfjfx.core.util.ValidacionExcepcion;

/** Importacion masiva de clientes y proveedores desde CSV o Excel, con vista previa. */
public final class ServicioImportacion {

    private final ServicioCliente clientes = new ServicioCliente();
    private final ServicioProveedor proveedores = new ServicioProveedor();

    public record Analisis(String[] cabecera, List<String[]> filas, List<String> errores) {
    }

    public record Resultado(int creados, int omitidos, List<String> errores) {
    }

    public Analisis analizar(TipoImportacion tipo, Path archivo) throws IOException {
        List<String[]> crudas = LectorTabla.leer(archivo);
        if (crudas.isEmpty()) {
            throw new ValidacionExcepcion("El archivo esta vacio");
        }
        String[] cabecera = crudas.get(0);
        validarCabecera(tipo, cabecera);
        List<String[]> filas = new ArrayList<>();
        for (int i = 1; i < crudas.size(); i++) {
            if (!filaVacia(crudas.get(i))) {
                filas.add(crudas.get(i));
            }
        }
        List<String> errores = new ArrayList<>();
        if (filas.isEmpty()) {
            errores.add("El archivo no tiene filas de datos");
        }
        return new Analisis(cabecera, filas, errores);
    }

    public Resultado importar(TipoImportacion tipo, Path archivo) throws IOException {
        List<String[]> filas = leer(tipo, archivo);
        return switch (tipo) {
            case CLIENTES -> importarClientes(filas);
            case PROVEEDORES -> importarProveedores(filas);
        };
    }

    public void plantilla(TipoImportacion tipo, Path destino) throws IOException {
        List<String[]> filas = new ArrayList<>();
        filas.add(tipo.cabecera());
        filas.add(tipo == TipoImportacion.CLIENTES
                ? new String[]{"EMPRESA", "RUC", "1790012345001", "", "", "Comercial Demo",
                    "compras@demo.ec", "0999999999", "Av. Siempre Viva", "SI", "500", "30"}
                : new String[]{"RUC", "1790012345001", "Proveedor Demo", "Proveedor Demo",
                    "Av. Siempre Viva", "0999999999", "ventas@demo.ec", "CONTRIBUYENTE_GENERAL",
                    "30", "SI", "NO", ""});
        try (Writer writer = Files.newBufferedWriter(destino, StandardCharsets.UTF_8)) {
            Csv.escribir(writer, filas);
        }
    }

    public List<String[]> leer(TipoImportacion tipo, Path archivo) throws IOException {
        return analizar(tipo, archivo).filas();
    }

    private void validarCabecera(TipoImportacion tipo, String[] cabecera) {
        Map<String, Integer> indices = indices(cabecera);
        List<String> faltantes = new ArrayList<>();
        for (String requerida : requeridas(tipo)) {
            if (!indices.containsKey(requerida)) {
                faltantes.add(requerida);
            }
        }
        if (!faltantes.isEmpty()) {
            throw new ValidacionExcepcion("Faltan columnas obligatorias: " + String.join(", ", faltantes));
        }
    }

    private Resultado importarClientes(List<String[]> filas) {
        int creados = 0;
        int omitidos = 0;
        List<String> errores = new ArrayList<>();
        Set<String> existentes = new HashSet<>();
        clientes.listar(null).forEach(c -> existentes.add(normalizarId(c.numeroIdentificacion())));

        Map<String, Integer> indices = indices(TipoImportacion.CLIENTES.cabecera());
        for (int i = 0; i < filas.size(); i++) {
            String[] fila = filas.get(i);
            try {
                String tipo = texto(fila, indices, "tipo").toUpperCase(Locale.ROOT);
                String razon = texto(fila, indices, "razon_social");
                if (tipo.isBlank()) {
                    tipo = razon.isBlank() ? "PERSONA_NATURAL" : "EMPRESA";
                }
                if (!Set.of("CONSUMIDOR_FINAL", "PERSONA_NATURAL", "EMPRESA").contains(tipo)) {
                    throw new ValidacionExcepcion("Tipo de cliente no valido: " + tipo);
                }
                String tipoId = texto(fila, indices, "tipo_identificacion").toUpperCase(Locale.ROOT);
                if (tipoId.isBlank()) {
                    tipoId = switch (tipo) {
                        case "EMPRESA" -> "RUC";
                        case "CONSUMIDOR_FINAL" -> "CONSUMIDOR_FINAL";
                        default -> "CEDULA";
                    };
                }
                if (!Set.of("CEDULA", "RUC", "PASAPORTE", "CONSUMIDOR_FINAL").contains(tipoId)) {
                    throw new ValidacionExcepcion("Tipo de identificacion no valido: " + tipoId);
                }
                String numero = texto(fila, indices, "numero_identificacion");
                if (numero.isBlank() && "CONSUMIDOR_FINAL".equals(tipo)) {
                    numero = ServicioCliente.ID_CONSUMIDOR_FINAL;
                }
                if (numero.isBlank()) {
                    throw new ValidacionExcepcion("Falta la identificacion");
                }
                if (existentes.contains(normalizarId(numero))) {
                    omitidos++;
                    continue;
                }
                Cliente cliente = new Cliente(null, empresa(), tipo, tipoId, numero,
                        nulo(texto(fila, indices, "nombres")), nulo(texto(fila, indices, "apellidos")),
                        nulo(razon), nulo(texto(fila, indices, "email")),
                        nulo(texto(fila, indices, "telefono")), nulo(texto(fila, indices, "direccion")),
                        booleano(texto(fila, indices, "es_credito")),
                        decimal(texto(fila, indices, "limite_credito")),
                        entero(texto(fila, indices, "dias_credito")), BigDecimal.ZERO, true);
                clientes.guardar(cliente);
                existentes.add(normalizarId(numero));
                creados++;
            } catch (RuntimeException e) {
                errores.add("Fila " + (i + 2) + ": " + e.getMessage());
            }
        }
        return new Resultado(creados, omitidos, errores);
    }

    private Resultado importarProveedores(List<String[]> filas) {
        int creados = 0;
        int omitidos = 0;
        List<String> errores = new ArrayList<>();
        Set<String> existentes = new HashSet<>();
        proveedores.listar(null).forEach(p -> existentes.add(normalizarId(p.numeroIdentificacion())));

        Map<String, Integer> indices = indices(TipoImportacion.PROVEEDORES.cabecera());
        for (int i = 0; i < filas.size(); i++) {
            String[] fila = filas.get(i);
            try {
                String tipoId = texto(fila, indices, "tipo_identificacion").toUpperCase(Locale.ROOT);
                if (tipoId.isBlank()) {
                    tipoId = "RUC";
                }
                if (!Set.of("RUC", "CEDULA", "PASAPORTE").contains(tipoId)) {
                    throw new ValidacionExcepcion("Tipo de identificacion no valido: " + tipoId);
                }
                String numero = texto(fila, indices, "numero_identificacion");
                if (existentes.contains(normalizarId(numero))) {
                    omitidos++;
                    continue;
                }
                Proveedor proveedor = new Proveedor(null, empresa(), tipoId, numero,
                        texto(fila, indices, "razon_social"), nulo(texto(fila, indices, "nombre_comercial")),
                        nulo(texto(fila, indices, "direccion")), nulo(texto(fila, indices, "telefono")),
                        nulo(texto(fila, indices, "email")), nulo(texto(fila, indices, "tipo_contribuyente")),
                        entero(texto(fila, indices, "dias_credito")),
                        booleano(texto(fila, indices, "aplica_retencion_iva")),
                        booleano(texto(fila, indices, "aplica_retencion_renta")), true,
                        nulo(texto(fila, indices, "nota")));
                proveedores.guardar(proveedor);
                existentes.add(normalizarId(numero));
                creados++;
            } catch (RuntimeException e) {
                errores.add("Fila " + (i + 2) + ": " + e.getMessage());
            }
        }
        return new Resultado(creados, omitidos, errores);
    }

    private List<String> requeridas(TipoImportacion tipo) {
        return tipo == TipoImportacion.CLIENTES
                ? List.of("numero_identificacion")
                : List.of("numero_identificacion", "razon_social");
    }

    private static Map<String, Integer> indices(String[] cabecera) {
        Map<String, Integer> indices = new HashMap<>();
        for (int i = 0; i < cabecera.length; i++) {
            indices.put(normalizar(cabecera[i]), i);
        }
        return indices;
    }

    private static String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        String limpio = valor.replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT);
        return limpio;
    }

    private static String normalizarId(String valor) {
        return valor == null ? "" : valor.replaceAll("[^0-9A-Za-z]", "").toLowerCase(Locale.ROOT);
    }

    private static String texto(String[] fila, Map<String, Integer> indices, String columna) {
        Integer idx = indices.get(columna);
        return idx == null || idx >= fila.length || fila[idx] == null ? "" : fila[idx].trim();
    }

    private static String nulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static boolean booleano(String valor) {
        String v = valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
        return v.equals("SI") || v.equals("S") || v.equals("TRUE") || v.equals("1") || v.equals("X");
    }

    private static int entero(String valor) {
        try {
            return valor == null || valor.isBlank() ? 0 : Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static BigDecimal decimal(String valor) {
        try {
            return valor == null || valor.isBlank() ? BigDecimal.ZERO
                    : new BigDecimal(valor.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static boolean filaVacia(String[] fila) {
        for (String valor : fila) {
            if (valor != null && !valor.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }
}
