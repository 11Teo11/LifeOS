package com.example.lifeos.util

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CsvParserParametrizedTest(
    private val amountStr: String,
    private val shouldParse: Boolean,
    private val expectedAmount: Double
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "amount=\"{0}\" → shouldParse={1}")
        fun data() = listOf(
            arrayOf("100.00",    true,   100.00),
            arrayOf("-25.50",    true,   -25.50),
            arrayOf("0.00",      true,   0.00),
            arrayOf("999999.99", true,   999999.99),
            arrayOf("",          false,  0.0),
            arrayOf("abc",       false,  0.0),
            arrayOf("1,000.00",  false,  0.0)   // commas not valid as decimal separator
        )
    }

    private val parser = CsvParser()
    private val header = "Date,Time,Type,Product,Description,Amount,Fee,Currency,State,Balance"

    @Test
    fun `amount is parsed correctly or row is skipped`() {
        val csv = "$header\n2024-01-15,10:00:00,CARD,Current,Test,$amountStr,0.00,RON,COMPLETED,0.00"
        val result = parser.parseRevolutCsv(csv.byteInputStream())

        if (shouldParse) {
            assertEquals(1, result.size)
            assertEquals(expectedAmount, result[0].amount, 0.001)
        } else {
            assertTrue("Expected row to be skipped for amount=\"$amountStr\"", result.isEmpty())
        }
    }
}