package com.example.esp32control

import android.content.SharedPreferences


import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MainFragment : Fragment(), ProgramAdapter.OnItemClickListener {

    private val esp32Ip = "192.168.4.1"
    private lateinit var adapter: ProgramAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var jsonArray: JSONArray

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val saveProgramButton: Button = view.findViewById(R.id.saveProgramButton)
        val submitButton: Button = view.findViewById(R.id.submitButton)
        val turnOffAllButton: Button = view.findViewById(R.id.turnOffAllButton)
        val recyclerView: RecyclerView = view.findViewById(R.id.programRecyclerView)

        sharedPreferences = requireActivity().getSharedPreferences("LEDController", Context.MODE_PRIVATE)
        jsonArray = loadPrograms()

        adapter = ProgramAdapter(requireContext(), mutableListOf(), this)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        submitButton.setOnClickListener {
            val selectedLEDs = mutableListOf<String>()
            val checkBoxRed: CheckBox = view.findViewById(R.id.checkBoxRed)
            val checkBoxGreen: CheckBox = view.findViewById(R.id.checkBoxGreen)
            val checkBoxYellow: CheckBox = view.findViewById(R.id.checkBoxYellow)
            val durationInput: EditText = view.findViewById(R.id.durationInput)
            if (checkBoxRed.isChecked) selectedLEDs.add("R")
            if (checkBoxGreen.isChecked) selectedLEDs.add("G")
            if (checkBoxYellow.isChecked) selectedLEDs.add("Y")
            val duration = durationInput.text.toString().toIntOrNull() ?: 0
            controlLEDs(selectedLEDs, duration)
        }

        turnOffAllButton.setOnClickListener {
            turnOffAllLEDs()
        }

        saveProgramButton.setOnClickListener {
            showSaveDialog()
        }

        loadSavedPrograms()
    }

    private fun showSaveDialog() {
        val inflater: LayoutInflater = layoutInflater
        val dialogView: View = inflater.inflate(R.layout.dialog_save_program, null)
        val editTextProgramName: EditText = dialogView.findViewById(R.id.editTextProgramName)

        val dialog = AlertDialog.Builder(requireContext(), R.style.DialogTheme)
            .setTitle("Programm speichern")
            .setView(dialogView)
            .setPositiveButton("Speichern") { dialog, _ ->
                val programName = editTextProgramName.text.toString()
                if (programName.isNotEmpty()) {
                    saveProgram(programName)
                } else {
                    Toast.makeText(requireContext(), "Bitte geben Sie einen Programmnamen ein", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Abbrechen") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.setOnShowListener {
            val window = dialog.window
            if (window != null) {
                val params = window.attributes
                params.height = WindowManager.LayoutParams.WRAP_CONTENT
                window.attributes = params
            }
            // Positive button (Speichern) anpassen
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.GRAY)
            // Negative button (Abbrechen) anpassen
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.GRAY) // Optional
        }

        dialog.show()
    }


    private fun saveProgram(name: String) {
        val selectedLEDs = mutableListOf<String>()
        val checkBoxRed: CheckBox = view?.findViewById(R.id.checkBoxRed) ?: return
        val checkBoxGreen: CheckBox = view?.findViewById(R.id.checkBoxGreen) ?: return
        val checkBoxYellow: CheckBox = view?.findViewById(R.id.checkBoxYellow) ?: return
        val durationInput: EditText = view?.findViewById(R.id.durationInput) ?: return
        if (checkBoxRed.isChecked) selectedLEDs.add("R")
        if (checkBoxGreen.isChecked) selectedLEDs.add("G")
        if (checkBoxYellow.isChecked) selectedLEDs.add("Y")
        val duration = durationInput.text.toString()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val program = JSONObject().apply {
            put("name", name)
            put("leds", JSONArray(selectedLEDs))
            put("duration", duration)
            put("timestamp", timestamp)
        }
        jsonArray.put(program)

        sharedPreferences.edit().putString("programs", jsonArray.toString()).apply()

        loadSavedPrograms()
    }

    override fun onItemClick(position: Int) {
        showProgramDetails(position)
    }

    private fun showProgramDetails(position: Int) {
        val program = jsonArray.getJSONObject(position)
        val name = program.optString("name", "Kein Name")
        val leds = program.optJSONArray("leds") ?: JSONArray()
        val duration = program.optString("duration", "0")
        val timestamp = program.optString("timestamp", "Unbekannt")
        val ledsString = (0 until leds.length()).joinToString(", ") { leds.getString(it) }

        val details = "Name: $name\nLEDs: $ledsString\nDauer: $duration s\nErstellt: $timestamp"

        val dialogView = layoutInflater.inflate(R.layout.dialog_program_details, null)
        val detailsTextView: TextView = dialogView.findViewById(R.id.detailsTextView)
        detailsTextView.text = details

        val dialog = AlertDialog.Builder(requireContext(), R.style.DialogTheme)
            .setView(dialogView)
            .setPositiveButton("Starten") { _, _ ->
                val selectedLEDs = mutableListOf<String>()
                for (i in 0 until leds.length()) {
                    selectedLEDs.add(leds.getString(i))
                }
                val durationInt = duration.toIntOrNull() ?: 0
                controlLEDs(selectedLEDs, durationInt)
            }
            .setNegativeButton("Schließen") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.setOnShowListener {
            val window = dialog.window
            if (window != null) {
                val params = window.attributes
                params.height = WindowManager.LayoutParams.WRAP_CONTENT
                window.attributes = params
            }
        }

        dialog.show()
    }


    private fun loadSavedPrograms() {
        val programs = mutableListOf<JSONObject>()
        for (i in 0 until jsonArray.length()) {
            val program = jsonArray.getJSONObject(i)
            programs.add(program)
        }
        adapter.updatePrograms(programs)
    }

    private fun loadPrograms(): JSONArray {
        val programsString = sharedPreferences.getString("programs", "[]")
        return JSONArray(programsString)
    }

    private fun controlLEDs(leds: List<String>, duration: Int) {
        leds.forEach { led ->
            val url = "http://$esp32Ip/LED=${led}_ON"
            sendRequest(url)
        }

        if (duration > 0) {
            CoroutineScope(Dispatchers.IO).launch {
                delay(duration * 1000L)
                leds.forEach { led ->
                    val url = "http://$esp32Ip/LED=${led}_OFF"
                    sendRequest(url)
                }
            }
        }
    }

    private fun turnOffAllLEDs() {
        val leds = listOf("R", "G", "Y")
        leds.forEach { led ->
            val url = "http://$esp32Ip/LED=${led}_OFF"
            sendRequest(url)
        }
    }

    private fun sendRequest(urlString: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()
                connection.responseCode
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
