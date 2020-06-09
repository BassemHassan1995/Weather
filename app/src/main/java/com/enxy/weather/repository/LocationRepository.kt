package com.enxy.weather.repository

import com.enxy.weather.BuildConfig
import com.enxy.weather.base.NetworkRepository
import com.enxy.weather.data.entity.Location
import com.enxy.weather.network.NetworkService
import com.enxy.weather.network.json.opencage.LocationResponse
import com.enxy.weather.utils.Result
import com.enxy.weather.utils.exception.Failure

class LocationRepository(private val service: NetworkService) :
    NetworkRepository {
    companion object {
        const val OPEN_CAGE_API_KEY = BuildConfig.API_KEY_OPEN_CAGE
    }

    suspend fun getLocationsByName(locationName: String): Result<Failure, ArrayList<Location>> {
        return safeApiCall(
            call = {
                service.locationApi().getLocationsByNameAsync(
                    locationName,
                    OPEN_CAGE_API_KEY
                )
            },
            transform = ::transformLocationResponse
        )
    }

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