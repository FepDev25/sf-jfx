package ec.sfjfx.core.servicio;

import java.util.List;
import java.util.Map;

import ec.sfjfx.core.repositorio.Pagina;
import ec.sfjfx.core.modelo.Cliente;
import ec.sfjfx.core.modelo.ClienteContacto;
import ec.sfjfx.core.modelo.ClienteDireccion;
import ec.sfjfx.core.repositorio.ClienteDireccionRepositorio;
import ec.sfjfx.core.repositorio.ClienteRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;
import ec.sfjfx.core.util.Validaciones;

public final class ServicioCliente {

    public static final String ID_CONSUMIDOR_FINAL = "9999999999";

    private final ClienteRepositorio repo = new ClienteRepositorio();
    private final ClienteDireccionRepositorio direcciones = new ClienteDireccionRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<Cliente> listar(String filtro) {
        return repo.listar(empresa(), filtro);
    }

    public Pagina<Cliente> pagina(String filtro, int pagina, int tamano) {
        return repo.paginar(empresa(), filtro, pagina, tamano);
    }

    public Cliente porId(long id) {
        return repo.porId(id).orElseThrow(() -> new ValidacionExcepcion("Cliente no encontrado"));
    }

    public List<ClienteDireccion> direcciones(long clienteId) {
        return direcciones.porCliente(clienteId);
    }

    public long agregarDireccion(ClienteDireccion d) {
        Validaciones.requerido(d.direccion(), "Direccion");
        return direcciones.crear(d);
    }

    public void eliminarDireccion(long id) {
        direcciones.eliminar(id);
    }

    public List<ClienteContacto> contactos(long clienteId) {
        return direcciones.contactos(clienteId);
    }

    public long agregarContacto(ClienteContacto c) {
        Validaciones.requerido(c.nombre(), "Nombre del contacto");
        return direcciones.crearContacto(c);
    }

    public Cliente guardar(Cliente cl) {
        validar(cl);
        if (cl.id() == null) {
            long id = repo.crear(cl);
            auditoria.registrar("CLIENTES", "cliente", id, "CREAR", null, Map.of("nombre", cl.nombreParaMostrar()));
            return new Cliente(id, cl.empresaId(), cl.tipo(), cl.tipoIdentificacion(), cl.numeroIdentificacion(),
                    cl.nombres(), cl.apellidos(), cl.razonSocial(), cl.email(), cl.telefono(), cl.direccion(),
                    cl.esCredito(), cl.limiteCredito(), cl.diasCredito(), cl.saldo(), cl.activo());
        }
        repo.actualizar(cl);
        auditoria.registrar("CLIENTES", "cliente", cl.id(), "EDITAR", null, Map.of("nombre", cl.nombreParaMostrar()));
        return cl;
    }

    private void validar(Cliente cl) {
        if ("EMPRESA".equals(cl.tipo())) {
            Validaciones.requerido(cl.razonSocial(), "Razon social");
            if (!Validaciones.identificacionEcuador(cl.numeroIdentificacion())) {
                throw new ValidacionExcepcion("El RUC no es valido");
            }
        } else if ("PERSONA_NATURAL".equals(cl.tipo())) {
            Validaciones.requerido(cl.nombres(), "Nombres");
            if (!Validaciones.identificacionEcuador(cl.numeroIdentificacion())) {
                throw new ValidacionExcepcion("La cedula no es valida");
            }
        }
        if (!Validaciones.email(cl.email())) {
            throw new ValidacionExcepcion("El email no es valido");
        }
        if ("CONSUMIDOR_FINAL".equals(cl.tipo())) {
            return;
        }
        if (repo.identificacionExiste(empresa(), cl.numeroIdentificacion(), cl.id())) {
            throw new ValidacionExcepcion("Ya existe un cliente con esa identificacion");
        }
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }
}
