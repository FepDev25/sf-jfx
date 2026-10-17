package ec.sfjfx.core.servicio;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import ec.sfjfx.core.modelo.Categoria;
import ec.sfjfx.core.modelo.ListaPrecio;
import ec.sfjfx.core.modelo.Marca;
import ec.sfjfx.core.modelo.PrecioProducto;
import ec.sfjfx.core.modelo.Producto;
import ec.sfjfx.core.modelo.ProductoCompleto;
import ec.sfjfx.core.modelo.TipoListaPrecio;
import ec.sfjfx.core.modelo.TipoProducto;
import ec.sfjfx.core.modelo.UnidadMedida;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.Csv;
import ec.sfjfx.core.util.LectorTabla;

/** Importacion y exportacion de catalogo en CSV. */
public final class ServicioCatalogoCsv {

    private static final String[] CABECERA = {
            "codigo", "nombre", "categoria", "marca", "unidad", "tipo", "costo", "precio_publico", "activo"};

    private final ServicioProducto servicioProducto = new ServicioProducto();
    private final ServicioCategoria servicioCategoria = new ServicioCategoria();
    private final ServicioMarca servicioMarca = new ServicioMarca();
    private final ServicioUnidadMedida servicioUnidad = new ServicioUnidadMedida();
    private final ServicioListaPrecio servicioLista = new ServicioListaPrecio();

    /** Resultado de una importacion. */
    public record ResultadoImportacion(int creados, int omitidos, List<String> errores) {
    }

    public void exportar(Path destino) throws IOException {
        List<Producto> productos = servicioProducto.listar(null);
        Map<Long, String> categorias = mapaNombre(servicioCategoria.listar(), Categoria::nombre, Categoria::id);
        Map<Long, String> marcas = mapaNombre(servicioMarca.listar(), Marca::nombre, Marca::id);
        Map<Long, String> unidades = mapaNombre(servicioUnidad.listar(), UnidadMedida::codigo, UnidadMedida::id);

        List<String[]> filas = new ArrayList<>();
        filas.add(CABECERA);
        for (Producto p : productos) {
            ProductoCompleto completo = servicioProducto.cargar(p.id());
            filas.add(new String[]{
                    p.codigo(),
                    p.nombre(),
                    texto(categorias.get(p.categoriaId())),
                    texto(marcas.get(p.marcaId())),
                    texto(unidades.get(p.unidadMedidaId())),
                    p.tipo().name(),
                    valor(p.costo()),
                    precioPublico(completo),
                    Boolean.toString(p.activo())});
        }
        try (Writer w = Files.newBufferedWriter(destino, StandardCharsets.UTF_8)) {
            Csv.escribir(w, filas);
        }
    }

    public ResultadoImportacion importar(Path origen) throws IOException {
        List<String[]> filas = LectorTabla.leer(origen);
        int creados = 0;
        int omitidos = 0;
        List<String> errores = new ArrayList<>();

        Map<String, Long> categorias = idPorNombre(servicioCategoria.listar(), Categoria::nombre, Categoria::id);
        Map<String, Long> marcas = idPorNombre(servicioMarca.listar(), Marca::nombre, Marca::id);
        Map<String, Long> unidades = idPorNombre(servicioUnidad.listar(), UnidadMedida::codigo, UnidadMedida::id);
        Set<String> codigosExistentes = new java.util.HashSet<>();
        servicioProducto.listar(null).forEach(p -> codigosExistentes.add(p.codigo().toLowerCase()));
        ListaPrecio listaPublico = servicioLista.activas().stream()
                .filter(l -> l.tipo() == TipoListaPrecio.PUBLICO).findFirst().orElse(null);

        for (int i = 1; i < filas.size(); i++) {
            String[] fila = filas.get(i);
            if (fila.length < 2 || fila[0].isBlank()) {
                continue;
            }
            try {
                String codigo = fila[0].trim();
                if (codigosExistentes.contains(codigo.toLowerCase())) {
                    omitidos++;
                    continue;
                }
                Producto producto = new Producto(null, empresa(), codigo, fila[1].trim(), null,
                        categorias.get(texto(fila, 2).toLowerCase()), marcas.get(texto(fila, 3).toLowerCase()),
                        unidades.get(texto(fila, 4).toLowerCase()), tipo(fila, 5), null,
                        decimal(fila, 6), BigDecimal.ZERO, false, false,
                        tipo(fila, 5) == TipoProducto.GRANEL, null, null, null, null, null, true);

                List<PrecioProducto> precios = new ArrayList<>();
                if (listaPublico != null && !texto(fila, 7).isBlank()) {
                    precios.add(new PrecioProducto(null, listaPublico.id(), listaPublico.nombre(), 0L,
                            decimal(fila, 7), null, null, null));
                }

                servicioProducto.guardar(new ProductoCompleto(producto, List.of(), List.of(), Set.of(),
                        List.of(), Map.of(), precios, List.of()));
                codigosExistentes.add(codigo.toLowerCase());
                creados++;
            } catch (RuntimeException e) {
                errores.add("Fila " + (i + 1) + ": " + e.getMessage());
            }
        }
        return new ResultadoImportacion(creados, omitidos, errores);
    }

    private String precioPublico(ProductoCompleto completo) {
        return completo.precios().stream()
                .filter(p -> p.listaPrecioNombre() != null && p.listaPrecioNombre().toLowerCase().contains("public"))
                .map(p -> valor(p.precio())).findFirst().orElse("");
    }

    private String texto(String[] fila, int idx) {
        return fila.length > idx && fila[idx] != null ? fila[idx].trim() : "";
    }

    private String texto(String v) {
        return v == null ? "" : v;
    }

    private TipoProducto tipo(String[] fila, int idx) {
        try {
            return TipoProducto.valueOf(texto(fila, idx).toUpperCase());
        } catch (IllegalArgumentException e) {
            return TipoProducto.SIMPLE;
        }
    }

    private BigDecimal decimal(String[] fila, int idx) {
        String v = texto(fila, idx);
        try {
            return v.isBlank() ? BigDecimal.ZERO : new BigDecimal(v.replace(",", "."));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private String valor(BigDecimal v) {
        return v == null ? "" : v.toPlainString();
    }

    private <T> Map<String, Long> idPorNombre(List<T> elementos, Function<T, String> nombre,
                                              Function<T, Long> id) {
        Map<String, Long> mapa = new HashMap<>();
        for (T e : elementos) {
            String n = nombre.apply(e);
            if (n != null) {
                mapa.put(n.toLowerCase(), id.apply(e));
            }
        }
        return mapa;
    }

    private <T> Map<Long, String> mapaNombre(List<T> elementos, Function<T, String> nombre,
                                             Function<T, Long> id) {
        Map<Long, String> mapa = new HashMap<>();
        for (T e : elementos) {
            mapa.put(id.apply(e), nombre.apply(e));
        }
        return mapa;
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }
}
