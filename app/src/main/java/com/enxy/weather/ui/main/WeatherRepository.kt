package com.enxy.weather.ui.main

import android.util.Log
import com.enxy.weather.BuildConfig
import com.enxy.weather.base.BaseRepository
import com.enxy.weather.data.*
import com.enxy.weather.exception.Failure
import com.enxy.weather.functional.Result
import com.enxy.weather.network.ImageChooser
import com.enxy.weather.network.NetworkService
import com.enxy.weather.network.json.openweathermap.current.CurrentForecastResponse
import com.enxy.weather.network.json.openweathermap.hour.HourForecastResponse
import com.enxy.weather.ui.search.LocationInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class WeatherRepository @Inject constructor(
    private val service: NetworkService,
    private val database: AppDataBase
) :
    BaseRepository() {
    companion object {
        // OpenWeatherMap API URL queries
        const val OPEN_WEATHER_MAP_APPID = BuildConfig.API_KEY_OPEN_WEATHER_MAP
        const val DEFAULT_LANGUAGE = "ENG"
        const val DEFAULT_UNITS = "metric"
        const val THREE_HOUR_WEATHER_COUNT = 8 // 3 x 8 = 24 hour weather forecast
    }

    suspend fun getForecast(locationInfo: LocationInfo): Result<Failure, Forecast> {
        Log.d("WeatherRepository", "getForecast: locationInfo=$locationInfo")
        database.getForecastDao()
            .updateLastOpenedForecast(locationInfo.longitude, locationInfo.latitude)
        val isForecastCached: Boolean = database.getForecastDao()
            .isForecastCached(locationInfo.longitude, locationInfo.latitude)
        Log.d("WeatherRepository", "getForecast: isForecastCached=$isForecastCached")
        if (isForecastCached) {
            val forecast: Forecast = database.getForecastDao()
                .getForecastByLocationName(locationInfo.longitude, locationInfo.latitude)
            Log.d(
                "WeatherRepository",
                "getForecast: forecast.containsValidInfo()=${forecast.containsValidInfo()}"
            )
            if (forecast.containsValidInfo()) {
                return Result.Success(forecast)
            } else {
                val result: Result<Failure, Forecast> = requestForecast(locationInfo)
                if (result is Result.Success) {
                    database.getForecastDao().deleteForecast(forecast)
                    database.getForecastDao().insertForecast(result.success)
                }
                return result
            }
        } else {
            val result: Result<Failure, Forecast> = requestForecast(locationInfo)
            Log.d(
                "WeatherRepository",
                "getForecast: result is Result.Success=${result is Result.Success}"
            )
            if (result is Result.Success) {
                database.getForecastDao().insertForecast(result.success)
            }
            return result
        }
    }

    suspend fun updateForecast(forecast: Forecast): Result<Failure, Forecast> {
        val locationInfo =
            LocationInfo(forecast.locationName, forecast.longitude, forecast.latitude)
        Log.d("WeatherRepository", "updateForecast: locationInfo=$locationInfo")
        val result: Result<Failure, Forecast> = requestForecast(locationInfo)
        if (result is Result.Success) {
            database.getForecastDao().deleteForecast(forecast)
            database.getForecastDao().insertForecast(result.success)
        }
        return result
    }

    suspend fun getLastOpenedForecast(): Result<Failure, Forecast> {
        Log.d(
            "WeatherRepository",
            "getLastOpenedForecast: hasCachedForecasts()=${hasCachedForecasts()}"
        )
        return if (hasCachedForecasts()) {
            return Result.Success(database.getForecastDao().getLastOpenedForecast())
        } else
            Result.Error(Failure.DataNotFoundInCache)
    }

    suspend fun hasCachedForecasts(): Boolean = database.getForecastDao().hasCachedForecasts()

    private suspend fun requestForecast(locationInfo: LocationInfo): Result<Failure, Forecast> {
        val currentForecast: Result<Failure, CurrentForecast> =
            requestCurrentWeatherForecast(locationInfo.longitude, locationInfo.latitude)
        val hourForecast: Result<Failure, HourForecast> =
            requestHourWeatherForecast(locationInfo.longitude, locationInfo.latitude)
        return when {
            currentForecast is Result.Error -> Result.Error(currentForecast.error)
            hourForecast is Result.Error -> Result.Error(hourForecast.error)
            else -> transformToForecast(
                locationInfo = locationInfo,
                currentForecast = (currentForecast as Result.Success).success,
                hourForecast = (hourForecast as Result.Success).success
            )
        }
    }

    private suspend fun requestCurrentWeatherForecast(
        longitude: Double,
        latitude: Double
    ): Result<Failure, CurrentForecast> {
        return safeApiCall(
            call = {
                service.weatherApi().getCurrentForecastAsync(
                    longitude = longitude,
                    latitude = latitude,
                    APPID = OPEN_WEATHER_MAP_APPID,
                    language = DEFAULT_LANGUAGE,
                    units = DEFAULT_UNITS
                )
            },
            transform = ::transformCurrentForecastResponse
        )
    }

    private suspend fun requestHourWeatherForecast(
        longitude: Double,
        latitude: Double
    ): Result<Failure, HourForecast> {
        return safeApiCall(
            call = {
                service.weatherApi().getHourForecastAsync(
                    longitude = longitude,
                    latitude = latitude,
                    APPID = OPEN_WEATHER_MAP_APPID,
                    count = THREE_HOUR_WEATHER_COUNT,
                    language = DEFAULT_LANGUAGE,
                    units = DEFAULT_UNITS
                )
            },
            transform = ::transformHourForecastResponse
        )
    }

    /* This transform function differs from others.
     * It transforms the result of two forecasts (not the response of them) */
    private suspend fun transformToForecast(
        locationInfo: LocationInfo,
        currentForecast: CurrentForecast,
        hourForecast: HourForecast
    ): Result<Failure, Forecast> = withContext(Dispatchers.Default) {
        val locationName = locationInfo.locationName
        val longitude = locationInfo.longitude
        val latitude = locationInfo.latitude
        Result.Success(
            Forecast(
                locationName = locationName,
                longitude = longitude,
                latitude = latitude,
                currentForecast = currentForecast.copy(
                    longitude = longitude,
                    latitude = latitude,
                    locationName = locationName
                ),
                hourForecast = hourForecast.copy(
                    longitude = longitude,
                    latitude = latitude,
                    locationName = locationName
                )
            )
        )
    }

    private fun transformCurrentForecastResponse(currentForecastResponse: CurrentForecastResponse): CurrentForecast {
        val longitude = currentForecastResponse.coord.lon
        val latitude = currentForecastResponse.coord.lat
        val cityName = "${currentForecastResponse.name}, ${currentForecastResponse.sys.country}"
        val temperature: String
        currentForecastResponse.main.temp.roundToInt().let {
            temperature = when {
                it > 0 -> "+$it"
                else -> it.toString()
            }
        }
        val feelsLikeTemperature = currentForecastResponse.main.feelsLike.roundToInt().toString()
        val wind = currentForecastResponse.wind.speed.roundToInt().toString()
        val description = currentForecastResponse.weather[0].description.capitalize()
        val pressure = currentForecastResponse.main.pressure.toString()
        val humidity = currentForecastResponse.main.humidity.toString()
        val imageCode = currentForecastResponse.weather[0].id
        val dayPart: Char
        currentForecastResponse.weather[0].icon.let { dayPart = it[it.length - 1] }
        val imageId = ImageChooser.OpenWeatherMap.getImageIdCurrentForecast(imageCode, dayPart)
        return CurrentForecast(
            locationName = cityName,
            longitude = longitude,
            latitude = latitude,
            temperature = temperature,
            description = description,
            feelsLikeTemperature = feelsLikeTemperature,
            wind = wind,
            pressure = pressure,
            humidity = humidity,
            imageId = imageId
        )
    }

    private fun transformHourForecastResponse(hourForecastResponse: HourForecastResponse): HourForecast {
        val hourList = ArrayList<Hour>(hourForecastResponse.list.size)
        val longitude = hourForecastResponse.city.coord.lon
        val latitude = hourForecastResponse.city.coord.lat
        val locationName = "${hourForecastResponse.city.name}, ${hourForecastResponse.city.country}"
        for (hourListItem in hourForecastResponse.list) {
            val temperature: String
            hourListItem.main.temp.roundToInt().let {
                temperature = when {
                    it > 0 -> "+$it"
                    else -> it.toString()
                }
            }
            val time = hourListItem.dtTxt.substring(11, 16)
            val imageCode = hourListItem.weather[0].id
            val dayPart: Char
            hourListItem.weather[0].icon.let {
                dayPart = it[it.length - 1]
            }
            val imageId = ImageChooser.OpenWeatherMap.getImageIdHourForecast(imageCode, dayPart)
            hourList.add(Hour(temperature = temperature, time = time, imageId = imageId))
        }
        return HourForecast(
            locationName = locationName,
            longitude = longitude,
            latitude = latitude,
            hourArrayList = hourList
        )
    }
}