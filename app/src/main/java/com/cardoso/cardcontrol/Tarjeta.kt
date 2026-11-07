package com.cardoso.cardcontrol

data class Tarjeta(
    val id: Int,
    val banco: String,
    val ultimosDigitos: String,
    val limiteCredito: Double,
    val usoReportadoPorcentaje: Double,
    val colorHex: String // Para que cada tarjeta tenga su color real (Morado Nu, etc.)
)