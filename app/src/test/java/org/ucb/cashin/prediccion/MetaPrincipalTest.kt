package org.ucb.cashin.prediccion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.ucb.cashin.model.AvanceMeta
import org.ucb.cashin.model.EstadoMeta
import java.math.BigDecimal
import java.time.LocalDate

class MetaPrincipalTest {

    private fun meta(
        id: String,
        objetivo: String,
        ahorrado: String,
        fecha: LocalDate? = null,
        estado: EstadoMeta = EstadoMeta.ACTIVA,
    ) = AvanceMeta(
        metaId = id,
        usuarioId = "u",
        nombre = id,
        montoObjetivo = BigDecimal(objetivo),
        fechaLimite = fecha,
        estado = estado,
        totalAhorrado = BigDecimal(ahorrado),
    )

    private fun paso(numero: Int, estado: EstadoPaso, esMeta: Boolean = false) =
        PasoAhorro(numero, LocalDate.of(2026, 11, numero), BigDecimal.TEN, BigDecimal.TEN, estado, esMeta)

    private fun plan(estado: EstadoPlan, vararg pasos: PasoAhorro) =
        PlanAhorro(estado, CadenciaPlan.MENSUAL, 1, BigDecimal.TEN, pasos.toList(), null)

    private val hecho = EstadoPaso.COMPLETADO
    private val pendiente = EstadoPaso.PENDIENTE

    @Test fun `la meta que ya llego tiene prioridad aunque haya otra mas avanzada en porcentaje`() {
        val elegida = elegirMetaPrincipal(listOf(meta("a", "1000", "950"), meta("b", "500", "500")))

        assertEquals("b", elegida?.metaId)
    }

    @Test fun `si ninguna llego elige la mas avanzada`() {
        val elegida = elegirMetaPrincipal(listOf(meta("a", "1000", "100"), meta("b", "1000", "420")))

        assertEquals("b", elegida?.metaId)
    }

    @Test fun `si empatan gana la fecha mas cercana y sin fecha va al final`() {
        val elegida = elegirMetaPrincipal(
            listOf(
                meta("sin", "1000", "100"),
                meta("lejos", "1000", "100", LocalDate.of(2027, 5, 1)),
                meta("cerca", "1000", "100", LocalDate.of(2026, 12, 1)),
            ),
        )

        assertEquals("cerca", elegida?.metaId)
    }

    @Test fun `las completadas y canceladas no cuentan`() {
        val elegida = elegirMetaPrincipal(
            listOf(
                meta("c", "100", "100", estado = EstadoMeta.COMPLETADA),
                meta("x", "100", "90", estado = EstadoMeta.CANCELADA),
            ),
        )

        assertNull(elegida)
    }

    @Test fun `la tira muestra dos hechos, el actual y el siguiente`() {
        val p = plan(
            EstadoPlan.ALCANZA,
            paso(1, hecho), paso(2, hecho), paso(3, hecho),
            paso(4, EstadoPaso.ACTUAL), paso(5, pendiente), paso(6, pendiente),
            paso(7, pendiente, esMeta = true),
        )

        assertEquals(listOf(2, 3, 4, 5), p.pasosResumidos().map { it.numero })
    }

    @Test fun `sin pasos hechos empieza en el actual`() {
        val p = plan(EstadoPlan.ALCANZA, paso(1, EstadoPaso.ACTUAL), paso(2, pendiente), paso(3, pendiente, esMeta = true))

        assertEquals(listOf(1, 2), p.pasosResumidos().map { it.numero })
    }

    @Test fun `si solo falta el ultimo paso muestra los hechos`() {
        val p = plan(EstadoPlan.ALCANZA, paso(1, hecho), paso(2, hecho), paso(3, EstadoPaso.ACTUAL, esMeta = true))

        assertEquals(listOf(1, 2), p.pasosResumidos().map { it.numero })
    }

    @Test fun `meta lograda conserva el ultimo paso aunque sea el de la meta`() {
        val p = plan(EstadoPlan.LOGRADA, paso(1, hecho), paso(2, hecho, esMeta = true))

        assertEquals(listOf(1, 2), p.pasosResumidos().map { it.numero })
    }

    @Test fun `sin datos no muestra pasos`() {
        assertTrue(plan(EstadoPlan.SIN_DATOS, paso(1, hecho)).pasosResumidos().isEmpty())
    }
}
