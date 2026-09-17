package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.GanadoDao
import com.example.data.model.GanadoEntity
import com.example.data.model.PesajeEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [GanadoEntity::class, PesajeEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ganadoDao(): GanadoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "registro_ganado_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.ganadoDao())
                    }
                }
            }
        }

        suspend fun populateInitialData(dao: GanadoDao) {
            if (dao.getGanadoCount() > 0) return

            val sampleGanado = listOf(
                GanadoEntity(
                    numeroGanado = "TORO-01",
                    sexo = "Macho",
                    fechaNacimiento = "2022-03-15",
                    madre = null,
                    padre = null,
                    estado = "Activo"
                ),
                GanadoEntity(
                    numeroGanado = "VACA-101",
                    sexo = "Hembra",
                    fechaNacimiento = "2021-08-10",
                    madre = null,
                    padre = null,
                    estado = "Activo"
                ),
                GanadoEntity(
                    numeroGanado = "VACA-102",
                    sexo = "Hembra",
                    fechaNacimiento = "2022-01-20",
                    madre = null,
                    padre = null,
                    estado = "Gestante"
                ),
                GanadoEntity(
                    numeroGanado = "TER-201",
                    sexo = "Hembra",
                    fechaNacimiento = "2023-11-05",
                    madre = "VACA-101",
                    padre = "TORO-01",
                    estado = "Activo"
                ),
                GanadoEntity(
                    numeroGanado = "TER-202",
                    sexo = "Macho",
                    fechaNacimiento = "2024-01-14",
                    madre = "VACA-102",
                    padre = "TORO-01",
                    estado = "En engorde"
                ),
                GanadoEntity(
                    numeroGanado = "NOV-301",
                    sexo = "Macho",
                    fechaNacimiento = "2023-04-10",
                    madre = "VACA-101",
                    padre = "TORO-01",
                    estado = "Vendido"
                )
            )
            dao.insertAllGanado(sampleGanado)

            val samplePesajes = listOf(
                // TORO-01
                PesajeEntity(numeroGanado = "TORO-01", fecha = "2022-03-15", peso = 42.0, observacion = "Peso inicial de nacimiento"),
                PesajeEntity(numeroGanado = "TORO-01", fecha = "2022-09-15", peso = 215.0, observacion = "Pesaje al destete"),
                PesajeEntity(numeroGanado = "TORO-01", fecha = "2023-03-20", peso = 380.0, observacion = "Control anual"),
                PesajeEntity(numeroGanado = "TORO-01", fecha = "2024-03-18", peso = 590.0, observacion = "Seleccionado como reproductor"),
                PesajeEntity(numeroGanado = "TORO-01", fecha = "2024-09-02", peso = 645.0, observacion = "Excelente condición corporal"),

                // VACA-101
                PesajeEntity(numeroGanado = "VACA-101", fecha = "2021-08-10", peso = 38.0, observacion = "Peso inicial"),
                PesajeEntity(numeroGanado = "VACA-101", fecha = "2022-02-12", peso = 190.0, observacion = "Destete"),
                PesajeEntity(numeroGanado = "VACA-101", fecha = "2023-01-15", peso = 410.0, observacion = "Primer servicio"),
                PesajeEntity(numeroGanado = "VACA-101", fecha = "2024-02-10", peso = 485.0, observacion = "Post-parto"),
                PesajeEntity(numeroGanado = "VACA-101", fecha = "2024-08-15", peso = 512.0, observacion = "Buena producción láctea"),

                // VACA-102
                PesajeEntity(numeroGanado = "VACA-102", fecha = "2022-01-20", peso = 36.0, observacion = "Peso inicial"),
                PesajeEntity(numeroGanado = "VACA-102", fecha = "2022-07-25", peso = 185.0, observacion = "Destete"),
                PesajeEntity(numeroGanado = "VACA-102", fecha = "2023-05-10", peso = 420.0, observacion = "Confirmación preñez"),
                PesajeEntity(numeroGanado = "VACA-102", fecha = "2024-06-12", peso = 478.0, observacion = "Control gestacional"),

                // TER-201
                PesajeEntity(numeroGanado = "TER-201", fecha = "2023-11-05", peso = 39.0, observacion = "Nacimiento - Cría de VACA-101"),
                PesajeEntity(numeroGanado = "TER-201", fecha = "2024-02-10", peso = 112.0, observacion = "Primer trimestre"),
                PesajeEntity(numeroGanado = "TER-201", fecha = "2024-05-15", peso = 195.0, observacion = "Destete con buen desarrollo"),
                PesajeEntity(numeroGanado = "TER-201", fecha = "2024-08-20", peso = 260.0, observacion = "Crecimiento constante"),

                // TER-202
                PesajeEntity(numeroGanado = "TER-202", fecha = "2024-01-14", peso = 41.5, observacion = "Nacimiento - Cría de VACA-102"),
                PesajeEntity(numeroGanado = "TER-202", fecha = "2024-04-18", peso = 125.0, observacion = "Desarrollo adecuado"),
                PesajeEntity(numeroGanado = "TER-202", fecha = "2024-07-22", peso = 210.0, observacion = "Traslado a corral de engorde"),

                // NOV-301
                PesajeEntity(numeroGanado = "NOV-301", fecha = "2023-04-10", peso = 40.0, observacion = "Nacimiento"),
                PesajeEntity(numeroGanado = "NOV-301", fecha = "2023-10-15", peso = 220.0, observacion = "Destete"),
                PesajeEntity(numeroGanado = "NOV-301", fecha = "2024-03-30", peso = 430.0, observacion = "Pesaje previo a comercialización")
            )
            dao.insertAllPesajes(samplePesajes)
        }
    }
}
