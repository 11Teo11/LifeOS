package com.example.lifeos.util

import com.example.lifeos.data.db.entity.Transaction
import com.opencsv.CSVReader
import java.io.InputStream
import java.io.InputStreamReader

sealed class CsvParseResult {
    data class Success(val transactions: List<Transaction>) : CsvParseResult()
    object InvalidFormat : CsvParseResult()
    object EmptyFile : CsvParseResult()
}

class CsvParser {

    // Headerul real din noul format Revolut
    private val REVOLUT_HEADER_COLUMNS = listOf("Date", "Description", "Category", "Money in/out")

    fun parseRevolutCsv(inputStream: InputStream): CsvParseResult {
        try {
            val reader = CSVReader(InputStreamReader(inputStream))
            val rows = reader.readAll()
            reader.close()

            if (rows.isEmpty()) return CsvParseResult.EmptyFile

            // Gasim randul cu headerul real (cel care contine "Date", "Description", etc.)
            val headerRowIndex = rows.indexOfFirst { row ->
                val cells = row.map { it.trim() }
                REVOLUT_HEADER_COLUMNS.all { expected ->
                    cells.any { it.equals(expected, ignoreCase = true) }
                }
            }

            if (headerRowIndex == -1) return CsvParseResult.InvalidFormat

            val header = rows[headerRowIndex].map { it.trim() }

            // Gasim indicii coloanelor
            val dateIndex = header.indexOfFirst { it.equals("Date", ignoreCase = true) }
            val descIndex = header.indexOfFirst { it.equals("Description", ignoreCase = true) }
            val amountIndex = header.indexOfFirst { it.equals("Money in/out", ignoreCase = true) }

            if (dateIndex == -1 || descIndex == -1 || amountIndex == -1) {
                return CsvParseResult.InvalidFormat
            }

            val transactions = mutableListOf<Transaction>()
            val dataRows = rows.drop(headerRowIndex + 1)

            for (row in dataRows) {
                if (row.size <= amountIndex) continue
                try {
                    val date = row[dateIndex].trim()
                    val description = row[descIndex].trim()
                    val amountRaw = row[amountIndex].trim()

                    if (date.isBlank() || description.isBlank() || amountRaw.isBlank()) continue

                    // Parsam suma: "-37.50 RON" sau "120.00 RON"
                    val currency = if (amountRaw.contains("RON")) "RON"
                    else amountRaw.filter { it.isLetter() }.ifBlank { "RON" }

                    val amountStr = amountRaw
                        .replace(currency, "")
                        .replace(",", "")
                        .trim()

                    val amount = amountStr.toDoubleOrNull() ?: continue

                    transactions.add(
                        Transaction(
                            date = date,
                            description = description,
                            amount = amount,
                            currency = currency
                        )
                    )
                } catch (e: Exception) {
                    continue
                }
            }

            if (transactions.isEmpty()) return CsvParseResult.InvalidFormat
            return CsvParseResult.Success(transactions)

        } catch (e: Exception) {
            return CsvParseResult.InvalidFormat
        }
    }
}