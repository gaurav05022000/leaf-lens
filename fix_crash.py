import re

with open('app/src/main/java/com/example/ui/HomeScreen.kt', 'r') as f:
    content = f.read()

old_block = """    androidx.compose.runtime.LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted) {
            try {
                val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        viewModel.fetchWeather(location.latitude, location.longitude)
                    } else {
                        // Fallback to New Delhi if location is null
                        viewModel.fetchWeather(28.6139, 77.2090)
                    }
                }.addOnFailureListener {
                    viewModel.fetchWeather(28.6139, 77.2090)
                }
            } catch (e: SecurityException) {
                viewModel.fetchWeather(28.6139, 77.2090)
            }
        } else {
            locationPermissions.launchMultiplePermissionRequest()
            // Fetch fallback immediately while waiting for permission
            viewModel.fetchWeather(28.6139, 77.2090)
        }
    }"""

new_block = """    androidx.compose.runtime.LaunchedEffect(locationPermissions.allPermissionsGranted) {
        try {
            if (locationPermissions.allPermissionsGranted) {
                try {
                    val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            viewModel.fetchWeather(location.latitude, location.longitude)
                        } else {
                            // Fallback to New Delhi if location is null
                            viewModel.fetchWeather(28.6139, 77.2090)
                        }
                    }.addOnFailureListener {
                        viewModel.fetchWeather(28.6139, 77.2090)
                    }
                } catch (e: SecurityException) {
                    viewModel.fetchWeather(28.6139, 77.2090)
                } catch (e: Exception) {
                    viewModel.fetchWeather(28.6139, 77.2090)
                }
            } else {
                locationPermissions.launchMultiplePermissionRequest()
                // Fetch fallback immediately while waiting for permission
                viewModel.fetchWeather(28.6139, 77.2090)
            }
        } catch (e: Exception) {
            viewModel.fetchWeather(28.6139, 77.2090)
        }
    }"""

if old_block in content:
    content = content.replace(old_block, new_block)
    with open('app/src/main/java/com/example/ui/HomeScreen.kt', 'w') as f:
        f.write(content)
    print("Patched HomeScreen")
else:
    print("Pattern not found in HomeScreen")
