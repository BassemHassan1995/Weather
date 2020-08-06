package com.enxy.weather.data.repository

import com.enxy.weather.BuildConfig
import com.enxy.weather.base.NetworkRepository
import com.enxy.weather.data.entity.Location
import com.enxy.weather.data.network.LocationApi
import com.enxy.weather.data.network.json.opencage.LocationResponse
import com.enxy.weather.utils.Result

class LocationRepository(private val locationApi: LocationApi) :
    NetworkRepository {
    companion object {
        const val OPEN_CAGE_API_KEY = BuildConfig.API_KEY_OPEN_CAGE
    }

    /**
     * Performs GET request to the OpenCage API to fetch locations by [locationName].
     */
    suspend fun getLocationsByName(locationName: String): Result<ArrayList<Location>> {
        return safeApiCall(
            call = {
                locationApi.getLocationsByNameAsync(
                    locationName,
                    OPEN_CAGE_API_KEY
                )
            },
            transform = ::transformLocationResponse
        )
    }

    /**
     * Converts [LocationResponse] to the list of [Location]
     */
    private fun transformLocationResponse(locationResponse: LocationResponse): ArrayList<Location> {
        val data = ArrayList<Location>()
        for (result in locationResponse.results) {
            val longitude = result.geometry.lng
            val latitude = result.geometry.lat
            val locationName = result.formatted
            val model = Location(
                locationName,
                longitude,
                latitude
            )
            data.add(model)
        }
        return data
    }
}