package org.ucb.cashin.prediccion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.ucb.cashin.model.GastoCategoriaMes
import org.ucb.cashin.model.TotalesMes
import java.math.BigDecimal
import java.time.YearMonth

class ReportesTest {

    private val nov = YearMonth.of(2026, 11)
    private val oct = YearMonth.of(2026, 10)
    private fun bd(texto: String) = BigDecimal(texto)

    private fun fila(mes: YearMonth, id: String, total: String, nombre: String = id) = GastoCategoriaMes(
        usuarioId = "u",
        mes = mes.atDay(1),
        categoriaId = id,
        nombre = nombre,
        color = "#2E7D32",
        total = bd(total),
    )

    private fun totales(mes: YearMonth, ingresos: String, gastos: String) = TotalesMes(
        usuarioId = "u",
        mes = mes.atDay(1),
        totalGastos = bd(gastos),
        totalIngresos = bd(ingresos),
        cantidadRegistros = 1,
    )

    /** Los gastos de ejemplo de la maqueta (noviembre contra octubre). */
    private val ejemplo = listOf(
        fila(nov, "vivienda", "900"), fila(oct, "vivienda", "900"),
        fila(nov, "comida", "640"), fila(oct, "comida", "542"),
        fila(nov, "transporte", "280"), fila(oct, "transporte", "310"),
        fila(nov, "salidas", "250"), fila(oct, "salidas", "190"),
        fila(nov, "salud", "150"), fila(oct, "salud", "150"),
        fila(nov, "otros", "130"), fila(oct, "otros", "120"),
    )

    // ---------- porcentajes ----------

    @Test
    fun `los porcentajes suman siempre 100`() {
        val casos = listOf(
            listOf("1", "1", "1"),
            listOf("900", "640", "280", "250", "150", "130"),
            listOf("0.01", "99.99"),
            listOf("10", "10", "10", "10", "10", "10", "10"),
            listOf("5"),
        )
        casos.forEach { montos ->
            assertEquals(montos.toString(), 100, porcentajesEnteros(montos.map(::bd)).sum())
        }
    }

    @Test
    fun `tres partes iguales reparten 34 33 33`() {
        assertEquals(listOf(34, 33, 33), porcentajesEnteros(listOf(bd("1"), bd("1"), bd("1"))))
    }

    @Test
    fun `sin montos los porcentajes son 0`() {
        assertEquals(listOf(0, 0), porcentajesEnteros(listOf(bd("0"), bd("0"))))
    }

    // ---------- reporte por categoría ----------

    @Test
    fun `el reporte ordena de mayor a menor y calcula totales`() {
        val r = reportePorCategoria(ejemplo, nov)
        assertEquals(listOf("vivienda", "comida", "transporte", "salidas", "salud", "otros"), r.porciones.map { it.categoriaId })
        assertEquals(bd("2350.00"), r.total)
        assertEquals(bd("2212.00"), r.totalAnterior)
        assertEquals(bd("138.00"), r.diferencia)
        assertEquals(Tendencia.SUBE, r.tendencia)
        assertEquals(listOf(38, 27, 12, 11, 6, 6), r.porciones.map { it.porcentaje })
    }

    @Test
    fun `cada categoria se compara con el mes anterior`() {
        val r = reportePorCategoria(ejemplo, nov).porciones.associateBy { it.categoriaId }
        assertEquals(Tendencia.IGUAL, r.getValue("vivienda").tendencia)
        assertEquals(0, r.getValue("vivienda").cambioPorcentaje)
        assertEquals(Tendencia.SUBE, r.getValue("comida").tendencia)
        assertEquals(18, r.getValue("comida").cambioPorcentaje)
        assertEquals(bd("98.00"), r.getValue("comida").diferencia)
        assertEquals(Tendencia.BAJA, r.getValue("transporte").tendencia)
        assertEquals(10, r.getValue("transporte").cambioPorcentaje)
        assertEquals(32, r.getValue("salidas").cambioPorcentaje)
    }

