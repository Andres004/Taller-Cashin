@file:UseSerializers(BigDecimalSerializer::class, LocalDateSerializer::class, InstantSerializer::class)

package org.ucb.cashin.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.ucb.cashin.util.BigDecimalSerializer
import org.ucb.cashin.util.InstantSerializer
import org.ucb.cashin.util.LocalDateSerializer
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/*
 * Una data class por tabla de supabase/migrations/001_esquema.sql (para LEER).
 * Para INSERTAR se usan las clases Nuevo* (sin id, usuario_id ni fechas automáticas):
 * la base de datos completa esos valores con sus DEFAULT (usuario_id = auth.uid()).
 * Regla: si cambia una columna en SQL, se cambia aquí en el mismo PR.
 */

/** Tabla `perfil`. El nivel se calcula: [nivel]. */
@Serializable
data class Perfil(
    val id: String,
    val nombre: String,
    val moneda: Moneda = Moneda.BOB,
    val avatar: String = "salchi",
    val xp: Int = 0,
    @SerialName("racha_actual") val rachaActual: Int = 0,
    @SerialName("racha_maxima") val rachaMaxima: Int = 0,
    @SerialName("ultimo_registro") val ultimoRegistro: LocalDate? = null,
    @SerialName("creado_en") val creadoEn: Instant? = null,
) {
    /** Nivel = xp / 200 + 1 (Maestro §7.3). */
    val nivel: Int get() = xp / XP_POR_NIVEL + 1

    companion object {
        const val XP_POR_NIVEL = 200
    }
}

/** Campos del perfil que la app SÍ puede editar (los demás los cambia el servidor). */
@Serializable
data class PerfilEditable(
    val nombre: String,
    val moneda: Moneda,
    val avatar: String,
)

/** Tabla `categoria`. `usuarioId == null` → categoría del sistema (no se edita). */
@Serializable
data class Categoria(
    val id: String,
    @SerialName("usuario_id") val usuarioId: String? = null,
    val nombre: String,
    val tipo: TipoRegistro,
    val icono: String? = null,
    val color: String? = null,
    val archivada: Boolean = false,
) {
    val esDelSistema: Boolean get() = usuarioId == null
}

@Serializable
data class NuevaCategoria(
    @SerialName("usuario_id") val usuarioId: String,   // obligatorio por RLS: el id del usuario actual
    val nombre: String,
    val tipo: TipoRegistro,
    val icono: String? = null,
    val color: String? = null,
)

/** Tabla `registro`: un gasto o ingreso anotado. */
@Serializable
data class Registro(
    val id: String,
    @SerialName("usuario_id") val usuarioId: String,
    @SerialName("categoria_id") val categoriaId: String,
    @SerialName("planificado_id") val planificadoId: String? = null,
    val tipo: TipoRegistro,
    val monto: BigDecimal,
    @SerialName("medio_pago") val medioPago: MedioPago = MedioPago.EFECTIVO,
    @SerialName("fecha_hora") val fechaHora: Instant,
    val nota: String? = null,
    @SerialName("creado_en") val creadoEn: Instant? = null,
)

@Serializable
data class NuevoRegistro(
    @SerialName("categoria_id") val categoriaId: String,
    val tipo: TipoRegistro,
    val monto: BigDecimal,
    @SerialName("medio_pago") val medioPago: MedioPago = MedioPago.EFECTIVO,
    @SerialName("fecha_hora") val fechaHora: Instant = Instant.now(),
    val nota: String? = null,
    @SerialName("planificado_id") val planificadoId: String? = null,
)

/** Tabla `registro_planificado`: pago o ingreso esperado. */
@Serializable
data class RegistroPlanificado(
    val id: String,
    @SerialName("usuario_id") val usuarioId: String,
    @SerialName("categoria_id") val categoriaId: String,
    val tipo: TipoRegistro,
    val nombre: String,
    val monto: BigDecimal,
    @SerialName("medio_pago") val medioPago: MedioPago = MedioPago.EFECTIVO,
    val frecuencia: Frecuencia,
    @SerialName("proxima_fecha") val proximaFecha: LocalDate,
    val estado: EstadoPlanificado = EstadoPlanificado.ACTIVO,
    @SerialName("creado_en") val creadoEn: Instant? = null,
)

