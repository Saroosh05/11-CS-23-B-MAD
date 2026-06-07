package com.example.home_chores_automation_app.data.repository

import com.example.home_chores_automation_app.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * All calls to the Google Gemini API go through here.
 *
 * Add your key to local.properties (project root, not committed to git):
 *   GEMINI_API_KEY=your_key_here
 *
 * Get a free key at: https://aistudio.google.com/app/apikey
 * Then rebuild the app.
 */
class GeminiRepository private constructor() {

    private val apiKey = BuildConfig.GEMINI_API_KEY.trim()

    // 2.5-flash first — separate quota bucket; 2.0 models may be exhausted on free tier
    private val models = listOf(
        "gemini-2.5-flash",
        "gemini-2.0-flash",
        "gemini-2.0-flash-lite"
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    class GeminiApiException(val statusCode: Int, message: String) : Exception(message)

    companion object {
        @Volatile private var INSTANCE: GeminiRepository? = null
        fun getInstance(): GeminiRepository =
            INSTANCE ?: synchronized(this) { INSTANCE ?: GeminiRepository().also { INSTANCE = it } }

        fun isConfigured(): Boolean {
            val key = BuildConfig.GEMINI_API_KEY.trim()
            return key.isNotEmpty() &&
                !key.equals("YOUR_GEMINI_API_KEY", ignoreCase = true)
        }

        fun friendlyMessage(error: Throwable): String {
            if (error is GeminiApiException) {
                return when (error.statusCode) {
                    429 -> "AI quota limit reached for this model. Wait a minute and try again, or create a new API key from a different Google account at aistudio.google.com/app/apikey"
                    400, 401, 403 -> "Invalid Gemini API key. Check GEMINI_API_KEY in local.properties and rebuild."
                    else -> error.message ?: "AI request failed."
                }
            }
            if (error is IllegalStateException) return error.message ?: "Gemini API key is not set."
            return error.message ?: "AI request failed."
        }
    }

    private fun ensureApiKey() {
        if (!isConfigured()) {
            throw IllegalStateException(
                "Gemini API key is missing. Add GEMINI_API_KEY=your_key to local.properties and rebuild."
            )
        }
    }

    private fun apiUrl(model: String): String =
        "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

    private fun executePrompt(model: String, text: String): String {
        val body = """
            {
              "contents": [{ "parts": [{ "text": ${gson.toJson(text)} }] }],
              "generationConfig": { "maxOutputTokens": 512, "temperature": 0.7 }
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(apiUrl(model))
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val raw = response.body?.string() ?: throw GeminiApiException(0, "Empty response from Gemini")
        if (!response.isSuccessful) {
            throw GeminiApiException(response.code, "Gemini API error ${response.code}: $raw")
        }

        val root = gson.fromJson(raw, JsonObject::class.java)
        val candidate = root.getAsJsonArray("candidates")
            ?.get(0)?.asJsonObject
            ?: throw GeminiApiException(0, "No candidates in response")
        return candidate.getAsJsonObject("content")
            ?.getAsJsonArray("parts")
            ?.get(0)?.asJsonObject
            ?.get("text")?.asString
            ?: throw GeminiApiException(0, "Could not parse Gemini text")
    }

    /**
     * Sends a prompt with automatic retry on rate limits (429) and model fallback.
     */
    private suspend fun prompt(text: String): String = withContext(Dispatchers.IO) {
        ensureApiKey()

        var lastError: Exception? = null
        for (model in models) {
            repeat(3) { attempt ->
                try {
                    return@withContext executePrompt(model, text)
                } catch (e: GeminiApiException) {
                    lastError = e
                    if (e.statusCode == 429 && attempt < 2) {
                        delay(3000L * (attempt + 1))
                    } else {
                        break
                    }
                } catch (e: Exception) {
                    throw e
                }
            }
        }
        throw lastError ?: GeminiApiException(0, "Gemini request failed")
    }

    suspend fun suggestChores(groupType: String): List<String> {
        val raw = prompt(
            "List exactly 6 common household chore task titles for a '$groupType' group. " +
            "Return ONLY a plain numbered list like:\n1. Task name\n2. Task name\n" +
            "No explanations, no extra text, just the 6 tasks."
        )
        return raw.lines()
            .mapNotNull { line ->
                val trimmed = line.trim()
                val cleaned = trimmed.replace(Regex("^[0-9]+[.)\\s]+|^[-*]\\s*"), "").trim()
                cleaned.ifEmpty { null }
            }
            .filter { it.isNotBlank() }
            .take(6)
    }

    suspend fun expandTaskDescription(title: String): String {
        return prompt(
            "Write a short, practical 2-3 sentence description for a household chore task called " +
            "\"$title\". Focus on what exactly needs to be done. Be specific and concise. " +
            "Return ONLY the description text, nothing else."
        ).trim()
    }

    suspend fun suggestFairAssignment(
        taskTitle: String,
        memberStats: List<MemberStat>
    ): String {
        val statsText = memberStats.joinToString("\n") { s ->
            "- ${s.name}: ${s.completedOnTime} on-time, ${s.completedLate} late, ${s.overdue} overdue, ${s.pending} pending tasks"
        }
        return prompt(
            "We need to assign the task \"$taskTitle\" to a group member.\n\n" +
            "Member stats:\n$statsText\n\n" +
            "Based on workload and reliability, who should get this task? " +
            "Reply in ONE sentence like: \"Assign to [Name] because [brief reason].\""
        ).trim()
    }

    data class MemberStat(
        val name: String,
        val completedOnTime: Int,
        val completedLate: Int,
        val overdue: Int,
        val pending: Int
    )
}
