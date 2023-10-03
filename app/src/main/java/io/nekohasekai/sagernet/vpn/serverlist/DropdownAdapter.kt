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
) :
    RecyclerView.Adapter<DropdownAdapter.ViewHolder>() {

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
        holder.dropdownItemName.text = subItems[position].name

        // Set up click listener for the expand/collapse functionality
        holder.subItemLayout.setOnClickListener {
            holder.selectedView.visibility = View.VISIBLE
            notifyItemChanged(holder.adapterPosition)

            // Handle item click by calling the lambda function
            subItemClickListener(subItems[position])
        }
    }

    override fun getItemCount(): Int {
        return subItems.size
    }
}
