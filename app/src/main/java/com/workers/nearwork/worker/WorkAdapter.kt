package com.workers.nearwork.worker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.workers.nearwork.R
import com.workers.nearwork.model.WorkPost

class WorkAdapter(
    private val workList: List<WorkPost>,
    private val onClick: (WorkPost) -> Unit
) : RecyclerView.Adapter<WorkAdapter.WorkViewHolder>() {

    class WorkViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // MATCH THESE TO YOUR NEW XML IDs
        val tvTitle: TextView = view.findViewById(R.id.tvHiredTitle)
        val tvDesc: TextView = view.findViewById(R.id.tvHiredDesc)
        val tvPrice: TextView = view.findViewById(R.id.tvHiredBudget)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkViewHolder {
        // USE THE NEW XML FILENAME HERE
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hired_job_card, parent, false)
        return WorkViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkViewHolder, position: Int) {
        val work = workList[position]

        // Populate with high-contrast formatting
        holder.tvTitle.text = work.category?.replaceFirstChar { it.uppercase() } ?: "Service"
        holder.tvDesc.text = work.description ?: "No description"
        holder.tvPrice.text = "Hired Price: $${work.finalPrice ?: work.budget ?: "0"}"

        holder.itemView.setOnClickListener {
            val currentPos = holder.bindingAdapterPosition
            if (currentPos != RecyclerView.NO_POSITION) {
                onClick(workList[currentPos])
            }
        }
    }

    override fun getItemCount() = workList.size
}