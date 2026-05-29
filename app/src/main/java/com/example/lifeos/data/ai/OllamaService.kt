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

class OllamaService(private val host: String = "10.0.2.2") {

    companion object {
        private const val MODEL = "llama3.2:1b"
    }

    private val baseUrl get() = "http://$host:11434"

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
You are a bank transaction classifier. Classify into exactly one category.
Respond with ONLY the category name, nothing else.

Transaction: "$description" Amount: $amount RON

Rules (follow strictly):
🍔 Food: supermarkets, grocery stores, restaurants, food delivery, cafes
🚌 Transport: buses, trams, metro, trains, ride sharing, fuel, parking, taxis
🎬 Entertainment: streaming, cinema, games, media subscriptions
🛍️ Shopping: clothing, electronics, online retail, phone top-up, general stores
💊 Health: pharmacy, medical, gym, fitness, dental
📚 Education: books, courses, university, learning platforms
📦 Other: anything not matching above

Critical examples - these MUST be classified exactly as shown:
"Penny" → 🍔 Food
"STB" → 🚌 Transport
"Kaufland" → 🍔 Food
"Carrefour" → 🍔 Food
"Mega Image" → 🍔 Food
"Lidl" → 🍔 Food
"Auchan" → 🍔 Food
"Glovo" → 🍔 Food
"Tazz" → 🍔 Food
"Uber" → 🚌 Transport
"Bolt" → 🚌 Transport
"Metrorex" → 🚌 Transport
"CFR" → 🚌 Transport
"Netflix" → 🎬 Entertainment
"Spotify" → 🎬 Entertainment
"Top-Up" → 🛍️ Shopping

Reply with ONLY the category. Example: 🍔 Food
        """.trimIndent()
    }

    private fun sendRequest(prompt: String): String {
        Log.d("OllamaService", "Sending request to Ollama...")
        val url = URL("$baseUrl/api/generate")
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