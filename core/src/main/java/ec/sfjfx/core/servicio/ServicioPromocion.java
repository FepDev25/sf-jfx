package ec.sfjfx.core.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ec.sfjfx.core.modelo.Promocion;
import ec.sfjfx.core.modelo.PromocionAlcance;
import ec.sfjfx.core.repositorio.PromocionRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;
import ec.sfjfx.core.util.Validaciones;

/** Promociones: CRUD y evaluacion de la mejor oferta aplicable (sin acumular). */
public final class ServicioPromocion {

    /** Oferta calculada para una linea. */
    public record Oferta(Long promocionId, String nombre, BigDecimal descuento) {
    }

    private final PromocionRepositorio repo = new PromocionRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<Promocion> listar() {
        return repo.listar(empresa());
    }

    public List<Promocion> activas() {
        return repo.activas(empresa());
    }

    public Promocion guardar(Promocion p) {
        Validaciones.requerido(p.nombre(), "Nombre");
        Validaciones.requerido(p.tipo(), "Tipo");
        if ("NXM".equals(p.tipo())) {
            if (p.cantidadRequerida() == null || p.cantidadPagada() == null
                    || p.cantidadPagada() >= p.cantidadRequerida()) {
                throw new ValidacionExcepcion("En NxM la cantidad pagada debe ser menor a la requerida");
            }
        }
        if (p.id() == null) {
            long id = repo.crear(p);
            auditoria.registrar("PROMOCIONES", "promocion", id, "CREAR", null, Map.of("nombre", p.nombre()));
            return new Promocion(id, p.empresaId(), p.nombre(), p.descripcion(), p.tipo(), p.valor(),
                    p.cantidadRequerida(), p.cantidadPagada(), p.prioridad(), p.combinable(), p.vigenteDesde(),
                    p.vigenteHasta(), p.estado(), p.activo(), p.alcances(), p.componentes());
        }
        repo.actualizar(p);
        auditoria.registrar("PROMOCIONES", "promocion", p.id(), "EDITAR", null, Map.of("nombre", p.nombre()));
        return p;
    }

    /** Devuelve la mejor oferta aplicable a una linea (mayor prioridad; no acumula). */
    public Optional<Oferta> evaluar(Long productoId, Long categoriaId, Long marcaId, BigDecimal cantidad,
                                    BigDecimal precioUnitario, Long sucursalId) {
        if (cantidad == null || cantidad.signum() <= 0 || precioUnitario == null) {
            return Optional.empty();
        }
        LocalDate hoy = LocalDate.now();
        Oferta mejor = null;
        int mejorPrioridad = Integer.MIN_VALUE;
        for (Promocion p : repo.activas(empresa())) {
            if (!vigente(p, hoy) || !aplica(p, productoId, categoriaId, marcaId, sucursalId)) {
                continue;
            }
            BigDecimal descuento = calcular(p, cantidad, precioUnitario);
            if (descuento.signum() <= 0) {
                continue;
            }
            if (mejor == null || p.prioridad() > mejorPrioridad) {
                mejor = new Oferta(p.id(), p.nombre(), descuento);
                mejorPrioridad = p.prioridad();
            }
        }
        return Optional.ofNullable(mejor);
    }

    private BigDecimal calcular(Promocion p, BigDecimal cantidad, BigDecimal precioUnitario) {
        BigDecimal base = cantidad.multiply(precioUnitario);
        return switch (p.tipo()) {
            case "PORCENTAJE" -> base.multiply(nz(p.valor())).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            case "MONTO" -> nz(p.valor()).min(base);
            case "PRECIO_FIJO" -> {
                BigDecimal dif = precioUnitario.subtract(nz(p.valor()));
                yield dif.signum() > 0 ? dif.multiply(cantidad) : BigDecimal.ZERO;
            }
            case "NXM" -> {
                int req = p.cantidadRequerida() == null ? 1 : p.cantidadRequerida();
                int pag = p.cantidadPagada() == null ? req : p.cantidadPagada();
                int gratis = cantidad.intValue() / req * (req - pag);
                yield precioUnitario.multiply(BigDecimal.valueOf(gratis));
            }
            default -> BigDecimal.ZERO;
        };
    }

    private boolean vigente(Promocion p, LocalDate hoy) {
        if (p.vigenteDesde() != null && hoy.isBefore(p.vigenteDesde())) {
            return false;
        }
        return p.vigenteHasta() == null || !hoy.isAfter(p.vigenteHasta());
    }

    private boolean aplica(Promocion p, Long productoId, Long categoriaId, Long marcaId, Long sucursalId) {
        if (p.alcances().isEmpty()) {
            return true;
        }
        for (PromocionAlcance a : p.alcances()) {
            Long ref = a.referenciaId();
            boolean coincide = switch (a.tipoAlcance()) {
                case "PRODUCTO" -> ref != null && ref.equals(productoId);
                case "CATEGORIA" -> ref != null && ref.equals(categoriaId);
                case "MARCA" -> ref != null && ref.equals(marcaId);
                case "SUCURSAL" -> ref != null && ref.equals(sucursalId);
                default -> false;
            };
            if (coincide) {
                return true;
            }
        }
        return false;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }
}
