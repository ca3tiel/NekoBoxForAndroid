package io.nekohasekai.sagernet.vpn.serverlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.nekohasekai.sagernet.R

class DropdownAdapter(
    private val subItems: List<ListSubItem>,
    private val subItemClickListener: (ListSubItem) -> Unit // Pass a lambda function as a parameter
) : RecyclerView.Adapter<DropdownAdapter.ViewHolder>() {

    private var lastSelectedPosition: Int = RecyclerView.NO_POSITION

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dropdownItemName: TextView = itemView.findViewById(R.id.dropdownItemName)
        val subItemLayout: LinearLayout = itemView.findViewById(R.id.subItemLayout)
        val selectedView: LinearLayout = itemView.findViewById(R.id.selected_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.dropdown_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val subItem = subItems[position]

        holder.dropdownItemName.text = subItem.name

        // Set up click listener for the expand/collapse functionality
        holder.subItemLayout.setOnClickListener {
            // Update the visibility of selectedView for the last selected item
            if (lastSelectedPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(lastSelectedPosition)
            }

            // Toggle the visibility of selectedView based on isSelected
            holder.selectedView.visibility = View.VISIBLE

            // Update the last selected position
            lastSelectedPosition = holder.adapterPosition

            // Handle item click by calling the lambda function
            subItemClickListener(subItem)
        }

        // Set the initial visibility of selectedView
        holder.selectedView.visibility = if (lastSelectedPosition == position) View.VISIBLE else View.INVISIBLE
    }

    override fun getItemCount(): Int {
        return subItems.size
    }
}
