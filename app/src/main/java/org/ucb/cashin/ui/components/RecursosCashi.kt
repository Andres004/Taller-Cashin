package org.ucb.cashin.ui.components

import android.content.Context
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ucb.cashin.R
import java.io.IOException

/**
 * Catálogo único de las imágenes de Cashi: un valor por cada momento de la app.
 *
 * [archivoAnimado] es el nombre (sin extensión) del WebP animado en `res/raw/`.
 * Mientras ese archivo no exista se muestra [imagenFija], así que agregar una animación
 * no requiere tocar código. La lista para el diseñador está en docs/RECURSOS_CASHI.md.
 */
enum class RecursoCashi(val archivoAnimado: String, @DrawableRes val imagenFija: Int) {
    BIENVENIDA("cashi_bienvenida", R.drawable.logo_cashin),

    DESAFIO_CUMPLIDO("cashi_desafio_cumplido", R.drawable.avatar_celebrando),
    META_COMPLETADA("cashi_meta_completada", R.drawable.avatar_celebrando),
    SUBIDA_NIVEL("cashi_subida_nivel", R.drawable.avatar_celebrando),

    FELIZ("cashi_feliz", R.drawable.avatar_feliz),
    PENSANDO("cashi_pensando", R.drawable.avatar_pensando),
    PREOCUPADO("cashi_preocupado", R.drawable.avatar_pensando), // aún no hay pose fija propia
    CELEBRANDO("cashi_celebrando", R.drawable.avatar_celebrando),
    COOL("cashi_cool", R.drawable.avatar_cool),

    METAS_VACIO("cashi_metas_vacio", R.drawable.avatar_feliz),
    META_LOGRADA("cashi_meta_lograda", R.drawable.avatar_celebrando),
}

/**
 * Muestra el [recurso] animado si su archivo existe y el teléfono lo soporta (Android 9+);
 * si no, su imagen fija. El tamaño lo define el [modifier].
 */
@Composable
fun ImagenCashi(
    recurso: RecursoCashi,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val animacion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) rememberAnimacion(recurso) else null
    if (animacion != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        AnimacionCashi(animacion, contentDescription, modifier)
    } else {
        Image(
            painter = painterResource(recurso.imagenFija),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = modifier,
        )
    }
}

/** Decodifica fuera del hilo principal; mientras tanto (o si no hay archivo) devuelve null. */
@RequiresApi(Build.VERSION_CODES.P)
@Composable
private fun rememberAnimacion(recurso: RecursoCashi): AnimatedImageDrawable? {
    val contexto = LocalContext.current
    val enVistaPrevia = LocalInspectionMode.current
    val animacion by produceState<AnimatedImageDrawable?>(initialValue = null, recurso) {
        if (!enVistaPrevia) value = withContext(Dispatchers.IO) { decodificar(contexto, recurso) }
    }
    return animacion
}

/** Null si el archivo no existe, está dañado o en realidad es una imagen sin animación. */
@RequiresApi(Build.VERSION_CODES.P)
private fun decodificar(contexto: Context, recurso: RecursoCashi): AnimatedImageDrawable? {
    val id = contexto.resources.getIdentifier(recurso.archivoAnimado, "raw", contexto.packageName)
    if (id == 0) return null
    return try {
        ImageDecoder.decodeDrawable(ImageDecoder.createSource(contexto.resources, id)) as? AnimatedImageDrawable
    } catch (_: IOException) {
        null
    }
}

/** El `Image` de Compose no reproduce un AnimatedImageDrawable: lo hace un ImageView. */
@RequiresApi(Build.VERSION_CODES.P)
@Composable
private fun AnimacionCashi(
    animacion: AnimatedImageDrawable,
    contentDescription: String?,
    modifier: Modifier,
) {
    AndroidView(
        factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
        update = {
            it.setImageDrawable(animacion)
            it.contentDescription = contentDescription
        },
        modifier = modifier,
    )
    DisposableEffect(animacion) {
        animacion.start()
        onDispose { animacion.stop() }
    }
}
