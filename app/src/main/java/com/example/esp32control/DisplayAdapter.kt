package com.example.esp32control

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class DisplayAdapter(private val context: Context, private val programs: MutableList<JSONObject>, private val itemClickListener: OnItemClickListener) : RecyclerView.Adapter<DisplayAdapter.ViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val programName: TextView = itemView.findViewById(R.id.programNameTextView)
        val programCard: CardView = itemView.findViewById(R.id.card_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.list_item_program, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val program = programs[position]
        holder.programName.text = program.optString("name", "Kein Name")

        holder.programCard.setOnClickListener {
            itemClickListener.onItemClick(position)
        }
    }

    override fun getItemCount(): Int {
        return programs.size
    }

    fun updatePrograms(newPrograms: MutableList<JSONObject>) {
        programs.clear()
        programs.addAll(newPrograms)
        notifyDataSetChanged()
    }
}
