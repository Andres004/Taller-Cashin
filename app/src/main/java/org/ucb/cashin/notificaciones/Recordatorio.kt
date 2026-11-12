package org.ucb.cashin.notificaciones

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.ucb.cashin.MainActivity
import org.ucb.cashin.R
import org.ucb.cashin.prediccion.AjusteRecordatorio
import org.ucb.cashin.prediccion.TipoAviso
import org.ucb.cashin.prediccion.debeAvisar
import org.ucb.cashin.prediccion.mensajeRecordatorio
import org.ucb.cashin.prediccion.proximoAviso
import org.ucb.cashin.util.Formato
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/*
 * Recordatorio diario (Sprint 5): un aviso a la hora elegida si ese día todavía no se anotó nada.
 * Todo vive en el teléfono (la alarma es de cada teléfono): no hay columnas nuevas ni librerías.
 * Qué decir y cuándo lo decide prediccion/Recordatorio.kt.
 */

private const val CANAL = "recordatorio_diario"
private const val ID_NOTIFICACION = 2611
private const val PREFERENCIAS = "recordatorio"

/**
 * Lo que el recordatorio guarda en el teléfono: lo que eligió la persona y lo último que la app
 * supo de sus anotaciones (para no avisar si hoy ya anotó y para nombrar la racha).
 */
class RecordatorioLocal(private val contexto: Context) {

    private val prefs get() = contexto.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE)

    fun ajuste(): AjusteRecordatorio = AjusteRecordatorio(
        activo = prefs.getBoolean("activo", false),
        hora = prefs.getInt("hora", org.ucb.cashin.prediccion.HORA_POR_DEFECTO),
        minuto = prefs.getInt("minuto", 0),
        soloLaborables = prefs.getBoolean("solo_laborables", false),
    )

    /** Guarda y vuelve a programar (o cancela) la alarma. */
    fun guardar(ajuste: AjusteRecordatorio) {
        prefs.edit()
            .putBoolean("activo", ajuste.activo)
            .putInt("hora", ajuste.hora)
            .putInt("minuto", ajuste.minuto)
            .putBoolean("solo_laborables", ajuste.soloLaborables)
            .apply()
        programar(contexto)
    }

    val ultimaAnotacion: LocalDate?
        get() = prefs.getLong("ultima_anotacion", Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }?.let(LocalDate::ofEpochDay)

    val racha: Int get() = prefs.getInt("racha", 0)

    /** Lo que se sabe del perfil (al abrir Inicio) o de una anotación recién guardada. */
    fun conocer(ultimaAnotacion: LocalDate?, racha: Int) {
        prefs.edit()
            .putLong("ultima_anotacion", ultimaAnotacion?.toEpochDay() ?: Long.MIN_VALUE)
            .putInt("racha", racha)
            .apply()
    }

    /** Si el teléfono puede mostrar el aviso (en Android 13+ hace falta permiso). */
    fun puedeNotificar(): Boolean {
        val permiso = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return permiso && NotificationManagerCompat.from(contexto).areNotificationsEnabled()
    }
}

/** El canal de notificaciones (Android 8+). Se crea al abrir la app; crearlo dos veces no hace nada. */
fun crearCanalRecordatorio(contexto: Context) {
    val canal = NotificationChannel(
        CANAL,
        contexto.getString(R.string.recordatorio_canal),
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply { description = contexto.getString(R.string.recordatorio_canal_desc) }
    contexto.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
}

private fun intentoAlarma(contexto: Context): PendingIntent = PendingIntent.getBroadcast(
    contexto,
    0,
    Intent(contexto, RecordatorioReceiver::class.java).setAction(RecordatorioReceiver.AVISAR),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)

/**
 * Programa la próxima alarma según el ajuste guardado, o la cancela si está apagado.
 * Es una alarma no exacta (puede llegar unos minutos tarde): no necesita el permiso de alarmas exactas.
 */
fun programar(contexto: Context) {
    val alarmas = contexto.getSystemService(AlarmManager::class.java)
    val intento = intentoAlarma(contexto)
    val cuando = proximoAviso(LocalDateTime.now(), RecordatorioLocal(contexto).ajuste())
    if (cuando == null) {
        alarmas.cancel(intento)
        return
    }
    val milis = cuando.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    alarmas.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, milis, intento)
}

/** Muestra el aviso si hoy todavía no se anotó nada. Tocarlo abre la app. */
private fun avisar(contexto: Context) {
    val local = RecordatorioLocal(contexto)
    val hoy = Formato.hoy()
    if (!debeAvisar(hoy, local.ultimaAnotacion, local.ajuste()) || !local.puedeNotificar()) return

    val mensaje = mensajeRecordatorio(hoy, local.ultimaAnotacion, local.racha)
    val titulo = when (mensaje.tipo) {
        TipoAviso.RACHA_EN_RIESGO -> contexto.resources.getQuantityString(R.plurals.recordatorio_racha_titulo, mensaje.racha, mensaje.racha)
        TipoAviso.SIN_RACHA -> contexto.getString(R.string.recordatorio_sin_racha_titulo)
    }
    val texto = when (mensaje.tipo) {
        TipoAviso.RACHA_EN_RIESGO -> contexto.getString(R.string.recordatorio_racha_texto)
        TipoAviso.SIN_RACHA -> contexto.getString(R.string.recordatorio_sin_racha_texto)
    }
    val abrir = PendingIntent.getActivity(
        contexto,
        0,
        Intent(contexto, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val notificacion = NotificationCompat.Builder(contexto, CANAL)
        .setSmallIcon(R.drawable.ic_notificacion)
        .setColor(ContextCompat.getColor(contexto, R.color.naranja))
        .setContentTitle(titulo)
        .setContentText(texto)
        .setContentIntent(abrir)
        .setAutoCancel(true)
        .build()
    try {
        NotificationManagerCompat.from(contexto).notify(ID_NOTIFICACION, notificacion)
    } catch (_: SecurityException) {
        // Se quitó el permiso entre la revisión y el aviso: no hay nada más que hacer.
    }
}

/**
 * Recibe la alarma diaria y el reinicio del teléfono. En los dos casos deja programada la próxima.
 * No está exportado: solo el sistema y la propia app le envían mensajes.
 */
class RecordatorioReceiver : BroadcastReceiver() {
    override fun onReceive(contexto: Context, intento: Intent) {
        if (intento.action == AVISAR) avisar(contexto)
        programar(contexto)
    }

    companion object {
        const val AVISAR = "org.ucb.cashin.RECORDATORIO"
    }
}
