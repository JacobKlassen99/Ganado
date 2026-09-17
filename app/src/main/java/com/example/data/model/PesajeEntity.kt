package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Pesaje con estructura idéntica a la hoja PESAJES en Google Sheets:
 * Nº Ganado | Fecha | Peso | Observación
 */
@Entity(
    tableName = "pesajes",
    indices = [Index(value = ["numero_ganado"])]
)
data class PesajeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "numero_ganado")
    val numeroGanado: String, // Nº Ganado

    @ColumnInfo(name = "fecha")
    val fecha: String, // Formato "YYYY-MM-DD"

    @ColumnInfo(name = "peso")
    val peso: Double, // Peso en kg

    @ColumnInfo(name = "observacion")
    val observacion: String = "" // Observación
)
