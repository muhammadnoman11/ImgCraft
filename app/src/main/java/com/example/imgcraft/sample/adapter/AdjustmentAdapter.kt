package com.example.imgcraft.sample.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.imgcraft.sample.R

/**
 * @author: Muhammad Noman
 * @github: https://github.com/muhammadnoman11
 * @date: 12/15/2025
 */
class AdjustmentAdapter(
    private val items: MutableList<AdjustmentTool>,
    private val onToolSelected: (AdjustmentTool) -> Unit
) : RecyclerView.Adapter<AdjustmentAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.txtName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.tools_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.name.text = item.name
        holder.name.setTextColor(
            if (item.isSelected) Color.BLUE else Color.BLACK
        )

        holder.name.setOnClickListener {
            selectItem(position)
            onToolSelected(item)
        }
    }

    override fun getItemCount() = items.size

    private fun selectItem(position: Int) {
        items.forEachIndexed { index, tool ->
            tool.isSelected = index == position
        }
        notifyDataSetChanged()
    }
}

