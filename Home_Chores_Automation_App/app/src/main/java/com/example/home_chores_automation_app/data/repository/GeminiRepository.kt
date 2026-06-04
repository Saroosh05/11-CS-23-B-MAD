package com.example.home_chores_automation_app.data.repository

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * All calls to the Gemini 2.0 Flash API go through here.
 *
 * IMPORTANT — before running the app, paste your Gemini API key below.
 * Get one for free at: https://aistudio.google.com/app/apikey
 */
class GeminiRepository private constructor() {

    // ── Replace this with your actual Gemini API key ─────────────────────────
    private val apiKey = "YOUR_GEMINI_API_KEY"
    // ─────────────────────────────────────────────────────────────────────────

    private val model   = "gemini-2.0-flash"
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    companion object {
        @Volatile private var INSTANCE: GeminiRepository? = null
        fun getInstance(): GeminiRepository =
            INSTANCE ?: synchronized(this) { INSTANCE ?: GeminiRepository().also { INSTANCE = it } }
    }

    /**
     * Sends a single text prompt and returns Gemini's response string.
     * Throws an exception on network failure or non-200 HTTP status.
     */
    private suspend fun prompt(text: String): String = withContext(Dispatchers.IO) {
        val body = """
            {
              "contents": [{ "parts": [{ "text": ${gson.toJson(text)} }] }],
              "generationConfig": { "maxOutputTokens": 512, "temperature": 0.7 }
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(baseUrl)
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val raw = response.body?.string() ?: throw Exception("Empty response from Gemini")
        if (!response.isSuccessful) throw Exception("Gemini API error ${response.code}: $raw")

        // Parse: candidates[0].content.parts[0].text
        val root      = gson.fromJson(raw, JsonObject::class.java)
        val candidate = root.getAsJsonArray("candidates")
            ?.get(0)?.asJsonObject
            ?: throw Exception("No candidates in response")
        candidate.getAsJsonObject("content")
            ?.getAsJsonArray("parts")
            ?.get(0)?.asJsonObject
            ?.get("text")?.asString
            ?: throw Exception("Could not parse Gemini text")
    }

    // ── Feature 1: Chore Suggestions ─────────────────────────────────────────

    /**
     * Returns a list of 6 common chore titles for the given group type
     * (e.g. "Home", "Hostel", "Office").
     */
    suspend fun suggestChores(groupType: String): List<String> {
        val raw = prompt(
            "List exactly 6 common household chore task titles for a '$groupType' group. " +
            "Return ONLY a plain numbered list like:\n1. Task name\n2. Task name\n" +
            "No explanations, no extra text, just the 6 tasks."
        )
        return raw.lines()
            .mapNotNull { line ->
                val trimmed = line.trim()
                // Strip leading "1. " "2. " "- " "* " etc.
                val cleaned = trimmed.replace(Regex("^[0-9]+[.)\\s]+|^[-*]\\s*"), "").trim()
                cleaned.ifEmpty { null }
            }
            .filter { it.isNotBlank() }
            .take(6)
    }

    // ── Feature 2: Smart Task Description ────────────────────────────────────

    /**
     * Takes a short task title and returns a 2-3 sentence description
     * explaining what the task involves and how to do it.
     */
    suspend fun expandTaskDescription(title: String): String {
        return prompt(
            "Write a short, practical 2-3 sentence description for a household chore task called " +
            "\"$title\". Focus on what exactly needs to be done. Be specific and concise. " +
            "Return ONLY the description text, nothing else."
        ).trim()
    }

    // ── Feature 3: Fair Assignment Suggestion ────────────────────────────────

    /**
     * Given completion rate stats per member, returns the name of who Gemini
     * recommends assigning next, with a one-line reason.
     */
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
