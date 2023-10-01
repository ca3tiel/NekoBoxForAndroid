package io.nekohasekai.sagernet.vpn.serverlist

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.SagerDatabase
import io.nekohasekai.sagernet.vpn.repositories.AppRepository

class MyFragment : Fragment() {

    @SuppressLint("DiscouragedApi")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_list, container, false)

        val iconClicked = arguments?.getString("iconClicked")

        var proxyGroup = SagerDatabase.groupDao.getById(1)!!
        var newProfiles = SagerDatabase.proxyDao.getByGroup(proxyGroup.id)

        val allServers = mutableListOf<ListItem>()
        val groupedServers = newProfiles
            .drop(2) // Skip the first 2 items
            .groupBy { item ->
                val serverName = item.displayName()
                serverName.substring(serverName.length - 5, serverName.length).substring(0, 2).lowercase()
            }

        groupedServers.forEach { (countryCode, profiles) ->
            val resourceName = "ic_${countryCode}_flag"
            val countryName = AppRepository.flagNameMapper(countryCode)

            val serverList = profiles.map { profile ->
                ListSubItem(profile.id, profile.displayName())
            }

            val listItem = ListItem(
                countryName,
                serverList,
                iconResId = resources.getIdentifier(resourceName, "drawable", context?.packageName)
            )
            listItem.setOnClickListener {
//                println("HAMED_LOG_1")
            }
            allServers.add(
                listItem
            )
        }

//        val mtnServers = mutableListOf(
//            ListItem("Germany", listOf("Germany 1", "Germany 2", "Germany 3", "Germany 4"), iconResId = R.drawable.ic_de_flag),
//            ListItem("France", listOf("France 1", "France 2", "France 3", "France 4"), iconResId = R.drawable.ic_fr_flag)
//        )
//
//        val mciServers = mutableListOf(
//            ListItem("Netherlands", listOf("Netherlands 1", "Netherlands 2", "Netherlands 3", "Netherlands 4"), iconResId = R.drawable.ic_nl_flag)
//        )

        val recyclerView: RecyclerView = rootView.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        val adapter = MyAdapter(allServers) { clickedItem ->
            println("HAMED_LOG_3")
        }
        recyclerView.adapter = adapter

        return rootView
    }
}
