package com.enxy.weather.repository

import com.enxy.weather.BuildConfig
import com.enxy.weather.base.NetworkRepository
import com.enxy.weather.data.entity.LocationInfo
import com.enxy.weather.exception.Failure
import com.enxy.weather.functional.Result
import com.enxy.weather.network.NetworkService
import com.enxy.weather.network.json.opencage.LocationResponse

class LocationRepository(private val service: NetworkService) :
    NetworkRepository {
    companion object {
        const val OPEN_CAGE_API_KEY = BuildConfig.API_KEY_OPEN_CAGE
    }

    suspend fun getLocationsByName(locationName: String): Result<Failure, ArrayList<LocationInfo>> {
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

    private fun transformLocationResponse(locationResponse: LocationResponse): ArrayList<LocationInfo> {
        val data = ArrayList<LocationInfo>()
        for (result in locationResponse.results) {
            val longitude = result.geometry.lng
            val latitude = result.geometry.lat
            val locationName = result.formatted
            val model = LocationInfo(
                locationName,
                longitude,
                latitude
            )
            data.add(model)
        }
        return data
    }
}