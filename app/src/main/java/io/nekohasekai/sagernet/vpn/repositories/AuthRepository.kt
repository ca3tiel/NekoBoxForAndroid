package io.nekohasekai.sagernet.vpn.repositories

import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response


object AuthRepository {
    private var token: String? = null
    private lateinit var email: String
    private lateinit var lastValidationError: String

    private fun setUserToken(data: String) {
        AppRepository.sharedPreferences.edit().putString("userToken", data).apply()
        token = data
    }

    fun clearUserToken() {
        AppRepository.sharedPreferences.edit().remove("userToken").apply()
        token = null
    }

    private fun setLastValidationError(data: String) {
        lastValidationError = data
    }
    fun setUserEmail(data: String) {
        email = data
    }

    fun getUserToken(): String? {
        return AppRepository.sharedPreferences.getString("userToken", null)
    }
    fun getLastValidationError(): String? {
        return lastValidationError
    }

    fun getUserEmail(): String {
        return email
    }

    fun login(email: String, password: String): Int {
        val requestBuilder = FormBody.Builder()
        requestBuilder.add("email", email)
        requestBuilder.add("passwd", password)
        val formBody = requestBuilder.build()

        val client = OkHttpClient()
        val url = AppRepository.getUserLoginUrl()

        val request = Request.Builder()
            .url(url)
            .header("xmplus-authorization", AppRepository.getPanelApiHeaderToken())
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .post(formBody)
            .build()

        try {
            val response: Response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val gson = Gson()
                val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
                val dataJsonObject = gson.fromJson(jsonObject.get("data").asJsonObject, JsonObject::class.java)
                val token = dataJsonObject.get("token").asString
                setUserToken(token)

            }
            return response.code;
        } catch (e: Exception) {
            println("Request failed: ${e.message}")
            return -1;
        }
    }

    fun register(email: String, password: String): Int {
        val requestBuilder = FormBody.Builder()
        requestBuilder.add("email", email)
        requestBuilder.add("passwd", password)
        val formBody = requestBuilder.build()

        val client = OkHttpClient()
        val url = AppRepository.getUserRegisterUrl()

        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .post(formBody)
            .build()

        try {
            val response: Response = client.newCall(request).execute()
            if(response.code == 422) {
                val responseBody = response.body?.string()
                val gson = Gson()
                val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
                val errors = gson.fromJson(jsonObject.get("errors").asJsonObject, JsonObject::class.java)
                setLastValidationError(errors.asJsonObject.entrySet().first().value.asString)
            }
            return response.code
        } catch (e: Exception) {
            println("Request failed: ${e.message}")
            return -1
        }
    }

    fun verify(email: String, verifyCode: String): Int {
        val requestBuilder = FormBody.Builder()
        requestBuilder.add("email", email)
        requestBuilder.add("code", verifyCode)
        val formBody = requestBuilder.build()

        val client = OkHttpClient()
        val url = AppRepository.getUserVerifyUrl()

        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .post(formBody)
            .build()

        try {
            val response: Response = client.newCall(request).execute()
            if(response.isSuccessful) {
                val responseBody = response.body?.string()
                val gson = Gson()
                val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
                val token = jsonObject.get("token").asString
                setUserToken(token)
            }
            return response.code
        } catch (e: Exception) {
            println("Request failed: ${e.message}")
            return -1
        }
    }

    fun sendResetPasswordEmail(email: String): Int {
        val requestBuilder = FormBody.Builder()
        requestBuilder.add("email", email)
        val formBody = requestBuilder.build()

        val client = OkHttpClient()
        val url = AppRepository.getUserResetPasswordUrl()

        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .post(formBody)
            .build()

        try {
            val response: Response = client.newCall(request).execute()
            return response.code
        } catch (e: Exception) {
            println("Request failed: ${e.message}")
            return -1
        }
    }
}
