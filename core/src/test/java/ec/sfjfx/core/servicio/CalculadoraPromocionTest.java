package ec.sfjfx.core.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import ec.sfjfx.core.modelo.Promocion;

class CalculadoraPromocionTest {

    private Promocion promocion(String tipo, String valor, Integer requerida, Integer pagada) {
        return new Promocion(null, 1, "Prueba", null, tipo, new BigDecimal(valor), requerida, pagada,
                0, false, null, null, "ACTIVA", true, List.of(), List.of());
    }

    @Test
    void porcentajeSobreElTotal() {
        BigDecimal descuento = CalculadoraPromocion.descuento(
                promocion("PORCENTAJE", "10", null, null), new BigDecimal("2"), new BigDecimal("10.00"));
        assertEquals(0, descuento.compareTo(new BigDecimal("2.00")));
    }

    @Test
    void montoFijoNoSuperaLaBase() {
        BigDecimal descuento = CalculadoraPromocion.descuento(
                promocion("MONTO", "15", null, null), BigDecimal.ONE, new BigDecimal("10.00"));
        assertEquals(0, descuento.compareTo(new BigDecimal("10.00")));
    }

    @Test
    void precioFijoDescuentaLaDiferenciaPorCantidad() {
        BigDecimal descuento = CalculadoraPromocion.descuento(
                promocion("PRECIO_FIJO", "8", null, null), new BigDecimal("3"), new BigDecimal("10.00"));
        assertEquals(0, descuento.compareTo(new BigDecimal("6.00")));
    }

    @Test
    void precioFijoMayorAlPrecioNoGeneraDescuento() {
        BigDecimal descuento = CalculadoraPromocion.descuento(
                promocion("PRECIO_FIJO", "12", null, null), BigDecimal.ONE, new BigDecimal("10.00"));
        assertEquals(0, descuento.compareTo(BigDecimal.ZERO));
    }

    @Test
    void nxmRegalaSegunMultiplosCompletos() {
        BigDecimal descuento = CalculadoraPromocion.descuento(
                promocion("NXM", "0", 2, 1), new BigDecimal("3"), new BigDecimal("10.00"));
        assertEquals(0, descuento.compareTo(new BigDecimal("10.00")));
        BigDecimal dosPares = CalculadoraPromocion.descuento(
                promocion("NXM", "0", 2, 1), new BigDecimal("4"), new BigDecimal("10.00"));
        assertEquals(0, dosPares.compareTo(new BigDecimal("20.00")));
    }

    @Test
    void cantidadInvalidaNoGeneraDescuento() {
        assertEquals(0, CalculadoraPromocion.descuento(
                promocion("PORCENTAJE", "10", null, null), BigDecimal.ZERO, new BigDecimal("10.00"))
                .compareTo(BigDecimal.ZERO));
    }
}
