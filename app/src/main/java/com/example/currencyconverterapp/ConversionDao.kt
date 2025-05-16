package com.example.currencyconverterapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversionDao {

    @Insert
    suspend fun insert(conversion: Conversion): Long

    @Query("SELECT * FROM conversions ORDER BY timestamp DESC")
    fun getAllConversions(): Flow<List<Conversion>>

    @Query("DELETE FROM conversions")
    suspend fun clearAll(): Int

    @Query("SELECT * FROM conversions WHERE fromCurrency = :currency OR toCurrency = :currency ORDER BY timestamp DESC")
    fun getConversionsByCurrency(currency: String): Flow<List<Conversion>>
}