    @Test
    fun `una categoria sin mes anterior es nueva`() {
        val r = reportePorCategoria(listOf(fila(nov, "venta", "200"), fila(oct, "sueldo", "3000")), nov)
        val venta = r.porciones.single()
        assertEquals(Tendencia.NUEVA, venta.tendencia)
        assertNull(venta.cambioPorcentaje)
        assertEquals(bd("0.00"), venta.anterior)
        // la que no tuvo nada en noviembre no aparece, pero sí cuenta en el total anterior
        assertEquals(bd("3000.00"), r.totalAnterior)
    }

    @Test
    fun `un mes sin datos da un reporte vacio`() {
        val r = reportePorCategoria(ejemplo, YearMonth.of(2026, 12))
        assertTrue(r.vacio)
        assertEquals(bd("0.00"), r.total)
        assertEquals(bd("2350.00"), r.totalAnterior)
    }

    @Test
    fun `empate de montos se ordena por nombre`() {
        val r = reportePorCategoria(listOf(fila(nov, "b", "50", "Salud"), fila(nov, "a", "50", "Comida")), nov)
        assertEquals(listOf("Comida", "Salud"), r.porciones.map { it.nombre })
    }

    // ---------- últimos meses ----------

    @Test
    fun `ultimos meses rellena con 0 y quita los vacios del principio`() {
        val serie = ultimosMeses(
            listOf(totales(YearMonth.of(2026, 8), "3200", "2300"), totales(nov, "3200", "2350")),
            hasta = nov,
        )
        assertEquals(listOf(8, 9, 10, 11), serie.map { it.mes.monthValue })
        assertEquals(bd("0.00"), serie[1].ingresos)
        assertEquals(bd("850.00"), serie.last().sobrante)
    }

    @Test
    fun `ultimos meses no pasa de 6 ni ve el futuro`() {
        val todos = (0L..9L).map { totales(nov.minusMonths(it), "100", "50") } + totales(nov.plusMonths(1), "1", "1")
        val serie = ultimosMeses(todos, hasta = nov)
        assertEquals(6, serie.size)
        assertEquals(nov, serie.last().mes)
        assertEquals(nov.minusMonths(5), serie.first().mes)
    }

    @Test
    fun `sin datos la serie esta vacia`() {
        assertTrue(ultimosMeses(emptyList(), nov).isEmpty())
    }

    // ---------- fijos, variables y recortable ----------

    @Test
    fun `la division del mes suma el total`() {
        val reporte = reportePorCategoria(ejemplo, nov)
        val desglose = DesgloseGastos(
            listOf(DesgloseCategoria("vivienda", bd("1250.00"), bd("0"), 4, bd("0"))),
        )
        val importancias = mapOf("vivienda" to 4, "comida" to 3, "salidas" to 1)
        val d = dividirGastoDelMes(reporte, desglose, importancias)
        assertEquals(reporte.total, d.total)
        // el fijo no pasa de lo que se gastó en la categoría
        assertEquals(bd("900.00"), d.fijo)
        // salidas (importancia 1): 50 % de 250 = 125 recortable
        assertTrue(d.recortable >= bd("125.00"))
    }

    @Test
    fun `sin desglose todo es variable o recortable`() {
        val reporte = reportePorCategoria(listOf(fila(nov, "salidas", "100")), nov)
        val d = dividirGastoDelMes(reporte, null, mapOf("salidas" to 5))
        assertEquals(bd("0.00"), d.fijo)
        assertEquals(bd("100.00"), d.variable)
        assertEquals(bd("0.00"), d.recortable)
    }

    // ---------- frases de Cashi ----------

    @Test
    fun `gastos frase de la categoria que mas subio`() {
        val f = fraseGastos(reportePorCategoria(ejemplo, nov))
        assertEquals(TipoFraseReporte.CATEGORIA_SUBIO, f.tipo)
        assertEquals("comida", f.nombre)
        assertEquals(bd("98.00"), f.monto)
    }

    @Test
    fun `gastos que suben poco no cuentan como subida`() {
        // otros sube 8 %: por debajo del mínimo; el mes baja en total
        val f = fraseGastos(
            reportePorCategoria(
                listOf(fila(nov, "otros", "130"), fila(oct, "otros", "120"), fila(oct, "comida", "200")),
                nov,
            ),
        )
        assertEquals(TipoFraseReporte.GASTO_BAJO, f.tipo)
        assertEquals(bd("190.00"), f.monto)
    }

