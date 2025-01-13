package com.example.seromatupgrade.ui.manual_regulation

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.seromatupgrade.R
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import kotlin.concurrent.thread

class ManualRegulationFragment : Fragment() {

    private lateinit var buttonConfirmChanges: Button
    private lateinit var editTextTempSp: EditText
    private lateinit var editTextTempH: EditText
    private lateinit var editTextHumSp: EditText
    private lateinit var editTextHumH: EditText

    data class Parameters(
        val TempSp: Double,
        val TempH: Double,
        val HumSp: Double,
        val HumH: Double
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_manual_regulation, container, false)

        buttonConfirmChanges = root.findViewById(R.id.changeButton)
        editTextTempSp = root.findViewById(R.id.editTextTempSp)
        editTextTempH = root.findViewById(R.id.editTextTempH)
        editTextHumSp = root.findViewById(R.id.editTextHumSp)
        editTextHumH = root.findViewById(R.id.editTextHumH)

        buttonConfirmChanges.setOnClickListener {
            if (!verifyParameters()) {
                return@setOnClickListener
            }
            val request = setParametersRequest()
            val client = OkHttpClient()
            thread {
                try {
                    if (request != null) {
                        client.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) {
                                showToast("Fail: ${response.message}")
                            } else {
                                showToast("Parameters set successfully")
                            }
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    showToast("Error: ${e.message}")
                }
            }
        }
        return root
    }

    override fun onStart() {

        val syf = OkHttpClient()
        val request = GetParametersRequest()

        syf.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                Log.i("Response", "Received Response from server")

                response.use {
                    if (!response.isSuccessful) {
                        Log.e("HTTP Error", "HTTP response unsuccessful")
                        return@use
                    }
                    val body = response.body?.string()
                    val gson = Gson()
                    val regulatorData = gson.fromJson(body, Parameters::class.java)
                    requireActivity().runOnUiThread {
                        editTextTempSp.setText(regulatorData.TempSp.toString())
                        editTextTempH.setText(regulatorData.TempH.toString())
                        editTextHumSp.setText(regulatorData.HumSp.toString())
                        editTextHumH.setText(regulatorData.HumH.toString())
                    }
                }
            }
        })
        super.onStart()

    }

    fun GetParametersRequest(): Request {

        val sharedPreferences = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val ipAddress = sharedPreferences.getString("ipAddress", "127.0.0.1") ?: "127.0.0.1"

        val URL: String = "http://$ipAddress:5000/parameters"

        val request = Request.Builder()
            .url(URL)
            .build()
        return request
    }

    private fun setParametersRequest(): Request? {

        if (!isAdded) {
            Log.e("ManualRegulationFragment", "Fragment is not added to an activity.")
            return null
        }

        val tempSp = editTextTempSp.text.toString().toDoubleOrNull() ?: 0.0
        val tempH = editTextTempH.text.toString().toDoubleOrNull() ?: 0.0
        val humSp = editTextHumSp.text.toString().toDoubleOrNull() ?: 0.0
        val humH = editTextHumH.text.toString().toDoubleOrNull() ?: 0.0

        val sharedPreferences = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val ipAddress = sharedPreferences.getString("ipAddress", "127.0.0.1") ?: "127.0.0.1"

        val url = "http://$ipAddress:5000/parameters"

        val parameters = Parameters(tempSp, tempH, humSp, humH)
        val gson = Gson()
        val json = gson.toJson(parameters)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = json.toString().toRequestBody(mediaType)

        return Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
    }

    private fun verifyParameters(): Boolean {
        val TempSp = editTextTempSp.text.toString().toDoubleOrNull()
        val TempH = editTextTempH.text.toString().toDoubleOrNull()
        val HumSp = editTextHumSp.text.toString().toDoubleOrNull()
        val HumH = editTextHumH.text.toString().toDoubleOrNull()

        if (TempSp == null || TempH == null || HumSp == null || HumH == null) {
            showToast("Please enter valid values for all fields")
            return false
        }

        if (TempSp <= 0 || TempSp >= 100) {
            showToast("Set temperature must be between 0 and 100")
            return false
        }

        if (TempH <= 0.1 || TempH >= 10) {
            showToast("Temperature hysteresis must be between 0.1 and 10")
            return false
        }

        if (HumSp <= 0 || HumSp >= 110) {
            showToast("Set humidity must must be between 0 and 110")
            return false
        }

        if (HumH <= 0.1 || HumH >= 20) {
            showToast("Humidity hysteresis must be between 0.1 and 20")
            return false
        }

        return true
    }

    private fun showToast(message: String) {
        requireActivity().runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }
}
