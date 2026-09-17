package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Ganado con estructura idéntica a la hoja GANADO en Google Sheets:
 * Nº Ganado | Sexo | Fecha nacimiento | Madre | Padre | Estado
 */
@Entity(tableName = "ganado")
data class GanadoEntity(
    @PrimaryKey
    @ColumnInfo(name = "numero_ganado")
    val numeroGanado: String, // Nº Ganado

    @ColumnInfo(name = "sexo")
    val sexo: String, // "Macho" o "Hembra"

    @ColumnInfo(name = "fecha_nacimiento")
    val fechaNacimiento: String, // Formato "YYYY-MM-DD"

    @ColumnInfo(name = "madre")
    val madre: String? = null, // Nº Ganado de la Madre (solo hembra)

    @ColumnInfo(name = "padre")
    val padre: String? = null, // Nº Ganado del Padre (solo macho)

    @ColumnInfo(name = "estado")
    val estado: String = "Activo" // "Activo", "Vendido", "Fallecido", "En engorde", "Gestante", etc.
)
