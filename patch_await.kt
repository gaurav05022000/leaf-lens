            storageRef.putBytes(data).await()
            val uri = storageRef.downloadUrl.await()
