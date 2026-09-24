package com.example

import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.util.ChatExportManager
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChatExportManagerTest {

    @Test
    fun testPlainTextExportFormatting() {
        val session = ChatSessionEntity(id = 1, title = "Test Conversation")
        val messages = listOf(
            ChatMessageEntity(id = 1, sessionId = 1, role = "user", content = "Hello Chottu!", timestamp = 1700000000000L),
            ChatMessageEntity(id = 2, sessionId = 1, role = "assistant", content = "Hello! How can I help you today?", timestamp = 1700000005000L)
        )

        val plainText = ChatExportManager.generatePlainTextExport(session, messages)

        assertTrue(plainText.contains("CHOTTU AI - CONVERSATION EXPORT"))
        assertTrue(plainText.contains("Test Conversation"))
        assertTrue(plainText.contains("USER:"))
        assertTrue(plainText.contains("Hello Chottu!"))
        assertTrue(plainText.contains("CHOTTU AI:"))
        assertTrue(plainText.contains("Hello! How can I help you today?"))
    }

    @Test
    fun testJsonExportStructure() {
        val session = ChatSessionEntity(id = 42, title = "Project Brainstorm")
        val messages = listOf(
            ChatMessageEntity(id = 101, sessionId = 42, role = "user", content = "Tell me an idea", timestamp = 1700000000000L)
        )

        val jsonString = ChatExportManager.generateJsonExport(session, messages)
        val json = JSONObject(jsonString)

        assertEquals("Chottu AI", json.getString("app"))
        assertEquals("1.0", json.getString("version"))
        assertEquals(42, json.getJSONObject("session").getInt("id"))
        assertEquals("Project Brainstorm", json.getJSONObject("session").getString("title"))

        val messagesArray = json.getJSONArray("messages")
        assertEquals(1, messagesArray.length())
        val firstMessage = messagesArray.getJSONObject(0)
        assertEquals("user", firstMessage.getString("role"))
        assertEquals("Tell me an idea", firstMessage.getString("content"))
    }
}
