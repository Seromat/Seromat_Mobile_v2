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
import org.json.JSONObject
import java.io.IOException
import kotlin.concurrent.thread

class ManualRegulationFragment : Fragment() {

    private lateinit var buttonConfirmChanges: Button
    private lateinit var editTextTempLb: EditText
    private lateinit var editTextTempHb: EditText
    private lateinit var editTextHumLb: EditText
    private lateinit var editTextHumHb: EditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_manual_regulation, container, false)

        buttonConfirmChanges = root.findViewById(R.id.changeButton)
        editTextTempLb = root.findViewById(R.id.editTextTempLb)
        editTextTempHb = root.findViewById(R.id.editTextTempHb)
        editTextHumLb = root.findViewById(R.id.editTextHumLb)
        editTextHumHb = root.findViewById(R.id.editTextHumHb)

        buttonConfirmChanges.setOnClickListener {
            if (!verifyParameters()) {
                return@setOnClickListener
            }
            val request = setParametersRequest()
            val client = OkHttpClient()
            thread {
                try {
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            showToast("Request failed: ${response.message}")
                        } else {
                            showToast("Parameters set successfully")
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    showToast("Network error: ${e.message}")
                }
            }
        }
        return root
    }

    private fun setParametersRequest(): Request {
        val lbTemp = editTextTempLb.text.toString()
        val hbTemp = editTextTempHb.text.toString()
        val lbHum = editTextHumLb.text.toString()
        val hbHum = editTextHumHb.text.toString()

        val sharedPreferences = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val ipAddress = sharedPreferences.getString("ipAddress", "127.0.0.1") ?: "127.0.0.1"

        val url = "http://$ipAddress:5000/set_parameters"

        val json = JSONObject().apply {
            put("temp_lb", lbTemp)
            put("temp_ub", hbTemp)
            put("hum_lb", lbHum)
            put("hum_ub", hbHum)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = json.toString().toRequestBody(mediaType)

        return Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
    }

    private fun verifyParameters(): Boolean {
        val lbTemp = editTextTempLb.text.toString().toDoubleOrNull()
        val ubTemp = editTextTempHb.text.toString().toDoubleOrNull()
        val lbHum = editTextHumLb.text.toString().toDoubleOrNull()
        val ubHum = editTextHumHb.text.toString().toDoubleOrNull()

        if (lbTemp == null || ubTemp == null || lbHum == null || ubHum == null) {
            showToast("Please enter valid values for all fields")
            return false
        }

        if (lbTemp < 0 || lbTemp > 100) {
            showToast("Lower temperature limit must be between 5 and 25")
            return false
        }

        if (ubTemp < 0 || ubTemp > 100) {
            showToast("Upper temperature limit must be between 0 and 100")
            return false
        }

        if (lbHum < 0 || lbHum > 100) {
            showToast("Lower humidity limit must be between 0 and 100")
            return false
        }

        if (ubHum < 0 || ubHum > 100) {
            showToast("Upper humidity limit must be between 0 and 100")
            return false
        }

        if (ubHum <= lbHum + 5) {
            showToast("Upper humidity limit must be at least 5% higher than the lower limit")
            return false
        }

        if (ubTemp <= lbTemp + 0.2) {
            showToast("Upper temperature limit must be at least 0.2 degrees higher than the lower limit")
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
