import re

with open('app/src/main/java/com/example/ui/HomeScreen.kt', 'r') as f:
    content = f.read()

old_block = """                    val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            viewModel.fetchWeather(location.latitude, location.longitude)
                        } else {
                            // Fallback to New Delhi if location is null
                            viewModel.fetchWeather(28.6139, 77.2090)
                        }
                    }.addOnFailureListener {
                        viewModel.fetchWeather(28.6139, 77.2090)
                    }"""

new_block = """                    val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
                    val location = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER) 
                        ?: locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                    if (location != null) {
                        viewModel.fetchWeather(location.latitude, location.longitude)
                    } else {
                        viewModel.fetchWeather(28.6139, 77.2090)
                    }"""

if old_block in content:
    content = content.replace(old_block, new_block)
    with open('app/src/main/java/com/example/ui/HomeScreen.kt', 'w') as f:
        f.write(content)
    print("Patched HomeScreen.kt")
else:
    print("Could not find old block in HomeScreen.kt")

