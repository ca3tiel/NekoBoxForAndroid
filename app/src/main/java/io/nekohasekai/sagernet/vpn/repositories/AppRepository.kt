package io.nekohasekai.sagernet.vpn.repositories

import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

object AppRepository {
    public var appName: String = "UnitaVPN"
    private var subscriptionLink: String = "https://panel.holyip.workers.dev/link/9RTsfMryrGwgWZVb48eN?config=1"
    private var baseUrl: String = "https://panel.holyip.com/"
    private var userLoginUrl: String = "https://panel.holyip.com/api/v2/client/token"
    private var userRegisterUrl: String = "https://panel.miatel.xyz/api/auth/register"
    private var userVerifyUrl: String = "https://panel.miatel.xyz/api/auth/verify"
    private var userResetPasswordUrl: String = "https://panel.miatel.xyz/api/auth/reset"
    private var userStateUrl: String = "https://panel.holyip.com/api/v2/client/stats"
    private var panelApiHeaderToken: String = "9f8a833ca1383c5449e1d8800b45fd54"
    private var panelSettingsUrl = "https://panel.miatel.xyz/api/settings"


    fun setBaseUrl(url: String) {
        baseUrl = url
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

                    setBaseUrl(baseUrl)
                    setUserLoginUrl(userLoginUrl)
                    setUserRegisterUrl(userRegisterUrl)
                    setUserStateUrl(userStateUrl)
                    setPanelApiHeaderToken(panelApiHeaderToken)
                }
            }
        })
    }

    fun getHttpRequest(url: String, formParams: HashMap<String, String>?, requestType: String = "GET"): Request {
        val requestBuilder = FormBody.Builder()

        formParams?.forEach { (key, value) ->
            requestBuilder.add(key, value)
        }

        val formBody = requestBuilder.build()

        val request = Request.Builder()
            .header("XMPus-API-Token", getPanelApiHeaderToken())
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

}
