package com.example.currencyconverterapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversions")
data class Conversion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fromCurrency: String,
    val toCurrency: String,
    val amount: Double,
    val convertedAmount: Double,
    val timestamp: Long = System.currentTimeMillis()
)