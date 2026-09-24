package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class GeminiService {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val MODEL_NAME = "gemini-3.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"
        
        const val CHOTTU_SYSTEM_INSTRUCTION = 
            "You are Chottu AI (छोटू AI), a brilliant, witty, and friendly voice & multimodal AI companion, inspired by Gemini and ChatGPT. " +
            "You speak naturally, warmly, and conversationally like a true voice assistant. " +
            "When responding for voice, keep explanations crisp, articulate, and punchy. " +
            "When analyzing photos, diagrams, or documents, provide sharp, insightful observations. " +
            "Always be helpful, encouraging, and sharp. Never give dull robotic replies."
    }

    /**
     * Resolves the effective API key (configured key or BuildConfig).
     */
    fun getResolvedApiKey(customKey: String?): String {
        if (!customKey.isNullOrBlank()) return customKey.trim()
        val buildKey = BuildConfig.GEMINI_API_KEY
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey.trim()
        }
        return ""
    }

    /**
     * Checks if a valid API key is available.
     */
    fun hasValidApiKey(customKey: String?): Boolean {
        return getResolvedApiKey(customKey).isNotBlank()
    }

    suspend fun generateResponse(
        prompt: String,
        customApiKey: String? = null,
        conversationHistory: List<Pair<String, String>> = emptyList(), // role to text
        attachmentData: AttachmentData? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getResolvedApiKey(customApiKey)
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException(
                    "Gemini API key is not configured. Please add GEMINI_API_KEY in the Secrets panel in AI Studio or enter your API key in Chottu AI Settings."
                )
            )
        }

        try {
            val rootJson = JSONObject()

            // System Instruction
            val systemInstructionJson = JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", CHOTTU_SYSTEM_INSTRUCTION)
                    })
                })
            }
            rootJson.put("systemInstruction", systemInstructionJson)

            // Contents array (Conversation history + current prompt)
            val contentsArray = JSONArray()

            // Add past turns for context (limit to last 6 turns for optimal latency)
            val recentHistory = if (conversationHistory.size > 6) {
                conversationHistory.takeLast(6)
            } else {
                conversationHistory
            }

            for ((role, text) in recentHistory) {
                if (text.isNotBlank()) {
                    val turnRole = if (role == "user") "user" else "model"
                    val turnObj = JSONObject().apply {
                        put("role", turnRole)
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", text) })
                        })
                    }
                    contentsArray.put(turnObj)
                }
            }

            // Current user turn
            val currentTurnObj = JSONObject().apply {
                put("role", "user")
                val partsArray = JSONArray()

                // If there is an attachment (image or document)
                if (attachmentData != null) {
                    val inlineDataObj = JSONObject().apply {
                        put("mimeType", attachmentData.mimeType)
                        put("data", attachmentData.base64Data)
                    }
                    partsArray.put(JSONObject().apply {
                        put("inlineData", inlineDataObj)
                    })
                }

                // Add prompt text
                val userPrompt = if (prompt.isBlank() && attachmentData != null) {
                    "Please analyze this file in detail and explain what you see or what it contains."
                } else {
                    prompt
                }
                partsArray.put(JSONObject().apply {
                    put("text", userPrompt)
                })

                put("parts", partsArray)
            }
            contentsArray.put(currentTurnObj)
            rootJson.put("contents", contentsArray)

            // Generation Config
            val generationConfig = JSONObject().apply {
                put("temperature", 0.7)
                put("topP", 0.95)
                put("topK", 40)
            }
            rootJson.put("generationConfig", generationConfig)

            val url = "$BASE_URL?key=$apiKey"
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = rootJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = try {
                        val errJson = JSONObject(responseBodyString)
                        val errorObj = errJson.optJSONObject("error")
                        errorObj?.optString("message") ?: "HTTP error ${response.code}"
                    } catch (e: Exception) {
                        "HTTP error ${response.code}: ${response.message}"
                    }
                    return@withContext Result.failure(Exception(errorMsg))
                }

                val jsonResponse = JSONObject(responseBodyString)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val replyTextBuilder = StringBuilder()
                        for (i in 0 until parts.length()) {
                            val partObj = parts.getJSONObject(i)
                            if (partObj.has("text")) {
                                replyTextBuilder.append(partObj.getString("text"))
                            }
                        }
                        return@withContext Result.success(replyTextBuilder.toString().trim())
                    }
                }

                Result.failure(Exception("No response generated by Chottu AI"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Helper to load URI into AttachmentData (Base64 + MIME).
     */
    suspend fun loadAttachment(context: Context, uri: Uri, mimeTypeInput: String?): AttachmentData? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val detectedMime = mimeTypeInput ?: contentResolver.getType(uri) ?: "application/octet-stream"

            if (detectedMime.startsWith("image/")) {
                // Resize image to max 1280px for fast tokenization & responsiveness
                var inputStream: InputStream? = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    val scaledBitmap = scaleBitmap(bitmap, 1280)
                    val outputStream = ByteArrayOutputStream()
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                    val bytes = outputStream.toByteArray()
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    return@withContext AttachmentData(
                        mimeType = "image/jpeg",
                        base64Data = base64,
                        isImage = true
                    )
                }
            } else {
                // Non-image document (text, json, csv, pdf, etc.)
                val inputStream = contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes != null) {
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    return@withContext AttachmentData(
                        mimeType = detectedMime,
                        base64Data = base64,
                        isImage = false
                    )
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (ratio > 1f) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}

data class AttachmentData(
    val mimeType: String,
    val base64Data: String,
    val isImage: Boolean
)
