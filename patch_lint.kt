        uri?.let {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val source = android.graphics.ImageDecoder.createSource(context.contentResolver, it)
                val bitmap = android.graphics.ImageDecoder.decodeBitmap(source)
                selectedBitmap = bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
            } else {
                @Suppress("DEPRECATION")
                selectedBitmap = android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            }
        }
