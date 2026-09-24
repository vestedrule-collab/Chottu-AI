package com.example

import com.example.data.remote.GeminiService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationTitleTest {

    private val geminiService = GeminiService()

    @Test
    fun testTitleSanitizationRemovesQuotesAndFormatting() {
        val raw1 = "\"Quantum Computing Basics\""
        assertEquals("Quantum Computing Basics", geminiService.sanitizeTitle(raw1))

        val raw2 = "**Mars Rover Exploration**."
        assertEquals("Mars Rover Exploration", geminiService.sanitizeTitle(raw2))

        val raw3 = "Title: Healthy Breakfast Ideas"
        assertEquals("Healthy Breakfast Ideas", geminiService.sanitizeTitle(raw3))

        val raw4 = "'Python Async Programming'..."
        assertEquals("Python Async Programming", geminiService.sanitizeTitle(raw4))
    }

    @Test
    fun testTitleLengthCap() {
        val veryLongTitle = "This is an extremely long title that exceeds the desired maximum length for sidebar navigation"
        val sanitized = geminiService.sanitizeTitle(veryLongTitle)
        assertTrue(sanitized.length <= 38)
    }
}
