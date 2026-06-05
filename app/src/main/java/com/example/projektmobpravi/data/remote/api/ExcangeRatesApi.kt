package com.example.projektmobpravi.data.remote.api

import com.example.projektmobpravi.data.remote.dto.ExchangeRatesResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface ExchangeRatesApi {

    @GET("v6/{apiKey}/latest/{baseCurrency}")
    suspend fun getLatestRates(
        @Path("apiKey") apiKey: String,
        @Path("baseCurrency") baseCurrency: String = "EUR"
    ): ExchangeRatesResponse
}