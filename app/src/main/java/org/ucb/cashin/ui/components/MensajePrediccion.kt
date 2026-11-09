package org.ucb.cashin.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import org.ucb.cashin.R
import org.ucb.cashin.prediccion.Animo
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
        TipoMensaje.META_ALCANZABLE -> stringResource(R.string.ia_meta_alcanzable)
        TipoMensaje.META_NO_LLEGA -> stringResource(R.string.ia_meta_no_llega)
        TipoMensaje.META_RITMO_NEGATIVO -> stringResource(R.string.ia_meta_ritmo_negativo)
        TipoMensaje.SIN_DATOS -> stringResource(R.string.ia_sin_datos)
        TipoMensaje.GASTO_MAS_RAPIDO -> stringResource(R.string.ia_gasto_mas_rapido)
        TipoMensaje.GASTO_NORMAL -> stringResource(R.string.ia_gasto_normal)
        TipoMensaje.RACHA_EN_RIESGO -> (dias ?: 0).let { pluralStringResource(R.plurals.ia_racha_en_riesgo, it, it) }
        TipoMensaje.DESAFIO_EN_RIESGO -> stringResource(R.string.ia_desafio_en_riesgo, nombre.orEmpty(), montoTexto)
        TipoMensaje.CATEGORIA_SUBE ->
            stringResource(R.string.ia_categoria_sube, nombre.orEmpty(), Formato.montoAproximado(monto ?: BigDecimal.ZERO))
        TipoMensaje.META_CERCA -> stringResource(R.string.ia_meta_cerca, nombre.orEmpty(), montoTexto)
        TipoMensaje.RACHA_BUENA -> (dias ?: 0).let { pluralStringResource(R.plurals.ia_racha_buena, it, it) }
        TipoMensaje.INGRESOS_DEL_MES -> stringResource(R.string.ia_ingresos_del_mes, montoTexto)
        TipoMensaje.INGRESOS_A_FAVOR -> stringResource(R.string.ia_ingresos_a_favor, montoTexto)
        TipoMensaje.META_AVANCE -> stringResource(R.string.ia_meta_avance, nombre.orEmpty(), montoTexto)
        TipoMensaje.XP_PARA_NIVEL -> stringResource(R.string.ia_xp_para_nivel, dias ?: 0)
    }
}

/** Pose de Cashi para cada ánimo del motor. */
fun Animo.emocion(): Emocion = when (this) {
    Animo.FELIZ -> Emocion.FELIZ
    Animo.NEUTRAL -> Emocion.NEUTRAL
    Animo.PREOCUPADO -> Emocion.PREOCUPADO
    Animo.CELEBRANDO -> Emocion.CELEBRANDO
}
