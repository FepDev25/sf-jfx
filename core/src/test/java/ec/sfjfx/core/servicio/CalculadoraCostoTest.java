package ec.sfjfx.core.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class CalculadoraCostoTest {

    @Test
    void promedioPonderadoPonderaPorCantidad() {
        BigDecimal promedio = CalculadoraCosto.promedioPonderado(
                new BigDecimal("10"), new BigDecimal("2.00"),
                new BigDecimal("10"), new BigDecimal("3.00"));
        assertEquals(0, promedio.compareTo(new BigDecimal("2.5000")));
    }

    @Test
    void primeraEntradaTomaElCostoRecibido() {
        BigDecimal promedio = CalculadoraCosto.promedioPonderado(
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("5"), new BigDecimal("1.75"));
        assertEquals(0, promedio.compareTo(new BigDecimal("1.7500")));
    }

    @Test
    void entradaSinCostoMantieneElPromedio() {
        BigDecimal promedio = CalculadoraCosto.promedioPonderado(
                new BigDecimal("4"), new BigDecimal("2.25"),
                new BigDecimal("6"), new BigDecimal("2.25"));
        assertEquals(0, promedio.compareTo(new BigDecimal("2.2500")));
    }

    @Test
    void redondeaACuatroDecimales() {
        BigDecimal promedio = CalculadoraCosto.promedioPonderado(
                new BigDecimal("3"), new BigDecimal("1.11"),
                new BigDecimal("7"), new BigDecimal("1.13"));
        assertEquals(0, promedio.compareTo(new BigDecimal("1.1240")));
    }
}
