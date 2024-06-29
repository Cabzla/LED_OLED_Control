package com.example.esp32control

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

class DisplayFragment : Fragment(), DisplayAdapter.OnItemClickListener {

    private val esp32Ip = "192.168.4.1"
    private lateinit var adapter: DisplayAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var jsonArray: JSONArray

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_display, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val timeInput: EditText = view.findViewById(R.id.timeInput)
        val trainNumberInput: EditText = view.findViewById(R.id.trainNumberInput)
        val trainNameInput: EditText = view.findViewById(R.id.trainNameInput)
        val trackNumberInput: EditText = view.findViewById(R.id.trackNumberInput)
        val infoTextInput: EditText = view.findViewById(R.id.infoTextInput)
        val routeInfoInput: EditText = view.findViewById(R.id.routeInfoInput)
        val updateDisplayButton: Button = view.findViewById(R.id.updateDisplayButton)
        val saveDisplayButton: Button = view.findViewById(R.id.saveDisplayButton)
        val recyclerView: RecyclerView = view.findViewById(R.id.displayRecyclerView)

        sharedPreferences = requireActivity().getSharedPreferences("DisplayController", Context.MODE_PRIVATE)
        jsonArray = loadPrograms()

        adapter = DisplayAdapter(requireContext(), mutableListOf(), this)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        updateDisplayButton.setOnClickListener {
            val time = timeInput.text.toString()
            val trainNumber = trainNumberInput.text.toString()
            val trainName = trainNameInput.text.toString()
            val trackNumber = trackNumberInput.text.toString()
            val infoText = infoTextInput.text.toString()
            val routeInfo = routeInfoInput.text.toString()

            val url = buildUrl(time, trainNumber, trainName, trackNumber, infoText, routeInfo)
            sendRequest(url)
        }

        saveDisplayButton.setOnClickListener {
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
            // button anpassen
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.GRAY)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.GRAY)
        }

        dialog.show()
    }


    private fun saveProgram(name: String) {
        val timeInput: EditText = view?.findViewById(R.id.timeInput) ?: return
        val trainNumberInput: EditText = view?.findViewById(R.id.trainNumberInput) ?: return
        val trainNameInput: EditText = view?.findViewById(R.id.trainNameInput) ?: return
        val trackNumberInput: EditText = view?.findViewById(R.id.trackNumberInput) ?: return
        val infoTextInput: EditText = view?.findViewById(R.id.infoTextInput) ?: return
        val routeInfoInput: EditText = view?.findViewById(R.id.routeInfoInput) ?: return

        val program = JSONObject().apply {
            put("name", name)
            put("time", timeInput.text.toString())
            put("trainNumber", trainNumberInput.text.toString())
            put("trainName", trainNameInput.text.toString())
            put("trackNumber", trackNumberInput.text.toString())
            put("infoText", infoTextInput.text.toString())
            put("routeInfo", routeInfoInput.text.toString())
            put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        }
        jsonArray.put(program)

        sharedPreferences.edit().putString("displayPrograms", jsonArray.toString()).apply()

        loadSavedPrograms()
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
        val programsString = sharedPreferences.getString("displayPrograms", "[]")
        return JSONArray(programsString)
    }

    private fun buildUrl(time: String, trainNumber: String, trainName: String, trackNumber: String, infoText: String, routeInfo: String): String {
        val baseUrl = "http://$esp32Ip/updateDisplay?"
        val timeParam = if (time.isNotEmpty()) "time=${URLEncoder.encode(time, "UTF-8")}&" else ""
        val trainNumberParam = if (trainNumber.isNotEmpty()) "trainNumber=${URLEncoder.encode(trainNumber, "UTF-8")}&" else ""
        val trainNameParam = if (trainName.isNotEmpty()) "trainName=${URLEncoder.encode(trainName, "UTF-8")}&" else ""
        val trackNumberParam = if (trackNumber.isNotEmpty()) "trackNumber=${URLEncoder.encode(trackNumber, "UTF-8")}&" else ""
        val infoTextParam = if (infoText.isNotEmpty()) "infoText=${URLEncoder.encode(infoText, "UTF-8")}&" else ""
        val routeInfoParam = if (routeInfo.isNotEmpty()) "routeInfo=${URLEncoder.encode(routeInfo, "UTF-8")}&" else ""

        return baseUrl + timeParam + trainNumberParam + trainNameParam + trackNumberParam + infoTextParam + routeInfoParam
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
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Anzeige aktualisiert", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Fehler beim Aktualisieren der Anzeige", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onItemClick(position: Int) {
        showProgramDetails(position)
    }

    private fun showProgramDetails(position: Int) {
        val program = jsonArray.getJSONObject(position)
        val name = program.optString("name", "Kein Name")
        val time = program.optString("time", "00:00")
        val trainNumber = program.optString("trainNumber", "N/A")
        val trainName = program.optString("trainName", "N/A")
        val trackNumber = program.optString("trackNumber", "N/A")
        val infoText = program.optString("infoText", "N/A")
        val routeInfo = program.optString("routeInfo", "N/A")
        val timestamp = program.optString("timestamp", "Unbekannt")

        val details = "Name: $name\nUhrzeit: $time\nZugnummer: $trainNumber\nZugname: $trainName\nGleisnummer: $trackNumber\nInfo Text: $infoText\nStreckeninformationen: $routeInfo\nErstellt: $timestamp"

        val dialogView = layoutInflater.inflate(R.layout.dialog_program_details, null)
        val detailsTextView: TextView = dialogView.findViewById(R.id.detailsTextView)
        detailsTextView.text = details

        val dialog = AlertDialog.Builder(requireContext(), R.style.DialogTheme)
            .setView(dialogView)
            .setPositiveButton("Starten") { _, _ ->
                val url = buildUrl(time, trainNumber, trainName, trackNumber, infoText, routeInfo)
                sendRequest(url)
            }
            .setNegativeButton("Schließen") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }
}





