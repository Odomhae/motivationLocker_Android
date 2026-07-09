package com.odom.motivationlocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuoteRepositoryTest {

    @Test
    fun `parses valid quote array`() {
        val json = """
            [
              { "id": 1, "quote": "Test quote", "writer": "- Someone" },
              { "id": 2, "quote": "Another quote", "writer": "- Someone Else" }
            ]
        """.trimIndent()

        val quotes = QuoteRepository.parseQuotes(json)

        assertEquals(2, quotes.size)
        assertEquals(Quote(1, "Test quote", "- Someone"), quotes[0])
        assertEquals(Quote(2, "Another quote", "- Someone Else"), quotes[1])
    }

    @Test
    fun `skips malformed entries but keeps valid ones`() {
        val json = """
            [
              { "id": 1, "quote": "Valid quote", "writer": "- Someone" },
              { "id": 2, "writer": "- Missing quote field" },
              { "id": 3, "quote": "Another valid quote", "writer": "- Someone Else" }
            ]
        """.trimIndent()

        val quotes = QuoteRepository.parseQuotes(json)

        assertEquals(2, quotes.size)
        assertEquals(1, quotes[0].id)
        assertEquals(3, quotes[1].id)
    }

    @Test
    fun `returns empty list for invalid json`() {
        val quotes = QuoteRepository.parseQuotes("not json")

        assertTrue(quotes.isEmpty())
    }

    @Test
    fun `returns empty list for empty array`() {
        val quotes = QuoteRepository.parseQuotes("[]")

        assertTrue(quotes.isEmpty())
    }
}