@Serializable
data class NuevoPlanificado(
    @SerialName("categoria_id") val categoriaId: String,
    val tipo: TipoRegistro,
    val nombre: String,
    val monto: BigDecimal,
    @SerialName("medio_pago") val medioPago: MedioPago = MedioPago.EFECTIVO,
    val frecuencia: Frecuencia,
    @SerialName("proxima_fecha") val proximaFecha: LocalDate,
)

/** Tabla `meta`: objetivo de ahorro (solo información, no hay dinero real). */
@Serializable
data class Meta(
    val id: String,
    @SerialName("usuario_id") val usuarioId: String,
    val nombre: String,
    val descripcion: String? = null,
    @SerialName("monto_objetivo") val montoObjetivo: BigDecimal,
    @SerialName("fecha_limite") val fechaLimite: LocalDate? = null,
    val estado: EstadoMeta = EstadoMeta.ACTIVA,
    @SerialName("creado_en") val creadoEn: Instant? = null,
)

@Serializable
data class NuevaMeta(
    val nombre: String,
    val descripcion: String? = null,
    @SerialName("monto_objetivo") val montoObjetivo: BigDecimal,
    @SerialName("fecha_limite") val fechaLimite: LocalDate? = null,
)

/** Tabla `aporte_meta`: ahorro o retiro anotado en una meta. */
@Serializable
data class AporteMeta(
    val id: String,
    @SerialName("meta_id") val metaId: String,
    val tipo: TipoAporte,
    val monto: BigDecimal,
    val fecha: LocalDate,
    val nota: String? = null,
)

@Serializable
data class NuevoAporteMeta(
    @SerialName("meta_id") val metaId: String,
    val tipo: TipoAporte,
    val monto: BigDecimal,
    val fecha: LocalDate,
    val nota: String? = null,
)

/** Tabla `deuda`: "debo" o "me deben". */
@Serializable
data class Deuda(
    val id: String,
    @SerialName("usuario_id") val usuarioId: String,
    val tipo: TipoDeuda,
    val contraparte: String,
    val descripcion: String? = null,
    @SerialName("monto_total") val montoTotal: BigDecimal,
    @SerialName("fecha_inicio") val fechaInicio: LocalDate,
    @SerialName("fecha_limite") val fechaLimite: LocalDate? = null,
    val estado: EstadoDeuda = EstadoDeuda.PENDIENTE,
    @SerialName("fecha_cancelacion") val fechaCancelacion: LocalDate? = null,
)

@Serializable
data class NuevaDeuda(
    val tipo: TipoDeuda,
    val contraparte: String,
    val descripcion: String? = null,
    @SerialName("monto_total") val montoTotal: BigDecimal,
    @SerialName("fecha_inicio") val fechaInicio: LocalDate,
    @SerialName("fecha_limite") val fechaLimite: LocalDate? = null,
)

/** Tabla `abono_deuda`: pago parcial de una deuda. */
@Serializable
data class AbonoDeuda(
    val id: String,
    @SerialName("deuda_id") val deudaId: String,
    val monto: BigDecimal,
    val fecha: LocalDate,
    val nota: String? = null,
)

@Serializable
data class NuevoAbonoDeuda(
    @SerialName("deuda_id") val deudaId: String,
    val monto: BigDecimal,
    val fecha: LocalDate,
    val nota: String? = null,
)

/** Tabla `desafio`. La app solo LEE desafíos; se crean con funciones del servidor (Sprint 4). */
@Serializable
data class Desafio(
    val id: String,
    @SerialName("usuario_id") val usuarioId: String,
    val titulo: String,
    val descripcion: String? = null,
    val periodo: PeriodoDesafio,
    val regla: ReglaDesafio,
    @SerialName("categoria_id") val categoriaId: String? = null,
    @SerialName("monto_limite") val montoLimite: BigDecimal? = null,
    @SerialName("dias_objetivo") val diasObjetivo: Int? = null,
    @SerialName("fecha_inicio") val fechaInicio: LocalDate,
    @SerialName("fecha_fin") val fechaFin: LocalDate,
    @SerialName("xp_recompensa") val xpRecompensa: Int = 50,
    val estado: EstadoDesafio = EstadoDesafio.ACTIVO,
    @SerialName("creado_en") val creadoEn: Instant? = null,
)
