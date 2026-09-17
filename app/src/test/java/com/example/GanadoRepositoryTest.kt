package com.example

import com.example.data.model.PesajeEntity
import com.example.data.repository.GanadoRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GanadoRepositoryTest {

    @Test
    fun testWeightMetricsCalculation_empty() {
        val metrics = GanadoRepository.calculateMetrics(emptyList())
        assertNull(metrics.pesoActual)
        assertNull(metrics.primerPeso)
        assertNull(metrics.pesoAnterior)
        assertNull(metrics.aumentoDesdeAnterior)
        assertNull(metrics.aumentoTotal)
        assertEquals(0, metrics.totalPesajes)
    }

    @Test
    fun testWeightMetricsCalculation_singleEntry() {
        val pesajes = listOf(
            PesajeEntity(id = 1, numeroGanado = "VACA-1", fecha = "2024-01-01", peso = 45.0, observacion = "Nacimiento")
        )
        val metrics = GanadoRepository.calculateMetrics(pesajes)
        assertEquals(45.0, metrics.pesoActual!!, 0.01)
        assertEquals(45.0, metrics.primerPeso!!, 0.01)
        assertNull(metrics.pesoAnterior)
        assertNull(metrics.aumentoDesdeAnterior)
        assertEquals(0.0, metrics.aumentoTotal!!, 0.01)
        assertEquals(1, metrics.totalPesajes)
    }

    @Test
    fun testWeightMetricsCalculation_multipleEntries() {
        val pesajes = listOf(
            PesajeEntity(id = 1, numeroGanado = "TORO-1", fecha = "2023-01-01", peso = 40.0, observacion = "Nacimiento"),
            PesajeEntity(id = 2, numeroGanado = "TORO-1", fecha = "2023-07-01", peso = 210.0, observacion = "Destete"),
            PesajeEntity(id = 3, numeroGanado = "TORO-1", fecha = "2024-01-01", peso = 390.0, observacion = "Control")
        )
        val metrics = GanadoRepository.calculateMetrics(pesajes)
        assertEquals(390.0, metrics.pesoActual!!, 0.01)
        assertEquals(210.0, metrics.pesoAnterior!!, 0.01)
        assertEquals(180.0, metrics.aumentoDesdeAnterior!!, 0.01)
        assertEquals(40.0, metrics.primerPeso!!, 0.01)
        assertEquals(350.0, metrics.aumentoTotal!!, 0.01)
        assertEquals(3, metrics.totalPesajes)
    }

    @Test
    fun testCalculateAge() {
        val age = GanadoRepository.calculateAge("2020-01-01")
        assertNotNull(age)
        assert(age.contains("año") || age.contains("mes"))
    }
}
