package com.example.esp32control

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainFragment : Fragment(), ProgramAdapter.OnItemClickListener {

    private val esp32Ip = "192.168.4.1"
    private lateinit var adapter: ProgramAdapter
    private lateinit var jsonArray: JSONArray

    private var isExpanded = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val checkBoxRed: CheckBox = view.findViewById(R.id.checkBoxRed)
        val checkBoxGreen: CheckBox = view.findViewById(R.id.checkBoxGreen)
        val checkBoxYellow: CheckBox = view.findViewById(R.id.checkBoxYellow)
        val durationInput: EditText = view.findViewById(R.id.durationInput)
        val submitButton: Button = view.findViewById(R.id.submitButton)
        val turnOffAllButton: Button = view.findViewById(R.id.turnOffAllButton)
        val saveProgramButton: Button = view.findViewById(R.id.saveProgramButton)
        val recyclerView: RecyclerView = view.findViewById(R.id.programRecyclerView)
        val expandableCardView: View = view.findViewById(R.id.expandableCardView)
        val expandableLayout: LinearLayout = view.findViewById(R.id.expandableLayout)
        val expandIcon: ImageView = view.findViewById(R.id.expandIcon)

        adapter = ProgramAdapter(requireContext(), mutableListOf(), this)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        expandableCardView.setOnClickListener {
            isExpanded = !isExpanded
            val transition = AutoTransition()
            transition.duration = 300 // Dauer der Animation in Millisekunden
            TransitionManager.beginDelayedTransition(view as ViewGroup, transition)
            if (isExpanded) {
                expandableLayout.visibility = View.VISIBLE
                expandIcon.setImageResource(R.drawable.ic_expand_less)
            } else {
                expandableLayout.visibility = View.GONE
                expandIcon.setImageResource(R.drawable.ic_expand_more)
            }
        }

        submitButton.setOnClickListener {
            val duration = durationInput.text.toString()
            if (duration.isEmpty()) {
                Toast.makeText(requireContext(), "Bitte geben Sie die Leuchtdauer ein", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (checkBoxRed.isChecked) sendRequest("/LED=R_ON&duration=$duration")
            if (checkBoxGreen.isChecked) sendRequest("/LED=G_ON&duration=$duration")
            if (checkBoxYellow.isChecked) sendRequest("/LED=Y_ON&duration=$duration")
        }

        turnOffAllButton.setOnClickListener {
            sendRequest("/LED=R_OFF")
            sendRequest("/LED=G_OFF")
            sendRequest("/LED=Y_OFF")
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
            // button anpassen
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.GRAY)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.GRAY)
        }

        dialog.show()
    }

    private fun saveProgram(name: String) {
        val checkBoxRed: CheckBox = view?.findViewById(R.id.checkBoxRed) ?: return
        val checkBoxGreen: CheckBox = view?.findViewById(R.id.checkBoxGreen) ?: return
        val checkBoxYellow: CheckBox = view?.findViewById(R.id.checkBoxYellow) ?: return
        val durationInput: EditText = view?.findViewById(R.id.durationInput) ?: return

        val program = JSONObject().apply {
            put("name", name)
            put("red", checkBoxRed.isChecked)
            put("green", checkBoxGreen.isChecked)
            put("yellow", checkBoxYellow.isChecked)
            put("duration", durationInput.text.toString())
            put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        }
        jsonArray.put(program)

        val sharedPreferences = requireActivity().getSharedPreferences("LEDController", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("ledPrograms", jsonArray.toString()).apply()

        loadSavedPrograms()
    }

    private fun loadSavedPrograms() {
        val sharedPreferences = requireActivity().getSharedPreferences("LEDController", Context.MODE_PRIVATE)
        val programsString = sharedPreferences.getString("ledPrograms", "[]")
        jsonArray = JSONArray(programsString)

        val programs = mutableListOf<JSONObject>()
        for (i in 0 until jsonArray.length()) {
            val program = jsonArray.getJSONObject(i)
            programs.add(program)
        }
        adapter.updatePrograms(programs)
    }

    private fun sendRequest(path: String) {
        val urlString = "http://$esp32Ip$path"
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()
                connection.responseCode
                connection.disconnect()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Befehl gesendet", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Fehler beim Senden des Befehls", Toast.LENGTH_SHORT).show()
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
        val red = program.optBoolean("red", false)
        val green = program.optBoolean("green", false)
        val yellow = program.optBoolean("yellow", false)
        val duration = program.optString("duration", "0")
        val timestamp = program.optString("timestamp", "Unbekannt")

        val details = "Name: $name\nRot: $red\nGrün: $green\nGelb: $yellow\nLeuchtdauer: $duration sec\nErstellt: $timestamp"

        val dialogView = layoutInflater.inflate(R.layout.dialog_program_details, null)
        val detailsTextView: TextView = dialogView.findViewById(R.id.detailsTextView)
        detailsTextView.text = details

        val dialog = AlertDialog.Builder(requireContext(), R.style.DialogTheme)
            .setView(dialogView)
            .setPositiveButton("Starten") { _, _ ->
                if (red) sendRequest("/LED=R_ON&duration=$duration")
                if (green) sendRequest("/LED=G_ON&duration=$duration")
                if (yellow) sendRequest("/LED=Y_ON&duration=$duration")
            }
            .setNegativeButton("Schließen") { dialog, _ ->
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
}


