                val prompt = "You are an expert AI Botanist. Help the user with plant diagnostics, care tips, or recommendations.\n\nConversation so far:\n$historyText\nBotanist: "
                
                val contentParts = mutableListOf<Map<String, Any>>()
                contentParts.add(mapOf("type" to "text", "text" to prompt))
                
                if (bitmap != null) {
                    val base64 = bitmapToBase64(bitmap)
                    contentParts.add(mapOf(
                        "type" to "image_url",
                        "image_url" to mapOf("url" to "data:image/jpeg;base64,$base64")
                    ))
                }
                
                val message = mapOf("role" to "user", "content" to contentParts)
                
                val request = OpenRouterRequest(
                    model = "google/gemini-flash-1.5",
                    messages = listOf(message)
                )

                val modelsToTry = listOf("google/gemini-flash-1.5", "google/gemini-pro-1.5")
                var replyText: String? = null
                var lastError: Exception? = null
                
                for (model in modelsToTry) {
                    try {
                        val requestWithModel = request.copy(model = model)
                        val response = RetrofitClient.service.generateContent(
                            authorization = "Bearer ${BuildConfig.GEMINI_API_KEY}",
                            referer = "https://ai.studio",
                            title = "LeafLens",
                            request = requestWithModel
                        )
                        replyText = response.choices?.firstOrNull()?.message?.content
                        if (replyText != null) break
                    } catch (e: Exception) {
                        lastError = e
                        if (e is retrofit2.HttpException && (e.code() == 400 || e.code() == 401 || e.code() == 403 || e.code() == 429)) {
                            break
                        }
                    }
                }
