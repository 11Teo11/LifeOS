package com.example.lifeos.data.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class OllamaService {

    companion object {
        private const val BASE_URL = "http://10.0.2.2:11434"
        private const val MODEL = "mistral"
    }

    suspend fun classify(transactionDescription: String, amount: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("OllamaService", "Classifying: $transactionDescription")
                val prompt = buildPrompt(transactionDescription, amount)
                val response = sendRequest(prompt)
                Log.d("OllamaService", "Category: $response")
                parseCategory(response)
            } catch (e: Exception) {
                Log.e("OllamaService", "Error: ${e.message}")
                "📦 Other"
            }
        }
    }

    private fun buildPrompt(description: String, amount: Double): String {
        return """
            You are a transaction classifier. Classify this bank transaction into exactly one category.
            
            Transaction: "$description" Amount: $amount RON
            
            Categories: 🍔 Food, 🚌 Transport, 🎬 Entertainment, 🛍️ Shopping, 💊 Health, 📚 Education, 📦 Other
            
            Reply with ONLY the category name, nothing else. Example: 🍔 Food
        """.trimIndent()
    }

    private fun sendRequest(prompt: String): String {
        Log.d("OllamaService", "Sending request to Ollama...")
        val url = URL("$BASE_URL/api/generate")
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.connectTimeout = 30000
        connection.readTimeout = 60000

        val body = JSONObject().apply {
            put("model", MODEL)
            put("prompt", prompt)
            put("stream", false)
        }.toString()

        OutputStreamWriter(connection.outputStream).use { it.write(body) }

        val response = BufferedReader(InputStreamReader(connection.inputStream)).use {
            it.readText()
        }

        Log.d("OllamaService", "Response: $response")
        return JSONObject(response).getString("response").trim()
    }

    private fun parseCategory(response: String): String {
        val validCategories = listOf(
            "🍔 Food", "🚌 Transport", "🎬 Entertainment",
            "🛍️ Shopping", "💊 Health", "📚 Education", "📦 Other"
        )

        return validCategories.firstOrNull {
            response.contains(it, ignoreCase = true)
        } ?: "📦 Other"
    }
}