package org.ucb.cashin.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.ucb.cashin.R
import org.ucb.cashin.model.ErrorApp

/** Texto amigable (strings.xml) para cada [ErrorApp]. Los ViewModels guardan el ErrorApp, no el texto. */
@StringRes
fun ErrorApp.mensajeRes(): Int = when (this) {
    ErrorApp.SIN_CONEXION -> R.string.error_sin_conexion
    ErrorApp.CREDENCIALES_INVALIDAS -> R.string.error_credenciales
    ErrorApp.CORREO_YA_REGISTRADO -> R.string.error_correo_registrado
    ErrorApp.CORREO_NO_CONFIRMADO -> R.string.error_correo_no_confirmado
    ErrorApp.CONTRASENA_DEBIL -> R.string.error_contrasena_debil
    ErrorApp.CODIGO_INVALIDO -> R.string.error_codigo_invalido
    ErrorApp.DEMASIADOS_INTENTOS -> R.string.error_demasiados_intentos
    ErrorApp.SIN_PERMISO -> R.string.error_sin_permiso
    ErrorApp.DATO_INVALIDO -> R.string.error_dato_invalido
    ErrorApp.DESCONOCIDO -> R.string.error_desconocido
}

@Composable
fun ErrorApp.mensaje(): String = stringResource(mensajeRes())
