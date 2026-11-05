package org.ucb.cashin.model

import kotlinx.serialization.Serializable

// Valores permitidos: deben coincidir EXACTAMENTE con los CHECK de supabase/migrations/001_esquema.sql.

@Serializable
enum class TipoRegistro { GASTO, INGRESO }

@Serializable
enum class MedioPago { EFECTIVO, QR, TARJETA, TRANSFERENCIA, OTRO }

@Serializable
enum class Frecuencia { UNICA, SEMANAL, QUINCENAL, MENSUAL, ANUAL }

@Serializable
enum class EstadoPlanificado { ACTIVO, PAUSADO, CANCELADO }

@Serializable
enum class EstadoMeta { ACTIVA, COMPLETADA, CANCELADA }

@Serializable
enum class TipoAporte { AHORRO, RETIRO }

@Serializable
enum class TipoDeuda { DEBO, ME_DEBEN }

@Serializable
enum class EstadoDeuda { PENDIENTE, PAGADA }

@Serializable
enum class PeriodoDesafio { SEMANAL, MENSUAL }

/**
 * Qué mide un desafío. Las tres últimas (004_desafios_automaticos.sql) no necesitan historia:
 * cuántos movimientos anotar, cuántos días sin gastar y cuántos ahorros anotar en metas.
 * En todas las que cuentan algo, el objetivo va en `dias_objetivo`.
 */
@Serializable
enum class ReglaDesafio { LIMITE_CATEGORIA, LIMITE_TOTAL, DIAS_REGISTRO, CANTIDAD_REGISTROS, DIAS_SIN_GASTO, AHORRO_META }

@Serializable
enum class EstadoDesafio { ACTIVO, CUMPLIDO, FALLIDO, DESCARTADO }

@Serializable
enum class Moneda { BOB, USD }
