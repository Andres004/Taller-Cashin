package org.ucb.cashin.prediccion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.ucb.cashin.model.TotalesMes
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class SerieMensualTest {

    private val hoy = LocalDate.of(2026, 10, 15)

    private fun fila(mes: YearMonth, gastos: String, ingresos: String = "0") = TotalesMes(
        usuarioId = "u",
        mes = mes.atDay(1),
        totalGastos = BigDecimal(gastos),
        totalIngresos = BigDecimal(ingresos),
        cantidadRegistros = 1,
    )

    @Test fun `sin datos devuelve una serie vacia`() {
        assertTrue(construirSerie(emptyList(), hoy) { it.totalGastos }.isEmpty())
    }

    @Test fun `excluye el mes actual porque todavia no termina`() {
        val serie = construirSerie(
            listOf(fila(YearMonth.of(2026, 9), "100"), fila(YearMonth.of(2026, 10), "999")),
            hoy,
        ) { it.totalGastos }

        assertEquals(listOf(PuntoMensual(YearMonth.of(2026, 9), 100.0)), serie)
    }

    @Test fun `rellena con cero los meses sin anotaciones`() {
        val serie = construirSerie(
            listOf(fila(YearMonth.of(2026, 6), "300"), fila(YearMonth.of(2026, 8), "500")),
            hoy,
        ) { it.totalGastos }

        assertEquals(
            listOf(
                PuntoMensual(YearMonth.of(2026, 6), 300.0),
                PuntoMensual(YearMonth.of(2026, 7), 0.0),
                PuntoMensual(YearMonth.of(2026, 8), 500.0),
                PuntoMensual(YearMonth.of(2026, 9), 0.0),
            ),
            serie,
        )
    }

    @Test fun `ordena del mes mas antiguo al mas reciente`() {
        val serie = construirSerie(
            listOf(fila(YearMonth.of(2026, 9), "90"), fila(YearMonth.of(2026, 7), "70"), fila(YearMonth.of(2026, 8), "80")),
            hoy,
        ) { it.totalGastos }

        assertEquals(listOf(70.0, 80.0, 90.0), serie.map { it.valor })
    }

    @Test fun `usa el campo pedido`() {
        val serie = construirSerie(listOf(fila(YearMonth.of(2026, 9), "100", "3200")), hoy) { it.totalIngresos }

        assertEquals(3200.0, serie.single().valor, 0.001)
    }

    @Test fun `devuelve como maximo los ultimos doce meses`() {
        val filas = (1..20).map { fila(YearMonth.of(2026, 10).minusMonths(it.toLong()), "$it") }
        val serie = construirSerie(filas, hoy) { it.totalGastos }

        assertEquals(MAX_MESES_SERIE, serie.size)
        assertEquals(YearMonth.of(2026, 9), serie.last().mes)
    }
}