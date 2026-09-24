package com.example.data.repository

import com.example.data.local.ChatDao
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {
    val allSessions: Flow<List<ChatSessionEntity>> = chatDao.getAllSessions()

    fun getMessages(sessionId: Long): Flow<List<ChatMessageEntity>> {
        return chatDao.getMessagesForSession(sessionId)
    }

    suspend fun getRecentMessages(sessionId: Long, limit: Int = 10): List<ChatMessageEntity> {
        return chatDao.getRecentMessages(sessionId, limit)
    }

    suspend fun createSession(title: String = "New Conversation"): Long {
        val session = ChatSessionEntity(
            title = title,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return chatDao.insertSession(session)
    }

    suspend fun updateSessionTitle(sessionId: Long, newTitle: String) {
        val existing = chatDao.getSessionById(sessionId) ?: return
        chatDao.updateSession(existing.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
    }

    suspend fun touchSession(sessionId: Long) {
        val existing = chatDao.getSessionById(sessionId) ?: return
        chatDao.updateSession(existing.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteSession(session: ChatSessionEntity) {
        chatDao.deleteSession(session)
    }

    suspend fun addMessage(message: ChatMessageEntity): Long {
        touchSession(message.sessionId)
        return chatDao.insertMessage(message)
    }

    suspend fun deleteMessage(messageId: Long) {
        chatDao.deleteMessageById(messageId)
    }
}
