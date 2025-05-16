package com.example.currencyconverterapp

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface MetalRatesApiService {
    @GET("v1/latest")
    fun getMetalRates(
        @Query("api_key") apiKey: String,
        @Query("base") baseCurrency: String,
        @Query("currencies") currencies: String
    ): Call<MetalRatesResponse>
}

data class MetalRatesResponse(
    val success: Boolean,
    val base: String,
    val date: String,
    val rates: Map<String, Double>
)