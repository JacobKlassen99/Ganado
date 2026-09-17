package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GoogleSheetsResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "ganado") val ganado: List<List<Any?>>? = null,
    @Json(name = "pesajes") val pesajes: List<List<Any?>>? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "mensaje") val mensaje: String? = null
)

@JsonClass(generateAdapter = true)
data class AddGanadoRequest(
    @Json(name = "action") val action: String = "addGanado",
    @Json(name = "numeroGanado") val numeroGanado: String,
    @Json(name = "sexo") val sexo: String,
    @Json(name = "fechaNacimiento") val fechaNacimiento: String,
    @Json(name = "madre") val madre: String? = null,
    @Json(name = "padre") val padre: String? = null,
    @Json(name = "estado") val estado: String = "Actual"
)

@JsonClass(generateAdapter = true)
data class AddPesajeRequest(
    @Json(name = "action") val action: String = "addPesaje",
    @Json(name = "numeroGanado") val numeroGanado: String,
    @Json(name = "fecha") val fecha: String,
    @Json(name = "peso") val peso: Double,
    @Json(name = "observacion") val observacion: String = ""
)

@JsonClass(generateAdapter = true)
data class ApiResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "mensaje") val mensaje: String? = null,
    @Json(name = "error") val error: String? = null
)
