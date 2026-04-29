package com.example.lifeos.util

import com.example.lifeos.data.db.entity.Transaction
import com.opencsv.CSVReader
import java.io.InputStream
import java.io.InputStreamReader

class CsvParser {

    fun parseRevolutCsv(inputStream: InputStream): List<Transaction> {
        val transactions = mutableListOf<Transaction>()

        try {
            val reader = CSVReader(InputStreamReader(inputStream))
            val rows = reader.readAll()

            if (rows.isEmpty()) return emptyList()

            // Prima linie e header — o sărim
            val dataRows = rows.drop(1)

            for (row in dataRows) {
                if (row.size < 4) continue

                try {
                    val date = row[0].trim()
                    val description = row[4].trim()
                    val amount = row[5].trim().toDoubleOrNull() ?: continue
                    val currency = row[7].trim()

                    transactions.add(
                        Transaction(
                            date = date,
                            description = description,
                            amount = amount,
                            currency = currency
                        )
                    )
                } catch (e: Exception) {
                    // Sărim rândul dacă e malformat
                    continue
                }
            }

            reader.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return transactions
    }
}