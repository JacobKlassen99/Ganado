package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.GanadoEntity
import com.example.data.model.PesajeEntity
import com.example.data.repository.GanadoFichaMetrics
import com.example.data.repository.GanadoRepository
import com.example.data.repository.PesajeUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GanadoUiState(
    val ganadoList: List<GanadoEntity> = emptyList(),
    val filteredGanadoList: List<GanadoEntity> = emptyList(),
    val searchQuery: String = "",
    val filterSexo: String = "Todos", // "Todos", "Macho", "Hembra"
    val filterEstado: String = "Todos", // "Todos", "Actual", "Activo", "Gestante", "En engorde", "Vendido", etc.
    val latestWeights: Map<String, Double> = emptyMap(),
    val totalWeightGains: Map<String, Double> = emptyMap(),
    val totalCount: Int = 0,
    val machosCount: Int = 0,
    val hembrasCount: Int = 0,
    val activosCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class GanadoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GanadoRepository = GanadoRepository()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterSexo = MutableStateFlow("Todos")
    val filterSexo: StateFlow<String> = _filterSexo.asStateFlow()

    private val _filterEstado = MutableStateFlow("Todos")
    val filterEstado: StateFlow<String> = _filterEstado.asStateFlow()

    init {
        // Cargar inmediatamente desde Google Sheets
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            repository.refreshData()
        }
    }

    val allGanado: StateFlow<List<GanadoEntity>> = repository.allGanado
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allPesajes: StateFlow<List<PesajeEntity>> = repository.allPesajes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI state combinando datos de Google Sheets, pesajes, filtros y búsqueda
    val uiState: StateFlow<GanadoUiState> = combine(
        repository.allGanado,
        repository.allPesajes,
        repository.isLoading,
        repository.syncError,
        _searchQuery,
        _filterSexo,
        _filterEstado
    ) { params: Array<Any?> ->
        val ganadoList = params[0] as List<GanadoEntity>
        val pesajesList = params[1] as List<PesajeEntity>
        val loading = params[2] as Boolean
        val errorMsg = params[3] as String?
        val query = params[4] as String
        val sexoFilter = params[5] as String
        val estadoFilter = params[6] as String

        // Mapear último peso y ganancia total por ganado
        val pesajesByGanado = pesajesList.groupBy { it.numeroGanado }
        val latestWeightsMap = mutableMapOf<String, Double>()
        val gainsMap = mutableMapOf<String, Double>()

        for ((numero, list) in pesajesByGanado) {
            val sorted = list.sortedWith(compareBy({ it.fecha }, { it.id }))
            if (sorted.isNotEmpty()) {
                val latest = sorted.last().peso
                val first = sorted.first().peso
                latestWeightsMap[numero] = latest
                gainsMap[numero] = latest - first
            }
        }

        val filtered = ganadoList.filter { ganado ->
            val matchesQuery = query.isBlank() ||
                    ganado.numeroGanado.contains(query.trim(), ignoreCase = true)

            val matchesSexo = sexoFilter == "Todos" ||
                    ganado.sexo.equals(sexoFilter, ignoreCase = true)

            val matchesEstado = estadoFilter == "Todos" ||
                    ganado.estado.equals(estadoFilter, ignoreCase = true)

            matchesQuery && matchesSexo && matchesEstado
        }

        GanadoUiState(
            ganadoList = ganadoList,
            filteredGanadoList = filtered,
            searchQuery = query,
            filterSexo = sexoFilter,
            filterEstado = estadoFilter,
            latestWeights = latestWeightsMap,
            totalWeightGains = gainsMap,
            totalCount = ganadoList.size,
            machosCount = ganadoList.count { it.sexo.equals("Macho", ignoreCase = true) },
            hembrasCount = ganadoList.count { it.sexo.equals("Hembra", ignoreCase = true) },
            activosCount = ganadoList.count { it.estado.equals("Actual", ignoreCase = true) || it.estado.equals("Activo", ignoreCase = true) },
            isLoading = loading,
            errorMessage = errorMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GanadoUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onSexoFilterSelected(sexo: String) {
        _filterSexo.value = sexo
    }

    fun onEstadoFilterSelected(estado: String) {
        _filterEstado.value = estado
    }

    fun getGanado(numero: String): Flow<GanadoEntity?> = repository.getGanado(numero)

    fun getPesajesUi(numero: String): Flow<List<PesajeUiModel>> = repository.getPesajesUiForGanado(numero)

    fun getMetrics(numero: String): Flow<GanadoFichaMetrics> = repository.getMetricsForGanado(numero)

    fun getChildren(numero: String): Flow<List<GanadoEntity>> = repository.getChildren(numero)

    fun addGanado(
        numeroGanado: String,
        sexo: String,
        fechaNacimiento: String,
        madre: String?,
        padre: String?,
        estado: String,
        pesoInicial: Double?,
        observacion: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val entity = GanadoEntity(
                numeroGanado = numeroGanado.trim().uppercase(),
                sexo = sexo,
                fechaNacimiento = fechaNacimiento,
                madre = madre?.ifBlank { null },
                padre = padre?.ifBlank { null },
                estado = estado
            )
            val success = repository.insertGanado(entity)

            // Si se ingresó peso inicial, registrar también en Google Sheets hoja PESAJES
            if (success && pesoInicial != null && pesoInicial > 0) {
                val obs = if (observacion.isNotBlank()) observacion else "Peso inicial"
                val pesaje = PesajeEntity(
                    numeroGanado = entity.numeroGanado,
                    fecha = fechaNacimiento,
                    peso = pesoInicial,
                    observacion = obs
                )
                repository.insertPesaje(pesaje)
            }

            if (success) {
                onSuccess()
            } else {
                onError("No se pudo guardar en Google Sheets.")
            }
        }
    }

    fun addPesaje(
        numeroGanado: String,
        fecha: String,
        peso: Double,
        observacion: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val pesaje = PesajeEntity(
                numeroGanado = numeroGanado,
                fecha = fecha,
                peso = peso,
                observacion = observacion
            )
            val success = repository.insertPesaje(pesaje)
            if (success) {
                onSuccess()
            } else {
                onError("No se pudo registrar el pesaje en Google Sheets.")
            }
        }
    }

    fun deletePesaje(pesajeId: Long, numeroGanado: String, fecha: String, peso: Double, observacion: String) {
        val pesaje = PesajeEntity(
            id = pesajeId,
            numeroGanado = numeroGanado,
            fecha = fecha,
            peso = peso,
            observacion = observacion
        )
        repository.deletePesaje(pesaje)
    }

    fun deleteGanado(ganado: GanadoEntity) {
        repository.deleteGanado(ganado)
    }
}
