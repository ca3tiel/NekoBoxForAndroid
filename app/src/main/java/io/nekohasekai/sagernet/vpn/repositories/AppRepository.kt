package io.nekohasekai.sagernet.vpn.repositories

import android.content.SharedPreferences
import android.util.ArrayMap
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import io.nekohasekai.sagernet.BuildConfig
import io.nekohasekai.sagernet.vpn.serverlist.ListItem
import io.nekohasekai.sagernet.vpn.serverlist.MyAdapter
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object AppRepository {
    var LogTag: String = "HAMED_LOG"
    var appName: String = "UnitaVPN"
    private var subscriptionLink: String = "https://Apanel.holyip.workers.dev/link/9RTsfMryrGwgWZVb48eN?config=1"
    private var apiServersListUrl: String = "https://api.unitavpn.com/api/servers"
    private var baseUrl: String = "https://api.unitavpn.com/"
    private var userLoginUrl: String = "https://unitavpn.com/api/client/token"
    private var userRegisterUrl: String = "https://api.unitavpn.com/api/auth/register"
    private var userVerifyUrl: String = "https://api.unitavpn.com/api/auth/verify"
    private var userResetPasswordUrl: String = "https://api.unitavpn.com/api/auth/reset"
    private var userStateUrl: String = "https://unitavpn.com/api/client/stats"
    private var panelApiHeaderToken: String = "9f8a833ca1383c5449e1d8800b45fd54"
    private var panelSettingsUrl = "https://api.unitavpn.com/api/settings"
    var selectedServerId: Long = -1
    var ShareCustomMessage: String = "Share $appName whit your friends and family"
    var ShareApplicationLink: String = "https://play.google.com/store/apps/details?id=com.File.Manager.Filemanager&pcampaignid=web_share"
    var telegramLink: String = "https://t.me/unitavpn"
    var allServers: MutableList<ListItem> = mutableListOf()
    var allServersOriginal: MutableList<ListItem> = mutableListOf()
    lateinit var allServersRaw: JsonObject
    lateinit var recyclerView: RecyclerView
    var isBestServerSelected: Boolean = false
    lateinit var sharedPreferences: SharedPreferences
    var isConnected: Boolean = false
    var filterServersBy: String = "all"
    var appVersionCode: Int = 0
    var appShouldForceUpdate: Boolean = false
    private var isInternetConnected = true

    fun setBaseUrl(url: String) {
        baseUrl = url
    }

    fun getAllServer(): MutableList<ListItem> {
        val allServersString = sharedPreferences.getString("allServers", null)
        if(allServersString === null) {
            filterServersByTag(filterServersBy)
            return allServers
        }
        val gson = Gson()
        val itemType = object : TypeToken<MutableList<ListItem>>() {}.type
        val allServersList = gson.fromJson<MutableList<ListItem>>(allServersString, itemType)
        sharedPreferences.edit().remove("allServers").apply()
        allServers = allServersList
        return allServersList
    }

    fun setAllServer(servers: MutableList<ListItem>) {
        val gson = Gson()
        val allServersInJson = gson.toJson(servers)
        sharedPreferences.edit().putString("allServers", allServersInJson).apply()
    }

    fun setSubscriptionLink(url: String) {
        subscriptionLink = url
    }

    fun getSubscriptionLink(): String {
        return subscriptionLink
    }

    private fun getBaseUrl(): String? {
        return baseUrl
    }

    fun getUrl(path: String): String {
        return getBaseUrl() + path
    }

    fun getUserLoginUrl(): String {
        return userLoginUrl
    }

    fun getUserVerifyUrl(): String {
        return userVerifyUrl
    }

    fun getPanelApiHeaderToken(): String {
        return panelApiHeaderToken
    }

    fun getUserRegisterUrl(): String {
        return userRegisterUrl
    }

    fun getUserStateUrl(): String? {
        return userStateUrl
    }

    fun getUserResetPasswordUrl(): String {
        return userResetPasswordUrl
    }

    fun setUserResetPasswordUrl(url: String) {
        userResetPasswordUrl = url
    }

    fun setUserLoginUrl(path: String) {
        userStateUrl = path
    }

    fun setUserRegisterUrl(path: String) {
        userRegisterUrl = path
    }

    fun setUserStateUrl(path: String) {
        userStateUrl = path
    }

    fun setPanelApiHeaderToken(token: String) {
        panelApiHeaderToken = token
    }

    fun getSettings() {
        val client = OkHttpClient()
        val request = getHttpRequest(panelSettingsUrl, null, "GET")
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        throw IOException("Unexpected code ${response.code}")
                    }
                    val gson = Gson()
                    val jsonObject = gson.fromJson(response.body!!.string(), JsonObject::class.java)
                    val baseUrl = jsonObject.get("baseUrl").asString
                    val userLoginUrl = jsonObject.get("userLoginUrl").asString
                    val userRegisterUrl = jsonObject.get("userRegisterUrl").asString
                    val userStateUrl = jsonObject.get("userStateUrl").asString
                    val panelApiHeaderToken = jsonObject.get("panelApiHeaderToken").asString
                    val versionCode = jsonObject.get("versionCode").asInt
                    val forceUnder = jsonObject.get("forceUnder").asInt

                    setBaseUrl(baseUrl)
                    setUserLoginUrl(userLoginUrl)
                    setUserRegisterUrl(userRegisterUrl)
                    setUserStateUrl(userStateUrl)
                    setPanelApiHeaderToken(panelApiHeaderToken)
                    setVersionCode(versionCode, forceUnder)
                }
            }
        })
    }

    fun getSettingsSync(): Int {
        val client = OkHttpClient()
        val url = panelSettingsUrl

        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .get()
            .build()

        try {
            val response: Response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val gson = Gson()
                val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
                val baseUrl = jsonObject.get("baseUrl").asString
                val userLoginUrl = jsonObject.get("userLoginUrl").asString
                val userRegisterUrl = jsonObject.get("userRegisterUrl").asString
                val userStateUrl = jsonObject.get("userStateUrl").asString
                val panelApiHeaderToken = jsonObject.get("panelApiHeaderToken").asString
                val versionCode = jsonObject.get("versionCode").asInt
                val forceUnder = jsonObject.get("forceUnder").asInt

                setBaseUrl(baseUrl)
                setUserLoginUrl(userLoginUrl)
                setUserRegisterUrl(userRegisterUrl)
                setUserStateUrl(userStateUrl)
                setPanelApiHeaderToken(panelApiHeaderToken)
                setVersionCode(versionCode, forceUnder)
            }
            return response.code
        } catch (e: Exception) {
            debugLog("Get_Settings_Request_Failed: ${e.message}")
            return -1
        }
    }

    private fun setVersionCode(versionCodeParam: Int, forceUnderParam: Int) {
        appVersionCode = versionCodeParam
        appShouldForceUpdate = BuildConfig.VERSION_CODE <= forceUnderParam
    }

    fun getServersListAsync() {
        val client = OkHttpClient()
        val request = getHttpRequest(apiServersListUrl, null, "GET")
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        throw IOException("Unexpected code ${response.code}")
                    }
                    val gson = Gson()
                    val jsonObject = gson.fromJson(response.body!!.string(), JsonObject::class.java)
                    val servers = jsonObject.get("servers").asJsonObject
                    servers.entrySet().forEach { it ->
                        println("HAMED_LOG_SERVER: " + it.toString())
                    }
                }
            }
        })
    }

    fun getServersListSync(): Int {
        val client = OkHttpClient()
        val url = apiServersListUrl

        val request = Request.Builder()
            .url(url)
            .header("xmplus-authorization", getPanelApiHeaderToken())
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .get()
            .build()

        try {
            val response: Response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val gson = Gson()
                val serversObject = gson.fromJson(responseBody, JsonObject::class.java)
                val servers = serversObject.get("servers").asJsonObject
                allServersRaw = servers
            }
            return response.code
        } catch (e: Exception) {
            debugLog("Get_Servers_Request_Failed: ${e.message}")
            return -1
        }
    }

    fun getRawServersConfigAsString(): String
    {
        var serversConfigString: MutableList<String> = mutableListOf()
        allServersRaw.entrySet().forEach { it ->
            it.value.asJsonArray.forEach { it ->
                serversConfigString.add(it.asJsonObject.get("config").asString)
            }
        }
        return serversConfigString.joinToString("\n")
    }


    fun getHttpRequest(url: String, formParams: HashMap<String, String>?, requestType: String = "GET"): Request {
        val requestBuilder = FormBody.Builder()

        formParams?.forEach { (key, value) ->
            requestBuilder.add(key, value)
        }

        val formBody = requestBuilder.build()

        val request = Request.Builder()
            .header("xmplus-authorization", getPanelApiHeaderToken())
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .url(url)

        if(requestType == "GET") {
            request.get()
        } else if(requestType == "POST") {
            request.post(formBody)
        }

        return request.build()
    }

    fun flagNameMapper(countryCode: String): String
    {
        val countries = mapOf(
            "de" to "Germany",
            "tr" to "Turkey",
            "bg" to "Bulgaria",
            "fr" to "France",
            "nl" to "Netherlands",
            "ro" to "Romania",
        )
        return countries[countryCode].toString()
    }

    fun countryCodeMapper(countryName: String): String
    {
        val countries = mapOf(
            "Germany" to "de",
            "Turkey" to "tr",
            "Bulgaria" to "bg",
            "France" to "fr",
            "Netherlands" to "nl",
            "Romania" to "ro",
        )
        return countries[countryName].toString()
    }

    fun refreshServersListView()
    {
        val adapter = MyAdapter(getAllServer()) { }
        recyclerView.adapter = adapter
    }

    fun resetAllSubItemsStatus()
    {
        var servers = allServersOriginal
        servers.forEach { item ->
            item.dropdownItems.forEach { subItem ->
                subItem.isSelected = false
            }
        }
        allServers = servers.filter { element -> element in allServers }.toMutableList()
    }

    fun filterServersByTag(tag: String): Unit
    {
        filterServersBy = tag
        var servers = allServersOriginal
        if(tag === "all") {
            allServers = servers
            return
        }
        allServers = servers.map { item ->
            item.copy(dropdownItems = item.dropdownItems.filter { subItem ->
                subItem.tags.contains(tag)
            }.toMutableList())
        }.toMutableList()
    }

    fun setLastInternetSatus(status: Boolean) {
        isInternetConnected = status
    }

    fun isInternetAvailable(): Boolean {
        return isInternetConnected
    }

    fun debugLog(message: String) {
        Log.d(LogTag, message);
    }

    fun setServerPing(serverId: Long, ping: Int, status: Int): ArrayMap<String, String> {
        val arrayMap: ArrayMap<String, String> = ArrayMap()
        arrayMap["countryCode"] = ""
        arrayMap["serverName"] = ""
        allServers.forEach { item ->
            item.dropdownItems.forEach{
                if(it.id == serverId) {
                    arrayMap["countryCode"] = countryCodeMapper(item.name)
                    arrayMap["serverName"] = it.name
                    it.ping = ping
                    it.status = status
                    return arrayMap
                }
            }
        }
        return arrayMap
    }
}
