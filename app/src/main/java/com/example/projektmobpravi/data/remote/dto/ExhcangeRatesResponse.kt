package com.example.projektmobpravi.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ExchangeRatesResponse(
    @SerializedName("base_code")
    val base: String,
    @SerializedName("conversion_rates")
    val rates: Map<String, Double>
)