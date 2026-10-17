package ec.sfjfx.core.repositorio;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modulos.ModuloSistema;

public final class ModuloRepositorio {

    /** Codigos de modulo activos para una empresa. */
    public Set<String> activosPorEmpresa(long empresaId) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT modulo_codigo FROM empresa_modulo WHERE empresa_id = ? AND activo = TRUE")) {
                ps.setLong(1, empresaId);
                try (ResultSet rs = ps.executeQuery()) {
                    Set<String> codigos = new HashSet<>();
                    while (rs.next()) {
                        codigos.add(rs.getString(1));
                    }
                    return codigos;
                }
            }
        });
    }

    /** Estado de todos los modulos del sistema para una empresa. */
    public Map<String, Boolean> estado(long empresaId) {
        Map<String, Boolean> mapa = new LinkedHashMap<>();
        for (ModuloSistema m : ModuloSistema.values()) {
            mapa.put(m.codigo(), false);
        }
        Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT modulo_codigo, activo FROM empresa_modulo WHERE empresa_id = ?")) {
                ps.setLong(1, empresaId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        mapa.put(rs.getString("modulo_codigo"), rs.getBoolean("activo"));
                    }
                    return null;
                }
            }
        });
        return mapa;
    }

    public void ajustar(long empresaId, String moduloCodigo, boolean activo) {
        Bd.transaccion(c -> {
            String sql = "INSERT INTO empresa_modulo (empresa_id, modulo_codigo, activo) VALUES (?, ?, ?) "
                    + "ON CONFLICT (empresa_id, modulo_codigo) DO UPDATE SET activo = EXCLUDED.activo";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                ps.setString(2, moduloCodigo);
                ps.setBoolean(3, activo);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public void activar(long empresaId, String moduloCodigo) {
        ajustar(empresaId, moduloCodigo, true);
    }
}
