package com.enxy.weather.data.network.json.openweathermap.hour

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class HourClouds(
    @SerializedName("all")
    @Expose
    val all: Int
)