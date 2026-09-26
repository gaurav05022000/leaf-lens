import re

with open('app/src/main/java/com/example/ui/HomeViewModel.kt', 'r') as f:
    content = f.read()

old_block = """                        val geocoder = android.location.Geocoder(getApplication(), java.util.Locale.getDefault())
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            geocoder.getFromLocation(lat, lon, 1, object : android.location.Geocoder.GeocodeListener {
                                override fun onGeocode(addresses: MutableList<android.location.Address>) {
                                    if (addresses.isNotEmpty()) {
                                        val city = addresses[0].locality ?: addresses[0].subAdminArea ?: "Unknown Location"
                                        locationName.value = city
                                    }
                                }
                            })
                        } else {
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocation(lat, lon, 1)
                            if (!addresses.isNullOrEmpty()) {
                                val city = addresses[0].locality ?: addresses[0].subAdminArea ?: "Unknown Location"
                                locationName.value = city
                            }
                        }"""
                        
new_block = """                        val geocoder = android.location.Geocoder(getApplication(), java.util.Locale.getDefault())
                        try {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                geocoder.getFromLocation(lat, lon, 1, object : android.location.Geocoder.GeocodeListener {
                                    override fun onGeocode(addresses: MutableList<android.location.Address>) {
                                        if (addresses.isNotEmpty()) {
                                            val city = addresses[0].locality ?: addresses[0].subAdminArea ?: "Unknown Location"
                                            locationName.value = city
                                        }
                                    }
                                    override fun onError(errorMessage: String?) {
                                        super.onError(errorMessage)
                                    }
                                })
                            } else {
                                @Suppress("DEPRECATION")
                                val addresses = geocoder.getFromLocation(lat, lon, 1)
                                if (!addresses.isNullOrEmpty()) {
                                    val city = addresses[0].locality ?: addresses[0].subAdminArea ?: "Unknown Location"
                                    locationName.value = city
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }"""

if old_block in content:
    content = content.replace(old_block, new_block)
    with open('app/src/main/java/com/example/ui/HomeViewModel.kt', 'w') as f:
        f.write(content)
    print("Patched HomeViewModel")
else:
    print("Pattern not found in HomeViewModel")
