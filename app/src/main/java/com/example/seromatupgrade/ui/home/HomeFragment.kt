package com.example.seromatupgrade.ui.home

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.seromatupgrade.DatabaseManager.DatabaseManager
import com.example.seromatupgrade.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

//musi być inna klasa, gdyż potrzebny jest format gdzie nazwy są z Dużej litery
data class WeatherData(
    val Date: String,
    val TemperatureC: Double,
    val Humidity: Double,
    val CoolerStatus: String,
    val HumidifierStatus: String
)

class HomeFragment : Fragment() {

    private lateinit var handler: Handler
    private lateinit var dateText: TextView
    private lateinit var temperatureText: TextView
    private lateinit var humidityText: TextView
    private lateinit var coolerStatusText: TextView
    private lateinit var humidifierStatusText: TextView
    private lateinit var temperatureChart: LineChart
    private lateinit var humidityChart: LineChart
    private val client = OkHttpClient()
    private val temperatureEntries = mutableListOf<Entry>()
    private val humidityEntries = mutableListOf<Entry>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        dateText = root.findViewById(R.id.textViewMeasurementDate)
        temperatureText = root.findViewById(R.id.textViewMeasurementTemperature)
        humidityText = root.findViewById(R.id.textViewMeasurementHumidity)
        coolerStatusText = root.findViewById(R.id.coolerStatusText)
        humidifierStatusText = root.findViewById(R.id.humidifierStatusText)
        temperatureChart = root.findViewById(R.id.temperatureChart)
        humidityChart = root.findViewById(R.id.humidityChart)
        handler = Handler(Looper.getMainLooper())
        configureCharts()
        loadDataFromDatabaseAndUpdateCharts()
        startSendingRequests()

        return root
    }

    private fun loadDataFromDatabaseAndUpdateCharts() {
        val dbManager = DatabaseManager(requireContext())
        val measurements = dbManager.getLatestMeasurements()

        temperatureEntries.clear()
        humidityEntries.clear()
        //pętla typu foreach, które daje nam pozycje na wykresie(index) i pomiar
        for ((index, measurement) in measurements.withIndex()) {
            temperatureEntries.add(Entry(index.toFloat(), measurement.temperature.toFloat()))
            humidityEntries.add(Entry(index.toFloat(), measurement.humidity.toFloat()))
        }
        updateCharts()
    }


    private fun configureCharts()
    {
       temperatureChart.description = null
        humidityChart.description = null
    }

    private fun updateCharts() {

        val temperatureDataSet = LineDataSet(temperatureEntries, "Temperature")
        temperatureDataSet.color = Color.RED
        temperatureDataSet.valueTextColor = Color.BLACK

        val humidityDataSet = LineDataSet(humidityEntries, "Humidity")
        humidityDataSet.color = Color.BLUE
        humidityDataSet.valueTextColor = Color.BLACK

        temperatureChart.data = LineData(temperatureDataSet)
        temperatureChart.invalidate()

        humidityChart.data = LineData(humidityDataSet)
        humidityChart.invalidate()
    }

    private fun startSendingRequests() {
        val dbManager = DatabaseManager(requireContext())
        val runnable = object : Runnable {
            override fun run() {
                if (!isAdded) {
                    return
                }

                val request = buildRequest()
                if(request == null)
                {
                    return
                }
                client.newCall(request).enqueue(object : Callback {

                    override fun onFailure(call: Call, e: IOException) {
                        e.printStackTrace()
                    }

                    override fun onResponse(call: Call, response: Response) {
                        Log.i("Response", "Received Response from server")

                        response.use {
                            if (!response.isSuccessful) {
                                Log.e("HTTP Error", "Something didn't load, or wasn't successful")
                                return
                            }
                            val body = response.body?.string()
                            val gson = Gson()
                            val weatherData = gson.fromJson(body, WeatherData::class.java)
                            if (!isAdded) {
                                return
                            }
                            requireActivity().runOnUiThread {
                                temperatureText.text = weatherData.TemperatureC.toString()
                                humidityText.text = weatherData.Humidity.toString()
                                coolerStatusText.text = "Cooling: " + weatherData.CoolerStatus;
                                humidifierStatusText.text = "Humidifier: " + weatherData.HumidifierStatus;

                                val originalFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.UK)
                                val targetFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK)
                                val originalDate = originalFormat.parse(weatherData.Date)
                                val formattedDate = targetFormat.format(originalDate)
                                dateText.text = formattedDate
                                dbManager.insertMeasurement(formattedDate, weatherData.TemperatureC, weatherData.Humidity)
                                loadDataFromDatabaseAndUpdateCharts()
                                }
                            }
                        }
                    })

                val sharedPreferences = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
                val interval = sharedPreferences.getInt("msInterval", 5000)
                handler.postDelayed(this, interval.toLong())
            }
        }
        handler.post(runnable)
    }

    private fun buildRequest(): Request? {
        if (!isAdded) {
            Log.e("HomeFragment", "Fragment is not added to an activity.")
            return null
        }
        val sharedPreferences: SharedPreferences = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val ip = sharedPreferences.getString("ipAddress", "127.0.0.1") ?: "127.0.0.1"

        val url = "http://$ip:5000/sensor_data"
        return Request.Builder()
            .url(url)
            .build()
    }
}