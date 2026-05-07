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

    // Clasificare individuala (pastrata pentru compatibilitate)
    suspend fun classify(transactionDescription: String, amount: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("OllamaService", "Classifying: $transactionDescription")
                val prompt = buildSinglePrompt(transactionDescription, amount)
                val response = sendRequest(prompt)
                Log.d("OllamaService", "Category: $response")
                parseCategory(response)
            } catch (e: Exception) {
                Log.e("OllamaService", "Error: ${e.message}")
                "📦 Other"
            }
        }
    }

    // Clasificare batch — un singur apel pentru mai multe tranzactii
    suspend fun classifyBatch(descriptions: List<String>): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("OllamaService", "Batch classifying ${descriptions.size} transactions")
                val prompt = buildBatchPrompt(descriptions)
                val response = sendRequest(prompt)
                Log.d("OllamaService", "Batch response: $response")
                parseBatchResponse(response, descriptions.size)
            } catch (e: Exception) {
                Log.e("OllamaService", "Batch error: ${e.message}")
                List(descriptions.size) { "📦 Other" }
            }
        }
    }

    private fun buildBatchPrompt(descriptions: List<String>): String {
        val transactionLines = descriptions.mapIndexed { index, desc ->
            "${index + 1}. $desc"
        }.joinToString("\n")

        return """
You are a bank transaction classifier. Classify each transaction into exactly one category.

Categories:
🍔 Food - supermarkets, restaurants, food delivery, groceries, cafes
🚌 Transport - ride sharing, public transport, fuel, parking, taxi
🎬 Entertainment - streaming services, cinema, games, media subscriptions
🛍️ Shopping - clothing, electronics, online retail, general stores
💊 Health - pharmacy, medical services, gym, fitness
📚 Education - books, courses, university, learning platforms
📦 Other - anything that doesn't fit above

Examples:
- Kaufland, Penny, Carrefour, Lidl, Mega Image, Profi → 🍔 Food
- Glovo, Tazz, Bolt Food → 🍔 Food
- Uber, Bolt, STB, RATB, CFR, Metrorex → 🚌 Transport
- Netflix, Spotify, HBO, Disney → 🎬 Entertainment
- Top-Up, phone credit → 🛍️ Shopping

Transactions to classify:
$transactionLines

Reply with ONLY the category for each transaction, one per line, in the same order.
Format: just the category name, nothing else.
Example response for 3 transactions:
🍔 Food
🚌 Transport
🎬 Entertainment
        """.trimIndent()
    }

    private fun buildSinglePrompt(description: String, amount: Double): String {
        return """
You are a bank transaction classifier. Classify into exactly one category.

Transaction: "$description" Amount: $amount RON

Categories:
🍔 Food - supermarkets, restaurants, food delivery, groceries, cafes
🚌 Transport - ride sharing, public transport, fuel, parking, taxi
🎬 Entertainment - streaming services, cinema, games, media subscriptions
🛍️ Shopping - clothing, electronics, online retail, general stores
💊 Health - pharmacy, medical services, gym, fitness
📚 Education - books, courses, university, learning platforms
📦 Other - anything that doesn't fit above

Examples:
- Any supermarket or grocery store (Kaufland, Penny, Carrefour, Lidl, Mega Image, Profi) → 🍔 Food
- Any food delivery app (Glovo, Tazz, Bolt Food) → 🍔 Food
- Any ride sharing or public transport (Uber, Bolt, STB, RATB, CFR, bus, tram, metro) → 🚌 Transport
- Any streaming or music service (Netflix, Spotify, HBO, Disney) → 🎬 Entertainment
- Top-Up, phone credit, mobile recharge → 🛍️ Shopping

Reply with ONLY the category name. Example: 🍔 Food
        """.trimIndent()
    }

    private fun parseBatchResponse(response: String, expectedCount: Int): List<String> {
        val validCategories = listOf(
            "🍔 Food", "🚌 Transport", "🎬 Entertainment",
            "🛍️ Shopping", "💊 Health", "📚 Education", "📦 Other"
        )

        val lines = response.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val results = mutableListOf<String>()
        for (line in lines) {
            val matched = validCategories.firstOrNull { line.contains(it, ignoreCase = true) }
            if (matched != null) results.add(matched)
            if (results.size == expectedCount) break
        }

        while (results.size < expectedCount) {
            results.add("📦 Other")
        }

        return results
    }

    private fun sendRequest(prompt: String): String {
        Log.d("OllamaService", "Sending request to Ollama...")
        val url = URL("$baseUrl/api/generate")
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.connectTimeout = 30000
        connection.readTimeout = 120000

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