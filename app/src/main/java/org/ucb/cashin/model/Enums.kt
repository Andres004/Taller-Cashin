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

@Serializable
enum class ReglaDesafio { LIMITE_CATEGORIA, LIMITE_TOTAL, DIAS_REGISTRO }

@Serializable
enum class EstadoDesafio { ACTIVO, CUMPLIDO, FALLIDO, DESCARTADO }

@Serializable
enum class Moneda { BOB, USD }
