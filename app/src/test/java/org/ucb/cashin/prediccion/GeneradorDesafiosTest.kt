package org.ucb.cashin.prediccion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.ucb.cashin.model.GastoCategoriaMes
import org.ucb.cashin.model.PeriodoDesafio
import org.ucb.cashin.model.ReglaDesafio
import org.ucb.cashin.model.TotalesMes
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class GeneradorDesafiosTest {

    private val hoy = LocalDate.of(2026, 11, 15)
    private val agosto = YearMonth.of(2026, 8)
    private val septiembre = YearMonth.of(2026, 9)
    private val octubre = YearMonth.of(2026, 10)
    private val noviembre = YearMonth.of(2026, 11)

    private fun bd(texto: String) = BigDecimal(texto)

    private fun gasto(mes: YearMonth, categoriaId: String, nombre: String, total: String) = GastoCategoriaMes(
        usuarioId = "u1",
        mes = mes.atDay(1),
        categoriaId = categoriaId,
        nombre = nombre,
        total = bd(total),
    )

    private fun totales(mes: YearMonth, gastos: String) = TotalesMes(
        usuarioId = "u1",
        mes = mes.atDay(1),
        totalGastos = bd(gastos),
        totalIngresos = bd("3000"),
        cantidadRegistros = 10,
    )

    /** 3 meses completos: Alimentación 900 y Transporte 150 cada mes (total 1050). */
    private val gastos3Meses = listOf(agosto, septiembre, octubre).flatMap { mes ->
        listOf(gasto(mes, "c-ali", "Alimentación", "900"), gasto(mes, "c-tra", "Transporte", "150"))
    }
    private val historial3Meses = listOf(agosto, septiembre, octubre).map { totales(it, "1050") }

    @Test fun `sin historia solo propone dias de registro`() {
        val semanal = proponerDesafios(PeriodoDesafio.SEMANAL, emptyList(), emptyList(), hoy)
        assertEquals(1, semanal.size)
        assertEquals(ReglaDesafio.DIAS_REGISTRO, semanal[0].regla)
        assertEquals(5, semanal[0].diasObjetivo)

        val mensual = proponerDesafios(PeriodoDesafio.MENSUAL, emptyList(), emptyList(), hoy)
        assertEquals(20, mensual.single().diasObjetivo)
    }

    @Test fun `solo el mes actual no cuenta como historia`() {
        val propuestas = proponerDesafios(
            PeriodoDesafio.MENSUAL,
            listOf(gasto(noviembre, "c-ali", "Alimentación", "500")),
            listOf(totales(noviembre, "500")),
            hoy,
        )
        assertEquals(listOf(ReglaDesafio.DIAS_REGISTRO), propuestas.map { it.regla })
    }

    @Test fun `con 3 meses propone las 3 reglas en orden`() {
        val propuestas = proponerDesafios(PeriodoDesafio.MENSUAL, gastos3Meses, historial3Meses, hoy)
        assertEquals(
            listOf(ReglaDesafio.LIMITE_CATEGORIA, ReglaDesafio.LIMITE_TOTAL, ReglaDesafio.DIAS_REGISTRO),
            propuestas.map { it.regla },
        )
        assertTrue(propuestas.all { it.periodo == PeriodoDesafio.MENSUAL })
    }

    @Test fun `limite de categoria usa la de mayor promedio con 15 por ciento menos`() {
        val categoria = proponerDesafios(PeriodoDesafio.MENSUAL, gastos3Meses, historial3Meses, hoy)[0]
        assertEquals("c-ali", categoria.categoriaId)
        assertEquals("Alimentación", categoria.categoriaNombre)
        assertEquals(bd("900.00"), categoria.basadoEn)
        assertEquals(bd("765.00"), categoria.montoLimite)        // 900 × 0,85
    }

    @Test fun `limite total usa el pronostico con 10 por ciento menos`() {
        val total = proponerDesafios(PeriodoDesafio.MENSUAL, gastos3Meses, historial3Meses, hoy)[1]
        assertEquals(bd("1050.00"), total.basadoEn)
        assertEquals(bd("945.00"), total.montoLimite)             // 1050 × 0,90
    }

    @Test fun `semanal aplica el factor 7 sobre 30,44`() {
        val propuestas = proponerDesafios(PeriodoDesafio.SEMANAL, gastos3Meses, historial3Meses, hoy)
        assertEquals(bd("175.00"), propuestas[0].montoLimite)     // 900 × 7/30,44 × 0,85 = 175,92
        assertEquals(bd("215.00"), propuestas[1].montoLimite)     // 1050 × 7/30,44 × 0,90 = 217,31
        assertEquals(5, propuestas[2].diasObjetivo)
    }

    @Test fun `redondea hacia abajo a multiplos de 5`() {
        val gastos = listOf(gasto(octubre, "c-ali", "Alimentación", "103"))
        val propuesta = proponerDesafios(PeriodoDesafio.MENSUAL, gastos, emptyList(), hoy)[0]
        assertEquals(bd("85.00"), propuesta.montoLimite)          // 103 × 0,85 = 87,55 → 85
    }

    @Test fun `promedia entre los meses de la ventana aunque la categoria no tenga gasto en alguno`() {
        val gastos = listOf(
            gasto(agosto, "c-ali", "Alimentación", "300"),
            gasto(octubre, "c-ali", "Alimentación", "300"),
            gasto(septiembre, "c-tra", "Transporte", "30"),
        )
        val propuesta = proponerDesafios(PeriodoDesafio.MENSUAL, gastos, emptyList(), hoy)[0]
        assertEquals(bd("200.00"), propuesta.basadoEn)             // (300 + 0 + 300) / 3
    }

    @Test fun `nunca propone montos menores o iguales a cero`() {
        val gastos = listOf(gasto(octubre, "c-ali", "Alimentación", "4"))
        val historial = listOf(totales(octubre, "4"))
        val propuestas = proponerDesafios(PeriodoDesafio.SEMANAL, gastos, historial, hoy)
        assertTrue(propuestas.mapNotNull { it.montoLimite }.all { it > BigDecimal.ZERO })
        assertEquals(bd("5.00"), propuestas[0].montoLimite)       // mínimo Bs 5
    }

    @Test fun `sin gastos en los meses completos no propone limites`() {
        val historial = listOf(totales(septiembre, "0"), totales(octubre, "0"))
        val propuestas = proponerDesafios(PeriodoDesafio.MENSUAL, emptyList(), historial, hoy)
        assertEquals(listOf(ReglaDesafio.DIAS_REGISTRO), propuestas.map { it.regla })
    }
}
