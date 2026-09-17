package com.example.data.repository

import android.util.Log
import com.example.data.model.GanadoEntity
import com.example.data.model.PesajeEntity
import com.example.data.remote.AddGanadoRequest
import com.example.data.remote.AddPesajeRequest
import com.example.data.remote.GoogleSheetsApiService
import com.example.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

data class PesajeUiModel(
    val id: Long,
    val numeroGanado: String,
    val fecha: String,
    val peso: Double,
    val aumentoAnterior: Double?,
    val aumentoTotal: Double?,
    val observacion: String
)

data class GanadoFichaMetrics(
    val primerPeso: Double?,
    val pesoAnterior: Double?,
    val pesoActual: Double?,
    val aumentoDesdeAnterior: Double?,
    val aumentoTotal: Double?,
    val totalPesajes: Int
)

class GanadoRepository(
    private val apiService: GoogleSheetsApiService = RetrofitClient.apiService
) {
    private val TAG = "GanadoRepository"

    private val _allGanado = MutableStateFlow<List<GanadoEntity>>(emptyList())
    val allGanado: Flow<List<GanadoEntity>> = _allGanado.asStateFlow()

    private val _allPesajes = MutableStateFlow<List<PesajeEntity>>(emptyList())
    val allPesajes: Flow<List<PesajeEntity>> = _allPesajes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: Flow<Boolean> = _isLoading.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: Flow<String?> = _syncError.asStateFlow()

    private fun formatCleanDate(rawDate: String): String {
        val trimmed = rawDate.trim()
        return try {
            if (trimmed.contains("T")) {
                // Formato ISO: "2025-01-10T04:00:00.000Z" -> "2025-01-10"
                trimmed.substringBefore("T")
            } else {
                trimmed
            }
        } catch (e: Exception) {
            trimmed
        }
    }

    private fun cleanNumber(value: Any?): String {
        if (value == null) return ""
        val s = value.toString().trim()
        return if (s.endsWith(".0")) s.substringBefore(".0") else s
    }

    suspend fun refreshData() = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _syncError.value = null
        try {
            val response = apiService.getSheetData()
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    val ganadoEntities = mutableListOf<GanadoEntity>()
                    val rawGanadoRows = body.ganado ?: emptyList()

                    // Omitir cabecera si la primera fila contiene nombres de columnas
                    val startGanadoIdx = if (rawGanadoRows.isNotEmpty() &&
                        rawGanadoRows[0].isNotEmpty() &&
                        rawGanadoRows[0][0]?.toString()?.trim()?.equals("ID", ignoreCase = true) == true
                    ) 1 else 0

                    for (i in startGanadoIdx until rawGanadoRows.size) {
                        val row = rawGanadoRows[i]
                        if (row.size >= 2) {
                            val numero = cleanNumber(row.getOrNull(1))
                            if (numero.isNotBlank()) {
                                val sexo = row.getOrNull(2)?.toString()?.trim() ?: "Macho"
                                val fecha = formatCleanDate(row.getOrNull(3)?.toString() ?: "")
                                val madre = cleanNumber(row.getOrNull(4)).takeIf { it.isNotBlank() }
                                val padre = cleanNumber(row.getOrNull(5)).takeIf { it.isNotBlank() }
                                val estado = row.getOrNull(6)?.toString()?.trim()?.takeIf { it.isNotBlank() } ?: "Actual"

                                ganadoEntities.add(
                                    GanadoEntity(
                                        numeroGanado = numero.uppercase(),
                                        sexo = sexo,
                                        fechaNacimiento = fecha,
                                        madre = madre,
                                        padre = padre,
                                        estado = estado
                                    )
                                )
                            }
                        }
                    }

                    val pesajeEntities = mutableListOf<PesajeEntity>()
                    val rawPesajesRows = body.pesajes ?: emptyList()

                    val startPesajesIdx = if (rawPesajesRows.isNotEmpty() &&
                        rawPesajesRows[0].isNotEmpty() &&
                        rawPesajesRows[0][0]?.toString()?.trim()?.equals("ID", ignoreCase = true) == true
                    ) 1 else 0

                    for (i in startPesajesIdx until rawPesajesRows.size) {
                        val row = rawPesajesRows[i]
                        if (row.size >= 4) {
                            val numero = cleanNumber(row.getOrNull(1))
                            if (numero.isNotBlank()) {
                                val fecha = formatCleanDate(row.getOrNull(2)?.toString() ?: "")
                                val pesoRaw = row.getOrNull(3)?.toString()?.trim()?.toDoubleOrNull() ?: 0.0
                                val observacion = row.getOrNull(4)?.toString()?.trim() ?: ""

                                pesajeEntities.add(
                                    PesajeEntity(
                                        id = (i).toLong(),
                                        numeroGanado = numero.uppercase(),
                                        fecha = fecha,
                                        peso = pesoRaw,
                                        observacion = observacion
                                    )
                                )
                            }
                        }
                    }

                    _allGanado.value = ganadoEntities
                    _allPesajes.value = pesajeEntities
                    Log.d(TAG, "Google Sheets sincronizado exitosamente: ${ganadoEntities.size} ganado, ${pesajeEntities.size} pesajes")
                } else {
                    _syncError.value = body.error ?: "Error devuelto por Google Sheets"
                    Log.e(TAG, "Error en Google Sheets: ${body.error}")
                }
            } else {
                val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                _syncError.value = errorMsg
                Log.e(TAG, errorMsg)
            }
        } catch (e: Exception) {
            _syncError.value = e.localizedMessage ?: "Error de conexión con Google Sheets"
            Log.e(TAG, "Fallo al consultar Google Sheets", e)
        } finally {
            _isLoading.value = false
        }
    }

    fun getGanado(numero: String): Flow<GanadoEntity?> {
        return allGanado.map { list ->
            list.find { it.numeroGanado.equals(numero.trim(), ignoreCase = true) }
        }
    }

    fun getGanadoBySexo(sexo: String): Flow<List<GanadoEntity>> {
        return allGanado.map { list ->
            list.filter { it.sexo.equals(sexo.trim(), ignoreCase = true) }
        }
    }

    fun getChildren(parentNumero: String): Flow<List<GanadoEntity>> {
        return allGanado.map { list ->
            list.filter {
                it.madre.equals(parentNumero.trim(), ignoreCase = true) ||
                it.padre.equals(parentNumero.trim(), ignoreCase = true)
            }
        }
    }

    suspend fun insertGanado(ganado: GanadoEntity): Boolean = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _syncError.value = null
        try {
            val req = AddGanadoRequest(
                action = "addGanado",
                numeroGanado = ganado.numeroGanado,
                sexo = ganado.sexo,
                fechaNacimiento = ganado.fechaNacimiento,
                madre = ganado.madre ?: "",
                padre = ganado.padre ?: "",
                estado = ganado.estado
            )
            val res = apiService.addGanado(req)
            if (res.isSuccessful && (res.body()?.success == true || res.body()?.mensaje != null)) {
                // Inmediatamente recargar los datos actualizados desde Google Sheets
                refreshData()
                true
            } else {
                _syncError.value = res.body()?.error ?: "No se pudo guardar en Google Sheets"
                false
            }
        } catch (e: Exception) {
            _syncError.value = e.localizedMessage ?: "Error al guardar ganado en Google Sheets"
            Log.e(TAG, "Error insertGanado", e)
            false
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun insertPesaje(pesaje: PesajeEntity): Boolean = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _syncError.value = null
        try {
            val req = AddPesajeRequest(
                action = "addPesaje",
                numeroGanado = pesaje.numeroGanado,
                fecha = pesaje.fecha,
                peso = pesaje.peso,
                observacion = pesaje.observacion
            )
            val res = apiService.addPesaje(req)
            if (res.isSuccessful && (res.body()?.success == true || res.body()?.mensaje != null)) {
                // Inmediatamente recargar los datos actualizados desde Google Sheets
                refreshData()
                true
            } else {
                _syncError.value = res.body()?.error ?: "No se pudo guardar pesaje en Google Sheets"
                false
            }
        } catch (e: Exception) {
            _syncError.value = e.localizedMessage ?: "Error al registrar pesaje en Google Sheets"
            Log.e(TAG, "Error insertPesaje", e)
            false
        } finally {
            _isLoading.value = false
        }
    }

    fun deleteGanado(ganado: GanadoEntity) {
        _allGanado.value = _allGanado.value.filterNot { it.numeroGanado.equals(ganado.numeroGanado, ignoreCase = true) }
        _allPesajes.value = _allPesajes.value.filterNot { it.numeroGanado.equals(ganado.numeroGanado, ignoreCase = true) }
    }

    fun deletePesaje(pesaje: PesajeEntity) {
        _allPesajes.value = _allPesajes.value.filterNot { it.id == pesaje.id }
    }

    fun getPesajesUiForGanado(numero: String): Flow<List<PesajeUiModel>> {
        return allPesajes.map { list ->
            val forAnimal = list.filter { it.numeroGanado.equals(numero.trim(), ignoreCase = true) }
            val sorted = forAnimal.sortedWith(compareBy({ it.fecha }, { it.id }))
            val firstPeso = sorted.firstOrNull()?.peso

            sorted.mapIndexed { index, pesaje ->
                val prevPeso = if (index > 0) sorted[index - 1].peso else null
                val aumentoAnterior = prevPeso?.let { pesaje.peso - it }
                val aumentoTotal = firstPeso?.let { pesaje.peso - it }

                PesajeUiModel(
                    id = pesaje.id,
                    numeroGanado = pesaje.numeroGanado,
                    fecha = pesaje.fecha,
                    peso = pesaje.peso,
                    aumentoAnterior = aumentoAnterior,
                    aumentoTotal = aumentoTotal,
                    observacion = pesaje.observacion
                )
            }
        }
    }

    fun getMetricsForGanado(numero: String): Flow<GanadoFichaMetrics> {
        return allPesajes.map { list ->
            val forAnimal = list.filter { it.numeroGanado.equals(numero.trim(), ignoreCase = true) }
            calculateMetrics(forAnimal)
        }
    }

    companion object {
        fun calculateMetrics(pesajes: List<PesajeEntity>): GanadoFichaMetrics {
            val sorted = pesajes.sortedWith(compareBy({ it.fecha }, { it.id }))
            if (sorted.isEmpty()) {
                return GanadoFichaMetrics(
                    primerPeso = null,
                    pesoAnterior = null,
                    pesoActual = null,
                    aumentoDesdeAnterior = null,
                    aumentoTotal = null,
                    totalPesajes = 0
                )
            }

            val primerPeso = sorted.first().peso
            val pesoActual = sorted.last().peso
            val pesoAnterior = if (sorted.size > 1) sorted[sorted.size - 2].peso else null
            val aumentoDesdeAnterior = pesoAnterior?.let { pesoActual - it }
            val aumentoTotal = pesoActual - primerPeso

            return GanadoFichaMetrics(
                primerPeso = primerPeso,
                pesoAnterior = pesoAnterior,
                pesoActual = pesoActual,
                aumentoDesdeAnterior = aumentoDesdeAnterior,
                aumentoTotal = aumentoTotal,
                totalPesajes = sorted.size
            )
        }

        fun calculateAge(fechaNacimientoStr: String): String {
            return try {
                val cleaned = fechaNacimientoStr.trim().substringBefore("T")
                val birthDate = if (cleaned.contains("/")) {
                    val parts = cleaned.split("/")
                    if (parts.size == 3) {
                        val d = parts[0].padStart(2, '0')
                        val m = parts[1].padStart(2, '0')
                        val y = parts[2]
                        LocalDate.parse("$y-$m-$d")
                    } else {
                        LocalDate.parse(cleaned)
                    }
                } else {
                    LocalDate.parse(cleaned)
                }

                val today = LocalDate.now()
                if (birthDate.isAfter(today)) {
                    return "Fecha futura"
                }
                val period = Period.between(birthDate, today)
                val years = period.years
                val months = period.months
                val days = period.days

                buildString {
                    when {
                        years > 0 -> {
                            append("$years ${if (years == 1) "año" else "años"}")
                            if (months > 0) {
                                append(", $months ${if (months == 1) "mes" else "meses"}")
                            }
                        }
                        months > 0 -> {
                            append("$months ${if (months == 1) "mes" else "meses"}")
                            if (days > 0) {
                                append(", $days ${if (days == 1) "día" else "días"}")
                            }
                        }
                        else -> {
                            append("$days ${if (days == 1) "día" else "días"}")
                        }
                    }
                }
            } catch (e: Exception) {
                fechaNacimientoStr.substringBefore("T")
            }
        }
    }
}
