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
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.vpn.repositories.AppRepository
import kotlinx.coroutines.sync.Mutex

class MyAdapter(
    private val itemList: List<ListItem>,
    private val itemClickListener: (ListItem) -> Unit // Pass a lambda function as a parameter
) : RecyclerView.Adapter<MyAdapter.ViewHolder>() {
    private var lastSelectedPosition: Int = RecyclerView.NO_POSITION
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemName: TextView = itemView.findViewById(R.id.itemName)
        val itemIcon: ImageView = itemView.findViewById(R.id.itemIcon)
        val expandIcon: ImageView = itemView.findViewById(R.id.expandIcon)
        val dropdownList: RecyclerView = itemView.findViewById(R.id.dropdownList)
        val itemHeader: LinearLayout = itemView.findViewById(R.id.itemHeader)
        val selectedView: LinearLayout = itemView.findViewById(R.id.llSelectedView)
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
        holder.expandIcon.visibility = if (item.isBestServer) View.INVISIBLE else View.VISIBLE

        // Set up click listener for the expand/collapse functionality
        holder.itemHeader.setOnClickListener {

            if(item.isBestServer) {
                holder.selectedView.visibility = View.VISIBLE
            } else {
                item.isExpanded = !isExpanded

                if (lastSelectedPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(lastSelectedPosition)
                }
                holder.selectedView.visibility = View.INVISIBLE
            }

            // Update the last selected position
            lastSelectedPosition = holder.adapterPosition

            // Handle item click by calling the lambda function
            itemClickListener(item)
        }

        holder.selectedView.visibility = if (lastSelectedPosition == position && item.isBestServer) View.VISIBLE else View.INVISIBLE

        // Create and set a RecyclerView adapter for the dropdown list
        val dropdownAdapter = DropdownAdapter(item.dropdownItems) { clickedItem ->
            AppRepository.selectedServerId = clickedItem.id
            DataStore.selectedProxy = clickedItem.id
            if (DataStore.serviceState.connected) {
                SagerNet.reloadService()
            }
        }
        holder.dropdownList.layoutManager = LinearLayoutManager(holder.dropdownList.context)
        holder.dropdownList.adapter = dropdownAdapter
    }

    override fun getItemCount(): Int {
        return itemList.size
    }
}
