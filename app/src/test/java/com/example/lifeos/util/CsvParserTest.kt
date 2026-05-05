package com.example.lifeos.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CsvParserTest {

    private lateinit var parser: CsvParser

    // Column layout: 0=Type, 1=Product, 2=Started Date, 3=Completed Date,
    //                4=Description, 5=Amount, 6=Fee, 7=Currency
    private val validHeader =
        "Type,Product,Started Date,Completed Date,Description,Amount,Fee,Currency"

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
        val csv = "Col1,Col2,Col3,Col4,Col5,Col6,Col7,Col8\nA,B,C,D,E,10.0,F,RON"
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
        val csv = "$validHeader\nCARD_PAYMENT,Current,2024-01-15,2024-01-15,Starbucks,-5.50,0.00,RON"
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
    fun `row with fewer than 8 columns is skipped and returns InvalidFormat`() {
        val csv = "$validHeader\nCARD_PAYMENT,Current"
        val result = parser.parseRevolutCsv(csv.byteInputStream())
        assertTrue(result is CsvParseResult.InvalidFormat)
    }

    @Test
    fun `row with non-numeric amount is skipped and returns InvalidFormat`() {
        val csv = "$validHeader\nCARD_PAYMENT,Current,2024-01-15,2024-01-15,Coffee,NOT_A_NUMBER,0.00,RON"
        val result = parser.parseRevolutCsv(csv.byteInputStream())
        assertTrue(result is CsvParseResult.InvalidFormat)
    }

    @Test
    fun `multiple valid rows returns Success with all transactions`() {
        val csv = """
            $validHeader
            CARD_PAYMENT,Current,2024-01-15,2024-01-15,Coffee,-5.50,0.00,RON
            CARD_PAYMENT,Current,2024-01-16,2024-01-16,Groceries,-50.00,0.00,RON
            CARD_PAYMENT,Current,2024-01-17,2024-01-17,Rent,-500.00,0.00,RON
        """.trimIndent()

        val result = parser.parseRevolutCsv(csv.byteInputStream())

        assertTrue(result is CsvParseResult.Success)
        assertEquals(3, (result as CsvParseResult.Success).transactions.size)
    }

    @Test
    fun `mix of valid and short rows returns only valid transactions`() {
        val csv = """
            $validHeader
            CARD_PAYMENT,Current,2024-01-15,2024-01-15,Coffee,-5.50,0.00,RON
            BAD_ROW,OnlyTwoColumns
            CARD_PAYMENT,Current,2024-01-17,2024-01-17,Rent,-500.00,0.00,RON
        """.trimIndent()

        val result = parser.parseRevolutCsv(csv.byteInputStream())

        assertTrue(result is CsvParseResult.Success)
        assertEquals(2, (result as CsvParseResult.Success).transactions.size)
    }
}