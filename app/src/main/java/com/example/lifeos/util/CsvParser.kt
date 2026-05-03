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

    private val REVOLUT_HEADERS = listOf(
        "Type", "Product", "Started Date", "Completed Date",
        "Description", "Amount", "Fee", "Currency"
    )

    fun parseRevolutCsv(inputStream: InputStream): CsvParseResult {
        try {
            val reader = CSVReader(InputStreamReader(inputStream))
            val rows = reader.readAll()
            reader.close()

            if (rows.isEmpty()) return CsvParseResult.EmptyFile

            val header = rows[0].map { it.trim() }
            val isValidRevolut = REVOLUT_HEADERS.any { expectedHeader ->
                header.any { it.equals(expectedHeader, ignoreCase = true) }
            }

            if (!isValidRevolut) return CsvParseResult.InvalidFormat

            val transactions = mutableListOf<Transaction>()
            val dataRows = rows.drop(1)

            for (row in dataRows) {
                if (row.size < 8) continue
                try {
                    val date = row[0].trim()
                    val description = row[4].trim()
                    val amount = row[5].trim().toDoubleOrNull() ?: continue
                    val currency = row[7].trim()

                    if (date.isBlank() || description.isBlank()) continue

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