package io.nekohasekai.sagernet.vpn.serverlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.nekohasekai.sagernet.R

class MyAdapter(private val itemList: List<ListItem>) :
    RecyclerView.Adapter<MyAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemName: TextView = itemView.findViewById(R.id.itemName)
        val itemIcon: ImageView = itemView.findViewById(R.id.itemIcon)
        val expandIcon: ImageView = itemView.findViewById(R.id.expandIcon)
        val dropdownList: RecyclerView = itemView.findViewById(R.id.dropdownList)
        val itemHeader: LinearLayout = itemView.findViewById(R.id.itemHeader)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemList[position]
        holder.itemName.text = item.name
        holder.itemIcon.setImageResource(item.iconResId)

        val isExpanded = item.isExpanded
        holder.expandIcon.setImageResource(if (isExpanded) R.drawable.ic_minus else R.drawable.ic_plus)
        holder.dropdownList.visibility = if (isExpanded) View.VISIBLE else View.GONE

        // Set up click listener for the expand/collapse functionality
        holder.itemHeader.setOnClickListener {
            item.isExpanded = !isExpanded
            notifyItemChanged(holder.adapterPosition)
        }

        // Create and set a RecyclerView adapter for the dropdown list
        val dropdownAdapter = DropdownAdapter(item.dropdownItems)
        holder.dropdownList.layoutManager = LinearLayoutManager(holder.dropdownList.context)
        holder.dropdownList.adapter = dropdownAdapter
    }

    override fun getItemCount(): Int {
        return itemList.size
    }
}
