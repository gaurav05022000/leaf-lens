package com.example.api

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenRouterRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
    val response_format: OpenRouterResponseFormat? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterMessage(
    val role: String,
    val content: List<OpenRouterContentPart>
)

@JsonClass(generateAdapter = true)
data class OpenRouterContentPart(
    val type: String,
    val text: String? = null,
    val image_url: OpenRouterImageUrl? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterImageUrl(
    val url: String
)

@JsonClass(generateAdapter = true)
data class OpenRouterResponseFormat(
    val type: String
)

@JsonClass(generateAdapter = true)
data class OpenRouterResponse(
    val choices: List<OpenRouterChoice>?
)

@JsonClass(generateAdapter = true)
data class OpenRouterChoice(
    val message: OpenRouterResponseMessage?
)

@JsonClass(generateAdapter = true)
data class OpenRouterResponseMessage(
    val content: String?
)
