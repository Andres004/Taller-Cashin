package org.ucb.cashin.prediccion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.ucb.cashin.model.Desafio
import org.ucb.cashin.model.Frecuencia
import org.ucb.cashin.model.GastoCategoriaMes
import org.ucb.cashin.model.PeriodoDesafio
import org.ucb.cashin.model.RegistroPlanificado
import org.ucb.cashin.model.ReglaDesafio
import org.ucb.cashin.model.TipoRegistro
import org.ucb.cashin.model.TotalesMes
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class GeneradorDesafiosTest {

    /** Lunes: a la semana le quedan 7 días y al mes 22. */
    private val lunes = LocalDate.of(2026, 11, 9)
    /** Domingo: a la semana le queda solo hoy. */
    private val domingo = LocalDate.of(2026, 11, 15)
    private val meses = listOf(YearMonth.of(2026, 8), YearMonth.of(2026, 9), YearMonth.of(2026, 10))

    private fun bd(texto: String) = BigDecimal(texto)

    private fun gasto(mes: YearMonth, categoriaId: String, nombre: String, total: String) = GastoCategoriaMes(
        usuarioId = "u1", mes = mes.atDay(1), categoriaId = categoriaId, nombre = nombre, total = bd(total),
    )

    private fun totales(mes: YearMonth, gastos: String) = TotalesMes(
        usuarioId = "u1", mes = mes.atDay(1), totalGastos = bd(gastos), totalIngresos = bd("3000"), cantidadRegistros = 30,
    )

    /**
     * 3 meses completos: Alimentación 900 y Transporte 300 cada mes (regulares),
     * Entretenimiento 400 solo en octubre (ocasional). Total 1.200 al mes.
     */
    private val historia = HistoriaDesafios(
        gastoPorCategoria = meses.flatMap { mes ->
            listOf(gasto(mes, "c-ali", "Alimentación", "900"), gasto(mes, "c-tra", "Transporte", "300"))
        } + gasto(YearMonth.of(2026, 10), "c-ent", "Entretenimiento", "400"),
        totales = meses.map { totales(it, "1200") },
        tieneMetasActivas = true,
    )
    private val sinHistoria = HistoriaDesafios(emptyList(), emptyList(), tieneMetasActivas = false)
    private val nada = MedicionPeriodo()

    private fun armar(plantilla: PlantillaDesafio, hoy: LocalDate = lunes, medicion: MedicionPeriodo = nada, h: HistoriaDesafios = historia) =
        armarPropuesta(plantilla, hoy, h, medicion)

    // Catálogo

    @Test fun `el catalogo tiene 12 semanales y 5 mensuales`() {
        assertEquals(12, PlantillaDesafio.entries.count { it.periodo == PeriodoDesafio.SEMANAL })
        assertEquals(5, PlantillaDesafio.entries.count { it.periodo == PeriodoDesafio.MENSUAL })
    }

    // Sorteo

    @Test fun `cada semana salen 3 semanales y cada mes 1 mensual`() {
        assertEquals(3, sortearDesafios(PeriodoDesafio.SEMANAL, 1L, lunes, historia, nada).size)
        assertEquals(1, sortearDesafios(PeriodoDesafio.MENSUAL, 1L, lunes, historia, nada).size)
    }

    @Test fun `la misma cuenta en la misma semana siempre sortea lo mismo`() {
        val semilla = semillaDe("dabner", PeriodoDesafio.SEMANAL, lunes)
        val primera = sortearDesafios(PeriodoDesafio.SEMANAL, semilla, lunes, historia, nada)
        repeat(5) { assertEquals(primera, sortearDesafios(PeriodoDesafio.SEMANAL, semilla, lunes, historia, nada)) }
    }

    @Test fun `cuentas y semanas distintas sortean distinto`() {
        val semanas = (0L until 20L).map { lunes.plusWeeks(it) }
        val porCuenta = listOf("ana", "beto", "carla").map { cuenta ->
            semanas.map { sortearDesafios(PeriodoDesafio.SEMANAL, semillaDe(cuenta, PeriodoDesafio.SEMANAL, it), lunes, historia, nada) }
        }
        assertTrue(porCuenta[0] != porCuenta[1] || porCuenta[1] != porCuenta[2])
        assertTrue(porCuenta[0].map { it.map { p -> p.plantilla } }.distinct().size > 1)   // cambia de semana a semana
    }

    @Test fun `nunca repite el mismo reto dentro de la semana`() {
        (0L until 200L).forEach { semilla ->
            val elegidos = sortearDesafios(PeriodoDesafio.SEMANAL, semilla, lunes, historia, nada)
            assertEquals(elegidos.size, elegidos.map { it.regla to it.categoriaId }.distinct().size)
        }
    }

    @Test fun `no repite en la semana el reto que ya esta activo en el mes`() {
        val mensual = Desafio(
            id = "m", usuarioId = "u1", titulo = "x", periodo = PeriodoDesafio.MENSUAL,
            regla = ReglaDesafio.LIMITE_CATEGORIA, categoriaId = "c-ali", montoLimite = bd("765"),
            fechaInicio = lunes.withDayOfMonth(1), fechaFin = lunes.withDayOfMonth(30),
        )
        (0L until 200L).forEach { semilla ->
            val semanales = sortearDesafios(PeriodoDesafio.SEMANAL, semilla, lunes, historia, nada, evitar = listOf(mensual))
            assertTrue(semanales.none { it.regla == ReglaDesafio.LIMITE_CATEGORIA && it.categoriaId == "c-ali" })
        }
    }

    @Test fun `sin historia igual salen 3 semanales que no necesitan historia`() {
        val elegidos = sortearDesafios(PeriodoDesafio.SEMANAL, 7L, lunes, sinHistoria, nada)
        assertEquals(3, elegidos.size)
        assertTrue(elegidos.none { it.regla == ReglaDesafio.LIMITE_CATEGORIA || it.regla == ReglaDesafio.LIMITE_TOTAL })
        assertTrue(elegidos.none { it.regla == ReglaDesafio.AHORRO_META })   // no tiene metas
    }

    // Límites realistas

    @Test fun `limite de categoria con 15 por ciento menos de lo de costumbre`() {
        val ali = armar(PlantillaDesafio.S_CATEGORIA_1)!!
        assertEquals("c-ali", ali.categoriaId)
        assertEquals(bd("175.00"), ali.montoLimite)                  // 900 × 7/30,44 = 206,96 → × 0,85 = 175,92
        assertEquals(bd("765.00"), armar(PlantillaDesafio.M_CATEGORIA_1)!!.montoLimite)
    }

    @Test fun `la segunda categoria regular es transporte y respeta el minimo de 50`() {
        val transporte = armar(PlantillaDesafio.S_CATEGORIA_2)!!
        assertEquals("c-tra", transporte.categoriaId)
        assertEquals(bd("55.00"), transporte.montoLimite)              // 300 × 7/30,44 = 68,99 → × 0,85 = 58,6 → 55
    }

    @Test fun `una categoria ocasional no se propone`() {
        assertNull(armar(PlantillaDesafio.S_CATEGORIA_3))                // Entretenimiento solo en 1 de 3 meses
    }

    @Test fun `si suele gastar menos del minimo en la categoria no se propone`() {
        val poco = HistoriaDesafios(meses.map { gasto(it, "c-ali", "Alimentación", "15") }, emptyList(), false)
        assertNull(armar(PlantillaDesafio.S_CATEGORIA_1, h = poco))     // ~Bs 3,45 a la semana
        assertNull(armar(PlantillaDesafio.M_CATEGORIA_1, h = poco))
    }

    @Test fun `el limite nunca baja del minimo realista`() {
        val justo = HistoriaDesafios(meses.map { gasto(it, "c-ali", "Alimentación", "240") }, emptyList(), false)
        assertEquals(bd("50.00"), armar(PlantillaDesafio.S_CATEGORIA_1, h = justo)!!.montoLimite)   // 46,9 → 50
        (0L until 100L).flatMap { sortearDesafios(PeriodoDesafio.SEMANAL, it, lunes, historia, nada) }
            .mapNotNull { it.montoLimite }
            .forEach { assertTrue(it >= bd("50.00")) }
    }

    @Test fun `limite total semanal y su version dificil`() {
        assertEquals(bd("245.00"), armar(PlantillaDesafio.S_TOTAL)!!.montoLimite)        // 275,95 × 0,90 = 248,4
        assertEquals(bd("220.00"), armar(PlantillaDesafio.S_TOTAL_DURO)!!.montoLimite)   // 275,95 × 0,80 = 220,8
        assertEquals(bd("1080.00"), armar(PlantillaDesafio.M_TOTAL)!!.montoLimite)
    }

    // Que no empiece perdido ni ganado

    @Test fun `no propone un limite que en el periodo ya se gasto en 80 por ciento`() {
        val gastoAlto = MedicionPeriodo(gastoPorCategoria = mapOf("c-ali" to bd("140")), gastoTotal = bd("140"))
        assertNull(armar(PlantillaDesafio.S_CATEGORIA_1, medicion = gastoAlto))                   // 140 = 80 % de 175
        assertTrue(armar(PlantillaDesafio.S_TOTAL, medicion = gastoAlto) != null)                  // 140 < 80 % de 245
    }

    @Test fun `no propone dias que ya no alcanzan ni dias ya logrados`() {
        assertNull(armar(PlantillaDesafio.S_DIAS_5, hoy = domingo, medicion = MedicionPeriodo(diasConRegistro = 2)))
        assertTrue(armar(PlantillaDesafio.S_DIAS_5, hoy = domingo, medicion = MedicionPeriodo(diasConRegistro = 4)) != null)
        assertNull(armar(PlantillaDesafio.S_DIAS_3, medicion = MedicionPeriodo(diasConRegistro = 3)))
    }

    @Test fun `dias sin gasto cuenta hoy como posible`() {
        assertTrue(armar(PlantillaDesafio.S_SIN_GASTO_2, hoy = domingo, medicion = MedicionPeriodo(diasSinGasto = 1)) != null)
        assertNull(armar(PlantillaDesafio.S_SIN_GASTO_2, hoy = domingo, medicion = MedicionPeriodo(diasSinGasto = 0)))
    }

    @Test fun `ahorrar en una meta solo si tiene metas activas`() {
        assertTrue(armar(PlantillaDesafio.S_AHORRO_1) != null)
        assertNull(armar(PlantillaDesafio.S_AHORRO_1, h = historia.copy(tieneMetasActivas = false)))
        assertNull(armar(PlantillaDesafio.S_AHORRO_1, medicion = MedicionPeriodo(ahorros = 1)))
    }

    @Test fun `el periodo va de lunes a domingo o del 1 al ultimo dia`() {
        assertEquals(LocalDate.of(2026, 11, 9), inicioDelPeriodo(PeriodoDesafio.SEMANAL, domingo))
        assertEquals(LocalDate.of(2026, 11, 15), finDelPeriodo(PeriodoDesafio.SEMANAL, lunes))
        assertEquals(LocalDate.of(2026, 11, 1), inicioDelPeriodo(PeriodoDesafio.MENSUAL, lunes))
        assertEquals(LocalDate.of(2026, 11, 30), finDelPeriodo(PeriodoDesafio.MENSUAL, lunes))
    }

    // Pagos fijos (Fase 4)

    private fun planificado(categoriaId: String, monto: String) = RegistroPlanificado(
        id = categoriaId + monto,
        usuarioId = "u1",
        categoriaId = categoriaId,
        tipo = TipoRegistro.GASTO,
        nombre = "Pago",
        monto = bd(monto),
        frecuencia = Frecuencia.MENSUAL,
        proximaFecha = lunes,
    )

    /** Alquiler 900 y WiFi 350 (planificados), comida 800 y salidas 300 cada mes. */
    private fun historiaConFijos(
        planificados: List<RegistroPlanificado> = listOf(planificado("c-viv", "900"), planificado("c-viv", "350")),
        importancias: Map<String, Int> = mapOf("c-viv" to 4, "c-com" to 3, "c-sal" to 1),
    ): HistoriaDesafios {
        val gastos = meses.flatMap { mes ->
            listOf(
                gasto(mes, "c-viv", "Vivienda", "1250"),
                gasto(mes, "c-com", "Alimentación", "800"),
                gasto(mes, "c-sal", "Salidas", "300"),
            )
        }
        return HistoriaDesafios(
            gastoPorCategoria = gastos,
            totales = meses.map { totales(it, "2350") },
            tieneMetasActivas = true,
            desglose = desglosarGastos(planificados, gastos, importancias, lunes),
        )
    }

    @Test fun `el limite total se calcula sobre lo variable y no sobre los pagos fijos`() {
        val conFijos = armar(PlantillaDesafio.S_TOTAL, h = historiaConFijos())
        val reglaAnterior = armar(PlantillaDesafio.S_TOTAL, h = historiaConFijos().copy(desglose = null))

        assertEquals(bd("225.00"), conFijos?.montoLimite)
        assertEquals(bd("485.00"), reglaAnterior?.montoLimite)
    }

    @Test fun `no se proponen limites en categorias de importancia 4 o 5`() {
        val h = historiaConFijos()

        assertEquals("c-com", armar(PlantillaDesafio.S_CATEGORIA_1, h = h)?.categoriaId)
        assertEquals("c-sal", armar(PlantillaDesafio.S_CATEGORIA_2, h = h)?.categoriaId)
        assertNull(armar(PlantillaDesafio.S_CATEGORIA_3, h = h))
    }

    @Test fun `el limite de categoria usa solo su parte variable`() {
        val h = historiaConFijos(planificados = listOf(planificado("c-com", "200")))

        assertEquals(bd("115.00"), armar(PlantillaDesafio.S_CATEGORIA_1, h = h)?.montoLimite)
    }

    @Test fun `si todo es importante no hay retos de limite por categoria`() {
        val h = historiaConFijos(importancias = mapOf("c-viv" to 5, "c-com" to 4, "c-sal" to 4))

        assertNull(armar(PlantillaDesafio.S_CATEGORIA_1, h = h))
    }

    @Test fun `el WiFi de Bs 350 no cuenta y el reto de Bs 225 sigue vivo`() {
        val excluidas = setOf("c-viv")
        val pagos = listOf(
            Triple("plan-wifi", "c-viv", "350"),
            Triple("plan-alquiler", "c-viv", "900"),
            Triple(null, "c-com", "120"),
        )
        val medido = pagos
            .filter { (planificado, categoria, _) -> cuentaParaLimites(planificado, categoria, excluidas) }
            .fold(BigDecimal.ZERO) { suma, (_, _, monto) -> suma + bd(monto) }

        assertEquals(bd("120"), medido)
        val propuesta = armar(PlantillaDesafio.S_TOTAL, medicion = MedicionPeriodo(gastoTotal = medido), h = historiaConFijos())
        assertEquals(bd("225.00"), propuesta?.montoLimite)
    }

    @Test fun `un pago planificado no cuenta aunque su categoria sea recortable`() {
        assertFalse(cuentaParaLimites("plan", "c-sal", emptySet()))
        assertFalse(cuentaParaLimites(null, "c-viv", setOf("c-viv")))
        assertTrue(cuentaParaLimites(null, "c-sal", setOf("c-viv")))
    }
}
