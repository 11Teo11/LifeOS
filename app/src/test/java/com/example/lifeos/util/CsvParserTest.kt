package com.example.lifeos.util

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CsvParserTest {

    private lateinit var parser: CsvParser

    // Revolut CSV column layout: 0=Date, 1=Time, 2=Type, 3=Product,
    //                            4=Description, 5=Amount, 6=Fee, 7=Currency, 8=State, 9=Balance
    private val header = "Date,Time,Type,Product,Description,Amount,Fee,Currency,State,Balance"

    @Before
    fun setUp() {
        parser = CsvParser()
    }

    @Test
    fun `empty file returns empty list`() {
        val result = parser.parseRevolutCsv("".byteInputStream())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `header only returns empty list`() {
        val result = parser.parseRevolutCsv("$header\n".byteInputStream())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `valid row returns correct transaction`() {
        val csv = """
            $header
            2024-01-15,10:30:00,CARD_PAYMENT,Current,Starbucks,-5.50,0.00,RON,COMPLETED,100.00
        """.trimIndent()

        val result = parser.parseRevolutCsv(csv.byteInputStream())

        assertEquals(1, result.size)
        assertEquals("2024-01-15", result[0].date)
        assertEquals("Starbucks", result[0].description)
        assertEquals(-5.50, result[0].amount, 0.001)
        assertEquals("RON", result[0].currency)
    }

    @Test
    fun `row with fewer than 4 columns is skipped`() {
        val csv = "$header\n2024-01-15,10:30:00"
        val result = parser.parseRevolutCsv(csv.byteInputStream())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `row with non-numeric amount is skipped`() {
        val csv = "$header\n2024-01-15,10:30:00,CARD,Current,Coffee,NOT_A_NUMBER,0.00,RON,COMPLETED,100.00"
        val result = parser.parseRevolutCsv(csv.byteInputStream())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `multiple valid rows returns all transactions`() {
        val csv = """
            $header
            2024-01-15,10:30:00,CARD_PAYMENT,Current,Coffee,-5.50,0.00,RON,COMPLETED,100.00
            2024-01-16,11:00:00,CARD_PAYMENT,Current,Groceries,-50.00,0.00,RON,COMPLETED,50.00
            2024-01-17,09:00:00,CARD_PAYMENT,Current,Rent,-500.00,0.00,RON,COMPLETED,0.00
        """.trimIndent()

        val result = parser.parseRevolutCsv(csv.byteInputStream())

        assertEquals(3, result.size)
    }

    @Test
    fun `mixed valid and invalid rows returns only valid ones`() {
        val csv = """
            $header
            2024-01-15,10:30:00,CARD_PAYMENT,Current,Coffee,-5.50,0.00,RON,COMPLETED,100.00
            2024-01-16
            2024-01-17,10:00:00,CARD_PAYMENT,Current,Rent,-500.00,0.00,RON,COMPLETED,0.00
        """.trimIndent()

        val result = parser.parseRevolutCsv(csv.byteInputStream())

        assertEquals(2, result.size)
    }
}