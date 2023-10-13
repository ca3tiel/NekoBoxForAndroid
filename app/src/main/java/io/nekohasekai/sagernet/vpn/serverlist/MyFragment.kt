package io.nekohasekai.sagernet.vpn.serverlist

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
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

        arguments?.getString("iconClicked") // Do something with iconClicked if needed

        val proxyGroup = SagerDatabase.groupDao.getById(1)!!
        val newProfiles = SagerDatabase.proxyDao.getByGroup(proxyGroup.id)

        AppRepository.allServers = newProfiles
            .drop(2)
            .groupBy { profile ->
                val serverName = profile.displayName()
                serverName.substring(serverName.length - 5, serverName.length).substring(0, 2).lowercase()
            }
            .map { (countryCode, profiles) ->
                val resourceName = "ic_${countryCode}_flag"
                val countryName = AppRepository.flagNameMapper(countryCode)

                val serverList = profiles.map { profile ->
                    ListSubItem(profile.id, profile.displayName(), profile.status, profile.error, profile.ping)
                }

                ListItem(
                    countryName,
                    serverList.toMutableList(),
                    iconResId = resources.getIdentifier(resourceName, "drawable", context?.packageName)
                )
            }
            .toMutableList()

        AppRepository.recyclerView = rootView.findViewById(R.id.recyclerView)
        AppRepository.recyclerView.layoutManager = LinearLayoutManager(activity)
        AppRepository.refreshServersListView()

        return rootView
    }
}
