package com.example.myapplication

import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val esp32Ip = "192.168.4.1"
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var jsonArray: JSONArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val saveProgramButton: Button = findViewById(R.id.saveProgramButton)
        val submitButton: Button = findViewById(R.id.submitButton)
        val turnOffAllButton: Button = findViewById(R.id.turnOffAllButton)
        val listView: ListView = findViewById(R.id.programListView)

        sharedPreferences = getSharedPreferences("LEDController", Context.MODE_PRIVATE)
        jsonArray = loadPrograms()

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ArrayList())
        listView.adapter = adapter

        submitButton.setOnClickListener {
            val selectedLEDs = mutableListOf<String>()
            val checkBoxRed: CheckBox = findViewById(R.id.checkBoxRed)
            val checkBoxGreen: CheckBox = findViewById(R.id.checkBoxGreen)
            val checkBoxYellow: CheckBox = findViewById(R.id.checkBoxYellow)
            val durationInput: EditText = findViewById(R.id.durationInput)
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

        listView.setOnItemClickListener { _, _, position, _ ->
            showProgramDetails(position)
        }

        loadSavedPrograms()
    }

    private fun showSaveDialog() {
        val inflater: LayoutInflater = layoutInflater
        val dialogView: View = inflater.inflate(R.layout.dialog_save_program, null)
        val editTextProgramName: EditText = dialogView.findViewById(R.id.editTextProgramName)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Programm speichern")
            .setView(dialogView)
            .setPositiveButton("Speichern") { dialog, _ ->
                val programName = editTextProgramName.text.toString()
                if (programName.isNotEmpty()) {
                    saveProgram(programName)
                } else {
                    Toast.makeText(this, "Bitte geben Sie einen Programmnamen ein", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Abbrechen") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun saveProgram(name: String) {
        val selectedLEDs = mutableListOf<String>()
        val checkBoxRed: CheckBox = findViewById(R.id.checkBoxRed)
        val checkBoxGreen: CheckBox = findViewById(R.id.checkBoxGreen)
        val checkBoxYellow: CheckBox = findViewById(R.id.checkBoxYellow)
        val durationInput: EditText = findViewById(R.id.durationInput)
        if (checkBoxRed.isChecked) selectedLEDs.add("R")
        if (checkBoxGreen.isChecked) selectedLEDs.add("G")
        if (checkBoxYellow.isChecked) selectedLEDs.add("Y")
        val duration = durationInput.text.toString()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val program = JSONObject()
        program.put("name", name)
        program.put("leds", JSONArray(selectedLEDs))
        program.put("duration", duration)
        program.put("timestamp", timestamp)
        jsonArray.put(program)

        val editor = sharedPreferences.edit()
        editor.putString("programs", jsonArray.toString())
        editor.apply()

        loadSavedPrograms()
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

        val dialog = AlertDialog.Builder(this)
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

        dialog.show()
    }

    private fun loadSavedPrograms() {
        val programs = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            val program = jsonArray.getJSONObject(i)
            val name = program.optString("name", "Kein Name")
            programs.add(name)
        }
        adapter.clear()
        adapter.addAll(programs)
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

