package org.ucb.cashin.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import org.ucb.cashin.R
import org.ucb.cashin.prediccion.Animo
import org.ucb.cashin.prediccion.Confianza
import org.ucb.cashin.prediccion.MensajeAvatar
import org.ucb.cashin.prediccion.TipoMensaje
import org.ucb.cashin.util.Formato
import java.math.BigDecimal

/*
 * Puente entre el motor de predicción (Kotlin puro) y la pantalla:
 * convierte un MensajeAvatar en el texto de strings.xml y en la pose de Cashi.
 * Lo usan el detalle de meta (HU-20) y la tarjeta de pronóstico de Inicio.
 */

/** Texto del avatar para el mensaje que eligió el motor. */
@Composable
fun MensajeAvatar.texto(): String {
    val montoTexto = Formato.monto(monto ?: BigDecimal.ZERO)
    return when (tipo) {
        TipoMensaje.META_ALCANZADA -> stringResource(R.string.ia_meta_alcanzada)
        TipoMensaje.META_ALCANZABLE -> {
            val cantidad = meses ?: 0
            val cuando = fecha?.let { Formato.mes(it).lowercase() }.orEmpty()
            pluralStringResource(R.plurals.ia_meta_alcanzable, cantidad, montoTexto, cantidad, cuando)
        }
        TipoMensaje.META_NO_LLEGA -> stringResource(R.string.ia_meta_no_llega, montoTexto)
        TipoMensaje.META_RITMO_NEGATIVO -> stringResource(R.string.ia_meta_ritmo_negativo)
        TipoMensaje.SIN_DATOS -> stringResource(R.string.ia_sin_datos)
        TipoMensaje.GASTO_MAS_RAPIDO -> stringResource(R.string.ia_gasto_mas_rapido)
        TipoMensaje.GASTO_NORMAL -> stringResource(R.string.ia_gasto_normal)
    }
}

/** Pose de Cashi para cada ánimo del motor. */
fun Animo.emocion(): Emocion = when (this) {
    Animo.FELIZ -> Emocion.FELIZ
    Animo.NEUTRAL -> Emocion.NEUTRAL
    Animo.PREOCUPADO -> Emocion.PREOCUPADO
    Animo.CELEBRANDO -> Emocion.CELEBRANDO
}

/** "Estimación con 4 meses de datos" o "Estimación de confianza baja…" según la confianza. */
@Composable
fun textoConfianza(confianza: Confianza, mesesDeDatos: Int): String = when (confianza) {
    Confianza.SIN_DATOS -> stringResource(R.string.ia_confianza_sin_datos)
    else -> pluralStringResource(R.plurals.ia_confianza_meses, mesesDeDatos, mesesDeDatos, nivelConfianza(confianza))
}

@Composable
private fun nivelConfianza(confianza: Confianza): String = stringResource(
    when (confianza) {
        Confianza.ALTA -> R.string.ia_confianza_alta
        Confianza.MEDIA -> R.string.ia_confianza_media
        else -> R.string.ia_confianza_baja
    },
)