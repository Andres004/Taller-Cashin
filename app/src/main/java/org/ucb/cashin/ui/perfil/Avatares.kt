package org.ucb.cashin.ui.perfil

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.ucb.cashin.R
import org.ucb.cashin.ui.components.ImagenCashi
import org.ucb.cashin.ui.components.RecursoCashi

/**
 * Las poses de Cashi que se pueden elegir como avatar. [clave] es lo que se guarda en `perfil.avatar`.
 * El valor anterior "salchi" (el de todas las cuentas viejas) se muestra como [FELIZ].
 */
enum class AvatarCashi(val clave: String, val recurso: RecursoCashi, @StringRes val nombre: Int) {
    FELIZ("feliz", RecursoCashi.FELIZ, R.string.avatar_feliz),
    COOL("cool", RecursoCashi.COOL, R.string.avatar_cool),
    CELEBRANDO("celebrando", RecursoCashi.CELEBRANDO, R.string.avatar_celebrando),
    PENSANDO("pensando", RecursoCashi.PENSANDO, R.string.avatar_pensando),
    ESTRELLA("estrella", RecursoCashi.XP, R.string.avatar_estrella),
    MEDALLA("medalla", RecursoCashi.SUBIDA_NIVEL, R.string.avatar_medalla);

    companion object {
        fun de(clave: String?): AvatarCashi = entries.firstOrNull { it.clave == clave } ?: FELIZ
    }
}

/** El avatar en un círculo crema con borde naranja (o gris si [elegido] es false en la lista). */
@Composable
fun CirculoAvatar(
    avatar: AvatarCashi,
    modifier: Modifier = Modifier,
    tamano: Dp = 84.dp,
    resaltado: Boolean = true,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        border = BorderStroke(3.dp, if (resaltado) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline),
        modifier = modifier.size(tamano),
    ) {
        Box(contentAlignment = Alignment.Center) {
            ImagenCashi(
                recurso = avatar.recurso,
                contentDescription = stringResource(avatar.nombre),
                modifier = Modifier.size(tamano - 10.dp).clip(CircleShape),
            )
        }
    }
}
