package org.ucb.cashin.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.ucb.cashin.R
import org.ucb.cashin.ui.theme.CashinColores
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio

/**
 * Emociones del avatar "Salchi". Coinciden con el campo `emocion` que devuelve la IA
 * (Maestro §8.3: feliz | neutral | preocupado | celebrando). `COOL` es para rachas y logros.
 */
enum class Emocion(val recurso: RecursoCashi) {
    FELIZ(RecursoCashi.FELIZ),
    NEUTRAL(RecursoCashi.PENSANDO),
    PREOCUPADO(RecursoCashi.PREOCUPADO),
    CELEBRANDO(RecursoCashi.CELEBRANDO),
    COOL(RecursoCashi.COOL);

    companion object {
        /** Convierte el texto de la IA ("feliz", "celebrando"...) en una emoción; NEUTRAL si no se reconoce. */
        fun desdeTexto(texto: String?): Emocion =
            entries.firstOrNull { it.name.equals(texto?.trim(), ignoreCase = true) } ?: NEUTRAL
    }
}

/** Solo el avatar. [recurso] permite usar la imagen de un momento concreto (por ejemplo, un logro). */
@Composable
fun AvatarImagen(
    emocion: Emocion,
    modifier: Modifier = Modifier,
    tamano: Dp = 96.dp,
    recurso: RecursoCashi = emocion.recurso,
) {
    ImagenCashi(
        recurso = recurso,
        contentDescription = stringResource(R.string.avatar_descripcion),
        modifier = modifier.size(tamano),
    )
}

/**
 * Avatar + globo de texto: la forma en que Cashin "habla" con el usuario
 * (consejos, celebraciones, estados vacíos).
 */
@Composable
fun AvatarBurbuja(
    mensaje: String,
    modifier: Modifier = Modifier,
    emocion: Emocion = Emocion.FELIZ,
    tamanoAvatar: Dp = 88.dp,
    recurso: RecursoCashi = emocion.recurso,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.s),
    ) {
        AvatarImagen(emocion = emocion, tamano = tamanoAvatar, recurso = recurso)
        Box(modifier = Modifier.weight(1f)) {
            Surface(
                color = CashinColores.fondoAvatar,
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 20.dp),
            ) {
                Text(
                    text = mensaje,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA)
@Composable
private fun AvatarBurbujaPreview() = CashinTheme {
    AvatarBurbuja(mensaje = "¡Hola! Anota tu primer gasto del día y suma 10 XP.", emocion = Emocion.FELIZ)
}
