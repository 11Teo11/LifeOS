package com.example.lifeos.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CsvParserParametrizedTest(
    private val amountStr: String,
    private val expectSuccess: Boolean,
    private val expectedAmount: Double
) {

    companion object {
        private const val VALID_HEADER =
            "Type,Product,Started Date,Completed Date,Description,Amount,Fee,Currency"

        @JvmStatic
        @Parameterized.Parameters(name = "amount=\"{0}\" → success={1}")
        fun data() = listOf(
            arrayOf<Any>("100.00",    true,  100.00),
            arrayOf<Any>("-25.50",    true,  -25.50),
            arrayOf<Any>("0.00",      true,  0.00),
            arrayOf<Any>("999999.99", true,  999999.99),
            arrayOf<Any>("",          false, 0.0),
            arrayOf<Any>("abc",       false, 0.0),
            arrayOf<Any>("1,000.00",  false, 0.0),
            arrayOf<Any>("100.50.25", false,  0.0)
        )
    }

    private val parser = CsvParser()

    @Test
    fun `amount is parsed correctly or row is skipped`() {
        val csv = "$VALID_HEADER\nCARD_PAYMENT,Current,2024-01-15,2024-01-15,Test,$amountStr,0.00,RON"
        val result = parser.parseRevolutCsv(csv.byteInputStream())

        if (expectSuccess) {
            assertTrue("Expected Success but got $result", result is CsvParseResult.Success)
            val amount = (result as CsvParseResult.Success).transactions[0].amount
            assertEquals(expectedAmount, amount, 0.001)
        } else {
            assertTrue("Expected InvalidFormat but got $result", result is CsvParseResult.InvalidFormat)
        }
    }
}