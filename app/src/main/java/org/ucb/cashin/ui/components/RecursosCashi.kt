package org.ucb.cashin.ui.components

import android.content.Context
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
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
 * [archivoAnimado] es el nombre (sin extensión) del WebP, animado o fijo, en `res/raw/`.
 * Mientras ese archivo no exista se muestra [imagenFija], así que cambiar un dibujo
 * no requiere tocar código. La lista de archivos está en docs/RECURSOS_CASHI.md.
 */
enum class RecursoCashi(val archivoAnimado: String, @DrawableRes val imagenFija: Int) {
    BIENVENIDA("cashi_bienvenida", R.drawable.logo_bienvenida),
    PANTALLA_BIENVENIDA("cashi_pantalla_bienvenida", R.drawable.logo_bienvenida),
    INICIO_SESION("cashi_inicio_sesion", R.drawable.pose_medalla),
    CREAR_CUENTA("cashi_crear_cuenta", R.drawable.pose_mochila),

    DESAFIO_CUMPLIDO("cashi_desafio_cumplido", R.drawable.pose_trofeo),
    META_COMPLETADA("cashi_meta_completada", R.drawable.pose_cofre),
    SUBIDA_NIVEL("cashi_subida_nivel", R.drawable.pose_medalla),

    FELIZ("cashi_feliz", R.drawable.avatar_feliz),
    PENSANDO("cashi_pensando", R.drawable.avatar_pensando),
    PREOCUPADO("cashi_preocupado", R.drawable.avatar_preocupado),
    CELEBRANDO("cashi_celebrando", R.drawable.avatar_celebrando),
    COOL("cashi_cool", R.drawable.avatar_cool),

    METAS_VACIO("cashi_metas_vacio", R.drawable.pose_mochila),
    META_LOGRADA("cashi_meta_lograda", R.drawable.pose_trofeo_cerca),

    RACHA("cashi_racha", R.drawable.pose_llama),
    XP("cashi_xp", R.drawable.pose_estrella),
    AHORRO("cashi_ahorro", R.drawable.pose_cofre),
    CAMINO("cashi_camino", R.drawable.pose_mochila),
}

/**
 * Muestra el archivo de [recurso] si existe y el teléfono lo soporta (Android 9+);
 * si no, su imagen fija. El tamaño lo define el [modifier].
 */
@Composable
fun ImagenCashi(
    recurso: RecursoCashi,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val archivo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) rememberArchivo(recurso) else null
    if (archivo != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ArchivoCashi(archivo, contentDescription, modifier)
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
private fun rememberArchivo(recurso: RecursoCashi): Drawable? {
    val contexto = LocalContext.current
    val enVistaPrevia = LocalInspectionMode.current
    val archivo by produceState<Drawable?>(initialValue = null, recurso) {
        if (!enVistaPrevia) value = withContext(Dispatchers.IO) { decodificar(contexto, recurso) }
    }
    return archivo
}

/** Null si el archivo no existe o está dañado. Sirve tanto un WebP animado como uno fijo. */
@RequiresApi(Build.VERSION_CODES.P)
private fun decodificar(contexto: Context, recurso: RecursoCashi): Drawable? {
    val id = contexto.resources.getIdentifier(recurso.archivoAnimado, "raw", contexto.packageName)
    if (id == 0) return null
    return try {
        ImageDecoder.decodeDrawable(ImageDecoder.createSource(contexto.resources, id))
    } catch (_: IOException) {
        null
    }
}

/** El `Image` de Compose no reproduce un AnimatedImageDrawable: lo hace un ImageView. */
@RequiresApi(Build.VERSION_CODES.P)
@Composable
private fun ArchivoCashi(
    archivo: Drawable,
    contentDescription: String?,
    modifier: Modifier,
) {
    AndroidView(
        factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
        update = {
            it.setImageDrawable(archivo)
            it.contentDescription = contentDescription
        },
        modifier = modifier,
    )
    DisposableEffect(archivo) {
        val animacion = archivo as? AnimatedImageDrawable
        animacion?.start()
        onDispose { animacion?.stop() }
    }
}
