package com.enxy.weather.ui.main

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.enxy.weather.R
import com.enxy.weather.base.BaseFragment
import com.enxy.weather.data.CurrentForecast
import com.enxy.weather.data.Forecast
import com.enxy.weather.exception.Failure
import com.enxy.weather.extension.dpToPixels
import com.enxy.weather.extension.failure
import com.enxy.weather.extension.observe
import com.enxy.weather.ui.main.adapter.DayAdapter
import com.enxy.weather.ui.main.adapter.HourAdapter
import kotlinx.android.synthetic.main.main_fragment.*
import javax.inject.Inject


class MainFragment : BaseFragment() {
    override val layoutId = R.layout.main_fragment
    private lateinit var viewModel: MainViewModel
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var hourAdapter: HourAdapter
    @Inject
    lateinit var dayAdapter: DayAdapter

    companion object {
        fun newInstance() = MainFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        viewModel = getMainViewModel(viewModelFactory)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpSwipeRefreshLayout()
        setUpRecyclerView()
        with(viewModel) {
            observe(forecast, ::renderForecast)
            failure(forecastFailure, ::handleFailure)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.let {
            it.forecast.removeObservers(this)
            it.forecastFailure.removeObservers(this)
        }
    }

    private fun setUpSwipeRefreshLayout() {
        swipeRefreshLayout.setProgressViewOffset(true, 0, 55.dpToPixels)
        swipeRefreshLayout.setOnRefreshListener(::onRefresh)
    }

    private fun onRefresh() {
        if (!swipeRefreshLayout.isRefreshing)
            swipeRefreshLayout.isRefreshing = true
        viewModel.fetchWeatherForecast()
    }

    private fun handleFailure(failure: Failure?) {
        failure?.let {
            Log.d("MainFragment", "handleFailure: Failure=${failure.javaClass.name}")
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun renderForecast(forecast: Forecast?) {
        forecast?.let {
            Log.d("MainFragment", "renderHourWeather: hourForecast=$it")
            renderCurrentForecast(it.currentForecast)
            hourAdapter.updateData(it.hourForecast.hourArrayList)
            mainContentLinearLayout.isVisible = true
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun renderCurrentForecast(currentForecast: CurrentForecast) {
        Log.d("MainFragment", "renderCurrentWeather: currentForecast=$currentForecast")
        currentDescriptionTextView.text = currentForecast.description
        currentDescriptionImageView.setImageResource(currentForecast.imageId)
        currentTemperatureTextView.text = currentForecast.temperature
        currentFeelsLikeTextView.text = currentForecast.feelsLikeTemperature
        cityNameTextView.text = currentForecast.cityName
        currentHumidityValueTextView.text = currentForecast.humidity
        currentWindValueTextView.text = currentForecast.wind
        currentPressureValueTextView.text = currentForecast.pressure
        swipeRefreshLayout.isRefreshing = false
    }

    private fun setUpRecyclerView() {
        hourRecyclerView.adapter = hourAdapter

        hourRecyclerView.layoutManager = LinearLayoutManager(
            context, LinearLayoutManager.HORIZONTAL, false
        )
        hourRecyclerView.isNestedScrollingEnabled = false
    }
}