    @Test
    fun `si el mes bajo se celebra aunque una categoria sea nueva`() {
        // octubre: comida 2779; noviembre: solo salidas 1505 (nueva) -> el mes bajó 1274
        val f = fraseGastos(
            reportePorCategoria(listOf(fila(oct, "comida", "2779"), fila(nov, "salidas", "1505")), nov),
        )
        assertEquals(TipoFraseReporte.GASTO_BAJO, f.tipo)
        assertEquals(bd("1274.00"), f.monto)
    }

    @Test
    fun `gastos sin mes anterior van parecido`() {
        val f = fraseGastos(reportePorCategoria(listOf(fila(nov, "comida", "100")), nov))
        assertEquals(TipoFraseReporte.GASTO_PARECIDO, f.tipo)
    }

    @Test
    fun `gastos vacio no tiene datos`() {
        assertEquals(TipoFraseReporte.SIN_DATOS, fraseGastos(reportePorCategoria(emptyList(), nov)).tipo)
    }

    @Test
    fun `ingresos frase de la categoria extra con lo que sobra`() {
        val r = reportePorCategoria(
            listOf(fila(nov, "sueldo", "3000"), fila(oct, "sueldo", "3000"), fila(nov, "venta", "200")),
            nov,
        )
        val f = fraseIngresos(r, sobrante = bd("850"))
        assertEquals(TipoFraseReporte.INGRESO_EXTRA, f.tipo)
        assertEquals("venta", f.nombre)
        assertEquals(bd("200.00"), f.monto)
        assertEquals(bd("850.00"), f.sobrante)
    }

    @Test
    fun `ingresos parecidos sin sobrante negativo`() {
        val r = reportePorCategoria(listOf(fila(nov, "sueldo", "3000"), fila(oct, "sueldo", "3000")), nov)
        val f = fraseIngresos(r, sobrante = bd("-20"))
        assertEquals(TipoFraseReporte.INGRESOS_PARECIDOS, f.tipo)
        assertNull(f.monto)
    }

    private val serieEjemplo = listOf(
        MesResumen(YearMonth.of(2026, 6), bd("3000"), bd("2650")),
        MesResumen(YearMonth.of(2026, 7), bd("3000"), bd("2820")),
        MesResumen(YearMonth.of(2026, 8), bd("3200"), bd("2300")),
        MesResumen(YearMonth.of(2026, 9), bd("3000"), bd("2850")),
        MesResumen(YearMonth.of(2026, 10), bd("3000"), bd("2212")),
        MesResumen(nov, bd("3200"), bd("2350")),
    )

    @Test
    fun `meses el mejor desde agosto`() {
        val f = fraseMeses(serieEjemplo)
        assertEquals(TipoFraseReporte.MEJOR_MES, f.tipo)
        assertEquals(YearMonth.of(2026, 8), f.mesReferencia)
        assertEquals(bd("850.00"), f.monto)
    }

    @Test
    fun `meses el mejor de toda la serie`() {
        val f = fraseMeses(serieEjemplo.dropLast(1) + MesResumen(nov, bd("4000"), bd("2000")))
        assertEquals(TipoFraseReporte.MEJOR_MES, f.tipo)
        assertNull(f.mesReferencia)
    }

    @Test
    fun `meses peor que el anterior solo dice cuanto sobra`() {
        val f = fraseMeses(serieEjemplo.dropLast(1) + MesResumen(nov, bd("3000"), bd("2900")))
        assertEquals(TipoFraseReporte.SOBRA, f.tipo)
        assertEquals(bd("100.00"), f.monto)
    }

    @Test
    fun `meses en rojo`() {
        val f = fraseMeses(listOf(MesResumen(nov, bd("1000"), bd("1200"))))
        assertEquals(TipoFraseReporte.NO_SOBRA, f.tipo)
        assertEquals(bd("200.00"), f.monto)
    }

    @Test
    fun `meses sin datos`() {
        assertEquals(TipoFraseReporte.SIN_DATOS, fraseMeses(emptyList()).tipo)
    }
}
