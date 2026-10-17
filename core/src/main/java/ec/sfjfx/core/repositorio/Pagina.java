package ec.sfjfx.core.repositorio;

import java.util.List;

/** Resultado paginado de una consulta. */
public record Pagina<T>(List<T> items, long total, int pagina, int tamano) {

    public int totalPaginas() {
        return tamano <= 0 ? 1 : (int) Math.max(1, (total + tamano - 1) / tamano);
    }

    public boolean hayAnterior() {
        return pagina > 1;
    }

    public boolean haySiguiente() {
        return pagina < totalPaginas();
    }

    public long desde() {
        return total == 0 ? 0 : (long) (pagina - 1) * tamano + 1;
    }

    public long hasta() {
        return Math.min((long) pagina * tamano, total);
    }
}
