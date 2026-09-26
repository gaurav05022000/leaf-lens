    private suspend fun uploadImageToStorage(bitmap: Bitmap): String {
        return try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            val data = outputStream.toByteArray()
            
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val uid = auth.currentUser?.uid ?: "anonymous"
            val filename = "scans_${System.currentTimeMillis()}.jpg"
            val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference.child("users/$uid/$filename")
            
            kotlinx.coroutines.tasks.await(storageRef.putBytes(data))
            val uri = kotlinx.coroutines.tasks.await(storageRef.downloadUrl)
            uri.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
