package com.example.seromatupgrade.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.seromatupgrade.R

class SettingsFragment : Fragment() {

    private lateinit var ipAddressEditText: EditText
    private lateinit var millisecondsEditText: EditText
    private lateinit var saveButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_settings, container, false)

        ipAddressEditText = root.findViewById(R.id.ip_address)
        millisecondsEditText = root.findViewById(R.id.milliseconds)
        saveButton = root.findViewById(R.id.buttonSaveChanges)

        val sharedPreferences = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val savedIpAddress = sharedPreferences.getString("ipAddress", "")
        val savedMilliseconds = sharedPreferences.getInt("msInterval", 5000)

        ipAddressEditText.setText(savedIpAddress)
        millisecondsEditText.setText(savedMilliseconds.toString())

        saveButton.setOnClickListener {
            val intervalInMs = millisecondsEditText.text.toString().toInt()
            val editor = sharedPreferences.edit()
            editor.putString("ipAddress", ipAddressEditText.text.toString())
            editor.putInt("msInterval", intervalInMs)
            editor.apply()
            Toast.makeText(requireContext(), "Saved Changes!", Toast.LENGTH_LONG).show()
        }
        return root
    }
}
