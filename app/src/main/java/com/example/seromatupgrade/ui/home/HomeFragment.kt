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

data class StatusData(
    val Date: String,
    val TemperatureC: Double,
    val Humidity: Double,
    val AvgHumidity: Double,
    val CoolerStatus: String,
    val HumidifierStatus: String
)

data class Parameters(
    val TempSp: Double,
    val TempH: Double,
    val HumSp: Double,
    val HumH: Double
)

class HomeFragment : Fragment() {

    private lateinit var handler: Handler
    private lateinit var dateText: TextView
    private lateinit var temperatureText: TextView
    private lateinit var humidityText: TextView
    private lateinit var avgHumidityText: TextView
    private lateinit var coolerStatusText: TextView
    private lateinit var humidifierStatusText: TextView
    private lateinit var tempBoundariesText: TextView
    private lateinit var humBoundariesText: TextView
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
        avgHumidityText = root.findViewById(R.id.textViewAvgHumidity)
        coolerStatusText = root.findViewById(R.id.coolerStatusText)
        humidifierStatusText = root.findViewById(R.id.humidifierStatusText)

        tempBoundariesText = root.findViewById(R.id.tempBoundariesText)
        humBoundariesText = root.findViewById(R.id.humBoundariesText)

        temperatureChart = root.findViewById(R.id.temperatureChart)
        humidityChart = root.findViewById(R.id.humidityChart)
        handler = Handler(Looper.getMainLooper())
        configureCharts()
        loadDataFromDatabaseAndUpdateCharts()

        startSendingRequests()
        return root
    }

    override fun onStart() {
        getParameters()
        super.onStart()
    }

    private fun loadDataFromDatabaseAndUpdateCharts() {
        val dbManager = DatabaseManager(requireContext())
        val measurements = dbManager.getLatestMeasurements()

        temperatureEntries.clear()
        humidityEntries.clear()
        for ((index, measurement) in measurements.withIndex()) {
            temperatureEntries.add(Entry(index.toFloat(), measurement.temperature.toFloat()))
            humidityEntries.add(Entry(index.toFloat(), measurement.humidity.toFloat()))
        }
        updateCharts()
    }


    private fun configureCharts() {
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

                val request = getSensorDataRequest()
                if (request == null) {
                    return
                }
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        e.printStackTrace()
                    }
                    override fun onResponse(call: Call, response: Response) {
                        Log.i("Response", "Request response received")

                        response.use {
                            if (!response.isSuccessful) {
                                Log.e("HTTP Error", "HTTP response unsuccessful")
                                return
                            }
                            val body = response.body.string()
                            val gson = Gson()
                            val statusData = gson.fromJson(body, StatusData::class.java)
                            if (!isAdded) {
                                return
                            }
                            requireActivity().runOnUiThread {
                                temperatureText.text = statusData.TemperatureC.toString()
                                humidityText.text = statusData.Humidity.toString()
                                avgHumidityText.text = String.format("%.1f", statusData.AvgHumidity)
                                coolerStatusText.text = "Cooling: " + statusData.CoolerStatus;
                                humidifierStatusText.text = "Humidifier: " + statusData.HumidifierStatus;

                                val originalFormat =
                                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.UK)
                                val targetFormat =
                                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK)
                                val originalDate = originalFormat.parse(statusData.Date)
                                val formattedDate = targetFormat.format(originalDate)
                                dateText.text = formattedDate
                                dbManager.insertMeasurement(
                                    formattedDate,
                                    statusData.TemperatureC,
                                    statusData.Humidity
                                )
                                loadDataFromDatabaseAndUpdateCharts()
                            }
                        }
                    }
                })

                val sharedPreferences =
                    requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
                val interval = sharedPreferences.getInt("msInterval", 5000)
                handler.postDelayed(this, interval.toLong())
            }
        }
        handler.post(runnable)
    }

    private fun getSensorDataRequest(): Request? {
        if (!isAdded) {
            Log.e("HomeFragment", "Fragment is not added to an activity.")
            return null
        }
        val sharedPreferences: SharedPreferences =
            requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val ip = sharedPreferences.getString("ipAddress", "127.0.0.1") ?: "127.0.0.1"

        val url = "http://$ip:5000/sensor_data"
        return Request.Builder()
            .url(url)
            .build()
    }

    private fun getParameters() {

        val request = getParametersRequest()
        if (request == null) {
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
                        Log.e("HTTP Error", "HTTP response unsuccessful")
                        return
                    }
                    val body = response.body.string()
                    val gson = Gson()
                    val parameters = gson.fromJson(body, Parameters::class.java)
                    if (!isAdded) {
                        return
                    }
                    requireActivity().runOnUiThread {
                        tempBoundariesText.text = "(SP: ${parameters.TempSp} H: ${parameters.TempH})"
                        humBoundariesText.text = "(SP: ${parameters.HumSp} H: ${parameters.HumH})"
                    }
                }
            }
        })
    }

    private fun getParametersRequest(): Request? {
        if (!isAdded) {
            Log.e("HomeFragment", "Fragment is not added to an activity.")
            return null
        }
        val sharedPreferences: SharedPreferences =
            requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val ip = sharedPreferences.getString("ipAddress", "127.0.0.1") ?: "127.0.0.1"

        val url = "http://$ip:5000/parameters"
        return Request.Builder()
            .url(url)
            .build()
    }
}
