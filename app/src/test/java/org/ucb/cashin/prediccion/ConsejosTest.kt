package org.ucb.cashin.prediccion

import org.junit.Assert.assertEquals
import org.junit.Test
import org.ucb.cashin.model.AvanceMeta
import org.ucb.cashin.model.Desafio
import org.ucb.cashin.model.EstadoMeta
import org.ucb.cashin.model.GastoCategoriaMes
import org.ucb.cashin.model.PeriodoDesafio
import org.ucb.cashin.model.ProgresoDesafio
import org.ucb.cashin.model.ReglaDesafio
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class ConsejosTest {

    /** Día 15 de un mes de 30 días: el ritmo del mes se multiplica por 2. */
    private val hoy = LocalDate.of(2026, 11, 15)

    private fun bd(texto: String) = BigDecimal(texto)

    private val pronostico = Pronostico(
        esperado = bd("1000.00"),
        minimo = bd("900.00"),
        maximo = bd("1100.00"),
        confianza = Confianza.MEDIA,
        mesesUsados = 3,
    )

    private val desafio = Desafio(
        id = "d1",
        usuarioId = "u1",
        titulo = "Comida bajo control",
        periodo = PeriodoDesafio.SEMANAL,
        regla = ReglaDesafio.LIMITE_CATEGORIA,
        categoriaId = "c-ali",
        montoLimite = bd("200.00"),
        fechaInicio = LocalDate.of(2026, 11, 9),
        fechaFin = LocalDate.of(2026, 11, 15),
    )

    private fun progreso(gastado: String) = ProgresoDesafio("d1", bd(gastado), diasConRegistro = 3)

    private fun meta(ahorrado: String) = AvanceMeta(
        metaId = "m1",
        usuarioId = "u1",
        nombre = "Celular",
        montoObjetivo = bd("1000.00"),
        estado = EstadoMeta.ACTIVA,
        totalAhorrado = bd(ahorrado),
    )

    private fun gasto(mes: YearMonth, total: String) = GastoCategoriaMes(
        usuarioId = "u1",
        mes = mes.atDay(1),
        categoriaId = "c-ali",
        nombre = "Alimentación",
        total = bd(total),
    )

    /** Alimentación: 300 en cada uno de los 3 meses completos y [actual] en lo que va del mes. */
    private fun gastosConMesActual(actual: String) =
        listOf(8, 9, 10).map { gasto(YearMonth.of(2026, it), "300") } + gasto(YearMonth.of(2026, 11), actual)

    private fun consejo(
        anotoHoy: Boolean = true,
        racha: Int = 0,
        gastos: List<GastoCategoriaMes> = emptyList(),
        proyeccion: BigDecimal? = null,
        pronosticoDelMes: Pronostico = Pronostico.SIN_DATOS,
        desafioActivo: Desafio? = null,
        progresoDesafio: ProgresoDesafio? = null,
        metaMasCercana: AvanceMeta? = null,
    ) = consejoDelDia(
        hoy, anotoHoy, racha, gastos, proyeccion, pronosticoDelMes,
        desafioActivo, progresoDesafio, metaMasCercana,
    )

    // 1 · Racha en riesgo

    @Test fun `racha de 2 o mas sin anotar hoy esta en riesgo`() {
        val m = consejo(anotoHoy = false, racha = 4)
        assertEquals(TipoMensaje.RACHA_EN_RIESGO, m.tipo)
        assertEquals(Animo.PREOCUPADO, m.animo)
        assertEquals(4, m.dias)
    }

    @Test fun `racha de 1 sin anotar hoy no avisa`() {
        assertEquals(TipoMensaje.SIN_DATOS, consejo(anotoHoy = false, racha = 1).tipo)
    }

    // 2 · Desafío en riesgo

    @Test fun `desafio con 80 por ciento gastado avisa lo que queda`() {
        val m = consejo(desafioActivo = desafio, progresoDesafio = progreso("170"))
        assertEquals(TipoMensaje.DESAFIO_EN_RIESGO, m.tipo)
        assertEquals(Animo.PREOCUPADO, m.animo)
        assertEquals(bd("30.00"), m.monto)
        assertEquals("Comida bajo control", m.nombre)
    }

    @Test fun `desafio por debajo del 80 por ciento no avisa`() {
        assertEquals(TipoMensaje.SIN_DATOS, consejo(desafioActivo = desafio, progresoDesafio = progreso("150")).tipo)
    }

    @Test fun `desafio de dias no tiene limite y no avisa`() {
        val dias = desafio.copy(regla = ReglaDesafio.DIAS_REGISTRO, montoLimite = null, diasObjetivo = 5)
        assertEquals(TipoMensaje.SIN_DATOS, consejo(desafioActivo = dias, progresoDesafio = progreso("999")).tipo)
    }

    // 3 · Categoría que sube

    @Test fun `categoria que va 30 por ciento arriba de su promedio avisa`() {
        val m = consejo(gastos = gastosConMesActual("200"))          // 200 × 30/15 = 400 ≥ 300 × 1,3
        assertEquals(TipoMensaje.CATEGORIA_SUBE, m.tipo)
        assertEquals(Animo.PREOCUPADO, m.animo)
        assertEquals("Alimentación", m.nombre)
        assertEquals(bd("400.00"), m.monto)
    }

    @Test fun `categoria por debajo del 30 por ciento no avisa`() {
        assertEquals(TipoMensaje.GASTO_NORMAL, consejo(gastos = gastosConMesActual("180")).tipo)   // 360 < 390
    }

    // 4 · Gasto más rápido

    @Test fun `proyeccion sobre el maximo pronosticado avisa`() {
        val m = consejo(proyeccion = bd("1200.00"), pronosticoDelMes = pronostico)
        assertEquals(TipoMensaje.GASTO_MAS_RAPIDO, m.tipo)
        assertEquals(bd("1200.00"), m.monto)
    }

    // 5 · Meta cerca

    @Test fun `meta entre 90 y 100 por ciento celebra con lo que falta`() {
        val m = consejo(metaMasCercana = meta("950.00"))
        assertEquals(TipoMensaje.META_CERCA, m.tipo)
        assertEquals(Animo.CELEBRANDO, m.animo)
        assertEquals(bd("50.00"), m.monto)
        assertEquals("Celular", m.nombre)
    }

    @Test fun `meta ya alcanzada o lejos no es meta cerca`() {
        assertEquals(TipoMensaje.SIN_DATOS, consejo(metaMasCercana = meta("1000.00")).tipo)
        assertEquals(TipoMensaje.SIN_DATOS, consejo(metaMasCercana = meta("800.00")).tipo)
    }

    // 6 · Racha buena · 7 · Sin datos / normal

    @Test fun `racha de 3 o mas anotando hoy felicita`() {
        val m = consejo(racha = 3)
        assertEquals(TipoMensaje.RACHA_BUENA, m.tipo)
        assertEquals(Animo.FELIZ, m.animo)
        assertEquals(3, m.dias)
    }

    @Test fun `sin datos pide anotar mas`() {
        val m = consejo()
        assertEquals(TipoMensaje.SIN_DATOS, m.tipo)
        assertEquals(Animo.NEUTRAL, m.animo)
    }

    @Test fun `con datos y nada especial el gasto va normal`() {
        val m = consejo(proyeccion = bd("1000.00"), pronosticoDelMes = pronostico)
        assertEquals(TipoMensaje.GASTO_NORMAL, m.tipo)
        assertEquals(Animo.FELIZ, m.animo)
    }

    // Prioridad: cuando varias reglas se cumplen, gana la primera

    @Test fun `la prioridad se respeta de la regla 1 a la 6`() {
        val todo = { anotoHoy: Boolean, conDesafio: Boolean, conCategoria: Boolean, conProyeccion: Boolean, conMeta: Boolean ->
            consejo(
                anotoHoy = anotoHoy,
                racha = 5,
                gastos = if (conCategoria) gastosConMesActual("200") else emptyList(),
                proyeccion = if (conProyeccion) bd("1200.00") else null,
                pronosticoDelMes = pronostico,
                desafioActivo = desafio.takeIf { conDesafio },
                progresoDesafio = progreso("190"),
                metaMasCercana = meta(if (conMeta) "950.00" else "100.00"),
            ).tipo
        }
        assertEquals(TipoMensaje.RACHA_EN_RIESGO, todo(false, true, true, true, true))
        assertEquals(TipoMensaje.DESAFIO_EN_RIESGO, todo(true, true, true, true, true))
        assertEquals(TipoMensaje.CATEGORIA_SUBE, todo(true, false, true, true, true))
        assertEquals(TipoMensaje.GASTO_MAS_RAPIDO, todo(true, false, false, true, true))
        assertEquals(TipoMensaje.META_CERCA, todo(true, false, false, false, true))
        assertEquals(TipoMensaje.RACHA_BUENA, todo(true, false, false, false, false))
    }
}
