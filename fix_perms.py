import re

with open('app/src/main/java/com/example/ui/HomeScreen.kt', 'r') as f:
    content = f.read()

old_perms = """    val locationPermissions = com.google.accompanist.permissions.rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val notificationPermission = com.google.accompanist.permissions.rememberPermissionState(
        "android.permission.POST_NOTIFICATIONS"
    )

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (!notificationPermission.status.isGranted) {
                notificationPermission.launchPermissionRequest()
            }
        }
    }"""

new_perms = """    val locationPermissions = com.google.accompanist.permissions.rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        val notificationPermission = com.google.accompanist.permissions.rememberPermissionState(
            "android.permission.POST_NOTIFICATIONS"
        )
        androidx.compose.runtime.LaunchedEffect(Unit) {
            if (!notificationPermission.status.isGranted) {
                notificationPermission.launchPermissionRequest()
            }
        }
    }"""

if old_perms in content:
    content = content.replace(old_perms, new_perms)
    with open('app/src/main/java/com/example/ui/HomeScreen.kt', 'w') as f:
        f.write(content)
    print("Fixed HomeScreen")
else:
    print("Pattern not found in HomeScreen")
