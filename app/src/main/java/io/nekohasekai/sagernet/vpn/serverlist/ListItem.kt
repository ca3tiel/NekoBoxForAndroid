package io.nekohasekai.sagernet.vpn.serverlist

data class ListItem(var name: String, val dropdownItems: List<String>, var isExpanded: Boolean = false, var iconResId: Int)

