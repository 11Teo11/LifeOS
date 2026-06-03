package com.example.lifeos.data.agent

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object OllamaClient {

    private const val MODEL = "mistral"

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    // Blocking call — always invoke from Dispatchers.IO
    fun generate(prompt: String, host: String = "10.0.2.2"): String? {
        return try {
            val baseUrl = "http://$host:11434"
            val body = JSONObject().apply {
                put("model", MODEL)
                put("prompt", prompt)
                put("stream", false)
            }.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$baseUrl/api/generate")
                .post(body)
                .build()

            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                JSONObject(response.body?.string() ?: return null).getString("response")
            }
        } catch (e: Exception) {
            null
        }
    }
}