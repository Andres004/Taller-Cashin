package org.ucb.cashin.prediccion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.ucb.cashin.model.AvanceMeta
import org.ucb.cashin.model.EstadoMeta
import org.ucb.cashin.model.EstadoPlanificado
import org.ucb.cashin.model.Frecuencia
import org.ucb.cashin.model.GastoCategoriaMes
import org.ucb.cashin.model.RegistroPlanificado
import org.ucb.cashin.model.TipoRegistro
import java.math.BigDecimal
import java.time.LocalDate

class GastosFijosTest {

    private val hoy = LocalDate.of(2026, 11, 15)
    private fun bd(texto: String) = BigDecimal(texto)

    private fun planificado(
        categoria: String,
        monto: String,
        frecuencia: Frecuencia = Frecuencia.MENSUAL,
        estado: EstadoPlanificado = EstadoPlanificado.ACTIVO,
        tipo: TipoRegistro = TipoRegistro.GASTO,
    ) = RegistroPlanificado(
        id = categoria + monto,
        usuarioId = "u",
        categoriaId = categoria,
        tipo = tipo,
        nombre = "Pago",
        monto = bd(monto),
        frecuencia = frecuencia,
        proximaFecha = hoy,
        estado = estado,
    )

    /** El mismo gasto en los 3 meses completos anteriores a [hoy]. */
    private fun historia(categoria: String, monto: String) = (1L..3L).map { atras ->
        GastoCategoriaMes(
            usuarioId = "u",
            mes = hoy.withDayOfMonth(1).minusMonths(atras),
            categoriaId = categoria,
            nombre = categoria,
            total = bd(monto),
        )
    }

    private val importancias = mapOf("vivienda" to 4, "comida" to 3, "salidas" to 1)
    private val planAlquilerYWifi = listOf(planificado("vivienda", "900"), planificado("vivienda", "350"))
    private val historiaEjemplo = historia("vivienda", "1250") + historia("comida", "800") + historia("salidas", "300")

    private fun meta(objetivo: String) = AvanceMeta(
        metaId = "m",
        usuarioId = "u",
        nombre = "Celular",
        montoObjetivo = bd(objetivo),
        fechaLimite = null,
        estado = EstadoMeta.ACTIVA,
        totalAhorrado = bd("0"),
    )

    private fun pronostico(esperado: String) = Pronostico(bd(esperado), bd(esperado), bd(esperado), Confianza.MEDIA, 4)

    @Test
    fun `cada frecuencia se convierte a monto mensual`() {
        assertEquals(bd("433.33"), planificado("c", "100", Frecuencia.SEMANAL).montoMensual())
        assertEquals(bd("216.67"), planificado("c", "100", Frecuencia.QUINCENAL).montoMensual())
        assertEquals(bd("100.00"), planificado("c", "100", Frecuencia.MENSUAL).montoMensual())
        assertEquals(bd("100.00"), planificado("c", "1200", Frecuencia.ANUAL).montoMensual())
    }

    @Test
    fun `no son compromiso mensual el pago unico, el pausado ni el ingreso`() {
        assertNull(planificado("c", "100", Frecuencia.UNICA).montoMensual())
        assertNull(planificado("c", "100", estado = EstadoPlanificado.PAUSADO).montoMensual())
        assertNull(planificado("c", "100", tipo = TipoRegistro.INGRESO).montoMensual())
    }

    @Test
    fun `el porcentaje recortable baja a medida que sube la importancia`() {
        assertEquals(
            listOf(50, 30, 15, 5, 0),
            (1..5).map { porcentajeRecortable(it).toInt() },
        )
    }

    @Test
    fun `ejemplo aprobado separa fijo, variable y recorte posible`() {
        val d = desglosarGastos(planAlquilerYWifi, historiaEjemplo, importancias, hoy)

        assertEquals(bd("1250.00"), d.fijoMensual)
        assertEquals(bd("1100.00"), d.variableMensual)
        assertEquals(bd("270.00"), d.margenRecortable) // 800 × 15 % + 300 × 50 %
        assertEquals(bd("850.00"), capacidadDeAhorro(bd("3200"), bd("2350"), d))
    }

    @Test
    fun `un alquiler anotado a mano y luego planificado no se cuenta dos veces`() {
        val d = desglosarGastos(listOf(planificado("vivienda", "900")), historia("vivienda", "900"), importancias, hoy)

        assertEquals(bd("900.00"), d.fijoMensual)
        assertEquals(bd("0.00"), d.variableMensual)
        assertEquals(bd("0.00"), d.margenRecortable)
    }

    @Test
    fun `lo que excede al fijo en la misma categoria sigue siendo variable`() {
        val d = desglosarGastos(listOf(planificado("comida", "200")), historia("comida", "1000"), importancias, hoy)

        assertEquals(bd("800.00"), d.variableMensual)
    }

    @Test
    fun `un pago recien creado baja la capacidad aunque no este en el historial`() {
        val historiaSinWifi = historia("vivienda", "900") + historia("comida", "800") + historia("salidas", "300")
        val d = desglosarGastos(planAlquilerYWifi, historiaSinWifi, importancias, hoy)

        assertEquals(bd("850.00"), capacidadDeAhorro(bd("3200"), bd("2000"), d))
    }

    @Test
    fun `una categoria sin importancia conocida usa el valor por defecto`() {
        val d = desglosarGastos(emptyList(), historia("nueva", "100"), emptyMap(), hoy)

        assertEquals(bd("15.00"), d.margenRecortable)
    }

    @Test
    fun `sin desglose la capacidad es la de siempre`() {
        assertEquals(bd("350.00"), capacidadDeAhorro(bd("3200"), bd("2850"), null))
    }

    @Test
    fun `estimarMeta respeta los pagos fijos al calcular la capacidad`() {
        val historiaSinWifi = historia("vivienda", "900") + historia("comida", "800") + historia("salidas", "300")
        val d = desglosarGastos(planAlquilerYWifi, historiaSinWifi, importancias, hoy)

        val sinFijos = estimarMeta(meta("2400"), emptyList(), pronostico("3200"), pronostico("2000"), hoy)
        val conFijos = estimarMeta(meta("2400"), emptyList(), pronostico("3200"), pronostico("2000"), hoy, d)

        assertEquals(2, sinFijos.meses)
        assertEquals(3, conFijos.meses)
        assertEquals(bd("850.00"), conFijos.ahorroMensual)
        assertEquals(FuenteAhorro.CAPACIDAD, conFijos.fuente)
    }
}
