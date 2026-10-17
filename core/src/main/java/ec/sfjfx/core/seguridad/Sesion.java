package ec.sfjfx.core.seguridad;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import ec.sfjfx.core.modelo.Caja;
import ec.sfjfx.core.modelo.Empresa;
import ec.sfjfx.core.modelo.Sucursal;
import ec.sfjfx.core.modelo.Usuario;

/**
 * Sesion activa: usuario autenticado, empresa, sucursal/caja seleccionadas y permisos.
 */
public final class Sesion {

    private final Usuario usuario;
    private final Empresa empresa;
    private final Set<String> permisos;
    private Sucursal sucursal;
    private Caja caja;

    public Sesion(Usuario usuario, Empresa empresa, Set<String> permisos) {
        this.usuario = usuario;
        this.empresa = empresa;
        this.permisos = new HashSet<>(permisos);
    }

    public Usuario usuario() {
        return usuario;
    }

    public Empresa empresa() {
        return empresa;
    }

    public Sucursal sucursal() {
        return sucursal;
    }

    public void seleccionarSucursal(Sucursal sucursal) {
        this.sucursal = sucursal;
    }

    public Caja caja() {
        return caja;
    }

    public void seleccionarCaja(Caja caja) {
        this.caja = caja;
    }

    public Set<String> permisos() {
        return Collections.unmodifiableSet(permisos);
    }

    public boolean esSuperadmin() {
        return usuario.esSuperadmin();
    }

    public boolean tienePermiso(String codigo) {
        return esSuperadmin() || permisos.contains(codigo);
    }
}
