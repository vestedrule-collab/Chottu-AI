package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ChatExportManager {

    fun generatePlainTextExport(
        session: ChatSessionEntity?,
        messages: List<ChatMessageEntity>
    ): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("====================================================\n")
        sb.append("CHOTTU AI - CONVERSATION EXPORT\n")
        sb.append("Session: ${session?.title ?: "Chat"}\n")
        sb.append("Export Date: ${dateFormat.format(Date())}\n")
        sb.append("Total Messages: ${messages.size}\n")
        sb.append("====================================================\n\n")

        for (msg in messages) {
            val roleName = if (msg.role == "user") "USER" else "CHOTTU AI"
            val time = dateFormat.format(Date(msg.timestamp))
            sb.append("[$time] $roleName:\n")
            sb.append(msg.content.trim())
            if (!msg.mediaType.isNullOrBlank() || !msg.fileName.isNullOrBlank()) {
                sb.append("\n[Attachment: ${msg.fileName ?: msg.mediaType}]")
            }
            sb.append("\n\n----------------------------------------------------\n\n")
        }

        return sb.toString()
    }

    fun generateJsonExport(
        session: ChatSessionEntity?,
        messages: List<ChatMessageEntity>
    ): String {
        val root = JSONObject()
        root.put("app", "Chottu AI")
        root.put("version", "1.0")
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault()).format(Date()))

        val sessionObj = JSONObject()
        sessionObj.put("id", session?.id ?: 0)
        sessionObj.put("title", session?.title ?: "Chat Conversation")
        sessionObj.put("createdAt", session?.createdAt ?: 0)
        sessionObj.put("updatedAt", session?.updatedAt ?: 0)
        root.put("session", sessionObj)

        val msgArray = JSONArray()
        for (msg in messages) {
            val item = JSONObject()
            item.put("id", msg.id)
            item.put("role", msg.role)
            item.put("content", msg.content)
            item.put("timestamp", msg.timestamp)
            item.put("isVoice", msg.isVoice)
            if (msg.mediaType != null) item.put("mediaType", msg.mediaType)
            if (msg.fileName != null) item.put("fileName", msg.fileName)
            msgArray.put(item)
        }
        root.put("messages", msgArray)

        return root.toString(2)
    }

    fun saveAndShareExportFile(
        context: Context,
        content: String,
        fileName: String,
        mimeType: String
    ) {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportsDir, fileName)
        file.writeText(content)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Chottu AI Conversation Export - $fileName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Export & Share Conversation")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
