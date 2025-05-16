package com.example.currencyconverterapp

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ExchangeRateApiService {
    @GET("v6/32174a6c34ba0b8d272701a1/latest/{baseCurrency}")
    fun getExchangeRates(@Path("baseCurrency") baseCurrency: String): Call<ExchangeRateResponse>
}

data class ExchangeRateResponse(
    val base_code: String,
    val conversion_rates: Map<String, Double>
)
