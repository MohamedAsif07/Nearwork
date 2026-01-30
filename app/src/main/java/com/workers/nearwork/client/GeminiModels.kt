package com.workers.nearwork.client

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

// Data Classes for Gemini 2.5 API
data class GeminiRequest(val contents: List<Content>)
data class Content(val parts: List<Part>)
data class Part(val text: String? = null, val inline_data: InlineData? = null)
data class InlineData(val mime_type: String, val data: String)

data class GeminiResponse(val candidates: List<Candidate>?)
data class Candidate(val content: ResponseContent?)
data class ResponseContent(val parts: List<ResponsePart>?)
data class ResponsePart(val text: String?)

interface GeminiApiService {
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateDescription(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}