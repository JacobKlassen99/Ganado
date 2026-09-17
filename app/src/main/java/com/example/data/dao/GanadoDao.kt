package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.GanadoEntity
import com.example.data.model.PesajeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GanadoDao {
    // --- Ganado ---
    @Query("SELECT * FROM ganado ORDER BY numero_ganado ASC")
    fun getAllGanado(): Flow<List<GanadoEntity>>

    @Query("SELECT * FROM ganado WHERE numero_ganado = :numeroGanado LIMIT 1")
    fun getGanadoByNumero(numeroGanado: String): Flow<GanadoEntity?>

    @Query("SELECT * FROM ganado WHERE numero_ganado = :numeroGanado LIMIT 1")
    suspend fun getGanadoByNumeroOnce(numeroGanado: String): GanadoEntity?

    @Query("SELECT * FROM ganado WHERE sexo = :sexo ORDER BY numero_ganado ASC")
    fun getGanadoBySexo(sexo: String): Flow<List<GanadoEntity>>

    @Query("SELECT * FROM ganado WHERE madre = :parentNumero OR padre = :parentNumero ORDER BY fecha_nacimiento DESC")
    fun getChildren(parentNumero: String): Flow<List<GanadoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGanado(ganado: GanadoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllGanado(ganadoList: List<GanadoEntity>)

    @Update
    suspend fun updateGanado(ganado: GanadoEntity)

    @Delete
    suspend fun deleteGanado(ganado: GanadoEntity)

    @Query("SELECT COUNT(*) FROM ganado")
    suspend fun getGanadoCount(): Int

    // --- Pesajes ---
    @Query("SELECT * FROM pesajes WHERE numero_ganado = :numeroGanado ORDER BY fecha ASC, id ASC")
    fun getPesajesForGanado(numeroGanado: String): Flow<List<PesajeEntity>>

    @Query("SELECT * FROM pesajes WHERE numero_ganado = :numeroGanado ORDER BY fecha ASC, id ASC")
    suspend fun getPesajesForGanadoOnce(numeroGanado: String): List<PesajeEntity>

    @Query("SELECT * FROM pesajes ORDER BY fecha DESC, id DESC")
    fun getAllPesajes(): Flow<List<PesajeEntity>>

    @Query("SELECT * FROM pesajes ORDER BY fecha ASC, id ASC")
    suspend fun getAllPesajesOnce(): List<PesajeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPesaje(pesaje: PesajeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPesajes(pesajes: List<PesajeEntity>)

    @Delete
    suspend fun deletePesaje(pesaje: PesajeEntity)

    @Query("DELETE FROM pesajes WHERE numero_ganado = :numeroGanado")
    suspend fun deletePesajesByGanado(numeroGanado: String)
}
