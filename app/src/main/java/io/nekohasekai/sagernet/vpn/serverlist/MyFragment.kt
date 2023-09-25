package io.nekohasekai.sagernet.vpn.serverlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.nekohasekai.sagernet.R

class MyFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_list, container, false)

        val iconClicked = arguments?.getString("iconClicked")

        // Initialize item lists
        val Allservers = mutableListOf(
            ListItem("Germany", listOf("Germany 1", "Germany 2", "Germany 3", "Germany 4"), iconResId = R.drawable.ic_germany_flag),
            ListItem("France", listOf("France 1", "France 2", "France 3", "France 4"), iconResId = R.drawable.ic_france_flag),
            ListItem("Netherlands", listOf("Netherlands 1", "Netherlands 2", "Netherlands 3", "Netherlands 4"), iconResId = R.drawable.ic_netherlands_flag)
        )

        val MTNservers = mutableListOf(
            ListItem("Germany", listOf("Germany 1", "Germany 2", "Germany 3", "Germany 4"), iconResId = R.drawable.ic_germany_flag),
            ListItem("France", listOf("France 1", "France 2", "France 3", "France 4"), iconResId = R.drawable.ic_france_flag)
        )

        val MCIservers = mutableListOf(
            ListItem("Netherlands", listOf("Netherlands 1", "Netherlands 2", "Netherlands 3", "Netherlands 4"), iconResId = R.drawable.ic_netherlands_flag)
        )

        val recyclerView: RecyclerView = rootView.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        val adapter = when (iconClicked) {
            "IVMTN" -> MyAdapter(MTNservers)
            "IVMCI" -> MyAdapter(MCIservers)
            else -> MyAdapter(Allservers)
        }
        recyclerView.adapter = adapter

        return rootView
    }
}
