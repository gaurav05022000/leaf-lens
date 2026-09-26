package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap

@Composable
fun PlantImage(
    model: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    if (model != null && model.startsWith("data:image/")) {
        val base64 = model.substringAfter("base64,")
        val bytes = try { android.util.Base64.decode(base64, android.util.Base64.DEFAULT) } catch(e: Exception) { null }
        if (bytes != null) {
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = contentDescription,
                    modifier = modifier,
                    contentScale = contentScale
                )
                return
            }
        }
    }
    
    coil.compose.AsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}
