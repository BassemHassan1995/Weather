package com.enxy.weather.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.enxy.weather.data.entity.CurrentForecast
import com.enxy.weather.data.entity.Forecast
import com.enxy.weather.data.entity.HourForecast

@Database(entities = [CurrentForecast::class, HourForecast::class, Forecast::class], version = 3)
@TypeConverters(Converters::class)
abstract class AppDataBase : RoomDatabase() {
    abstract fun getForecastDao(): ForecastDao

    companion object {
        const val DATABASE_NAME = "weather-db"

        @Volatile private var instance: AppDataBase? = null

        fun getInstance(context: Context): AppDataBase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context, AppDataBase::class.java, DATABASE_NAME)
                    .build()
            }
        }
    }
}