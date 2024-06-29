package com.example.esp32control

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class ProgramAdapter(
    private val context: Context,
    private val programs: MutableList<JSONObject>,
    private val itemClickListener: OnItemClickListener
) : RecyclerView.Adapter<ProgramAdapter.ProgramViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgramViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.list_item_program, parent, false)
        return ProgramViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProgramViewHolder, position: Int) {
        val program = programs[position]
        val name = program.optString("name", "Kein Name")
        holder.programNameTextView.text = name

        holder.cardView.setOnClickListener {
            itemClickListener.onItemClick(position)
        }
    }

    override fun getItemCount(): Int {
        return programs.size
    }

    fun updatePrograms(newPrograms: List<JSONObject>) {
        programs.clear()
        programs.addAll(newPrograms)
        notifyDataSetChanged()
    }

    class ProgramViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val programNameTextView: TextView = itemView.findViewById(R.id.programNameTextView)
        val cardView: CardView = itemView.findViewById(R.id.card_view)
    }
}
