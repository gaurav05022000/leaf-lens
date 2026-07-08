                val contentParts = mutableListOf<OpenRouterContentPart>()
                contentParts.add(OpenRouterContentPart(type = "text", text = prompt))
                
                if (bitmap != null) {
                    val base64 = bitmapToBase64(bitmap)
                    contentParts.add(OpenRouterContentPart(
                        type = "image_url",
                        image_url = OpenRouterImageUrl(url = "data:image/jpeg;base64,$base64")
                    ))
                }
                
                val message = OpenRouterMessage(role = "user", content = contentParts)
                
                val request = OpenRouterRequest(
                    model = "google/gemini-flash-1.5",
                    messages = listOf(message)
                )
