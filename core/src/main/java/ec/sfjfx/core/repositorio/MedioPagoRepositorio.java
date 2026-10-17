package ec.sfjfx.core.repositorio;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ec.sfjfx.core.db.Bd;

/** Consultas simples sobre medios de pago (catalogo global). */
public final class MedioPagoRepositorio {

    public record MedioPago(Long id, String codigo, String nombre, String tipo) {
        @Override
        public String toString() {
            return nombre;
        }
    }

    public List<MedioPago> listar() {
        return Bd.consulta(c -> {
            String sql = "SELECT id, codigo, nombre, tipo FROM medio_pago WHERE activo = TRUE ORDER BY nombre";
            try (PreparedStatement ps = c.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                List<MedioPago> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(new MedioPago(rs.getLong("id"), rs.getString("codigo"), rs.getString("nombre"),
                            rs.getString("tipo")));
                }
                return lista;
            }
        });
    }
}
