package com.example.lifeos.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CsvParserTest {

    private lateinit var parser: CsvParser

    // Column layout: 0=Date, 1=Description, 2=Category, 3=Money in/out
    private val validHeader =
        "Date,Description,Category,Money in/out"

    @Before
    fun setUp() {
        parser = CsvParser()
    }

    @Test
    fun `empty file returns EmptyFile`() {
        val result = parser.parseRevolutCsv("".byteInputStream())
        assertTrue(result is CsvParseResult.EmptyFile)
    }

    @Test
    fun `header with no Revolut columns returns InvalidFormat`() {
        val csv = "Col1,Col2,Col3,Col4\nA,B,C,D"
        val result = parser.parseRevolutCsv(csv.byteInputStream())
        assertTrue(result is CsvParseResult.InvalidFormat)
    }

    @Test
    fun `header only with no data rows returns InvalidFormat`() {
        val result = parser.parseRevolutCsv("$validHeader\n".byteInputStream())
        assertTrue(result is CsvParseResult.InvalidFormat)
    }

    @Test
    fun `valid row returns Success with correct transaction fields`() {
        val csv = "$validHeader\n2024-01-15,Starbucks,Food,-5.50 RON"
        val result = parser.parseRevolutCsv(csv.byteInputStream())

        assertTrue(result is CsvParseResult.Success)
        val transactions = (result as CsvParseResult.Success).transactions
        assertEquals(1, transactions.size)
        assertEquals("2024-01-15", transactions[0].date)
        assertEquals("Starbucks", transactions[0].description)
        assertEquals(-5.50, transactions[0].amount, 0.001)
        assertEquals("RON", transactions[0].currency)
    }

    @Test
    fun `row with fewer than 4 columns is skipped and returns InvalidFormat`() {
        val csv = "$validHeader\n2024-01-15,Coffee"
        val result = parser.parseRevolutCsv(csv.byteInputStream())
        assertTrue(result is CsvParseResult.InvalidFormat)
    }

    @Test
    fun `row with non-numeric amount is skipped and returns InvalidFormat`() {
        val csv = "$validHeader\n2024-01-15,Coffee,Food,NOT_A_NUMBER"
        val result = parser.parseRevolutCsv(csv.byteInputStream())
        assertTrue(result is CsvParseResult.InvalidFormat)
    }

    @Test
    fun `multiple valid rows returns Success with all transactions`() {
        val csv = """
            $validHeader
            2024-01-15,Coffee,Food,-5.50 RON
            2024-01-16,Groceries,Food,-50.00 RON
            2024-01-17,Rent,Housing,-500.00 RON
        """.trimIndent()

        val result = parser.parseRevolutCsv(csv.byteInputStream())

        assertTrue(result is CsvParseResult.Success)
        assertEquals(3, (result as CsvParseResult.Success).transactions.size)
    }

    @Test
    fun `mix of valid and short rows returns only valid transactions`() {
        val csv = """
            $validHeader
            2024-01-15,Coffee,Food,-5.50 RON
            BAD_ROW,OnlyTwoColumns
            2024-01-17,Rent,Housing,-500.00 RON
        """.trimIndent()

        val result = parser.parseRevolutCsv(csv.byteInputStream())

        assertTrue(result is CsvParseResult.Success)
        assertEquals(2, (result as CsvParseResult.Success).transactions.size)
    }
}