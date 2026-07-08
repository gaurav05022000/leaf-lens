package com.example

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.ui.CollectionScreen
import com.example.ui.HomeScreen
import com.example.ui.LoginScreen
import com.example.ui.ProfileScreen
import com.example.ui.ScannerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NavBarBg
import com.example.ui.theme.SurfaceVariant
import com.example.ui.theme.TextPrimary

import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

// Triggering reinstall 4
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setApiKey(BuildConfig.FIREBASE_API_KEY)
                    .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                    .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                    .build()
                com.google.firebase.FirebaseApp.initializeApp(this, options)
            }
        } catch (e: Exception) {
            // Ignore init errors
        }

        try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            if (auth.currentUser != null) {
                 android.util.Log.d("FirebaseAuth", "Already signed in. UID: ${auth.currentUser?.uid}")
            }
        } catch(e: Exception) {
            android.util.Log.e("FirebaseAuth", "Auth error", e)
        }

        try {
            com.example.ui.PointsManager.initialize(this)
            com.example.ui.ScanHistoryManager.initialize(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val rcKey = BuildConfig.REVENUECAT_API_KEY
            if (rcKey.isNotEmpty() && rcKey.startsWith("goog_")) {
                com.revenuecat.purchases.Purchases.logLevel = com.revenuecat.purchases.LogLevel.DEBUG
                com.revenuecat.purchases.Purchases.configure(com.revenuecat.purchases.PurchasesConfiguration.Builder(this, rcKey).build())
                android.util.Log.d("RevenueCat", "RevenueCat configured with key starting with: ${rcKey.take(8)}")
            } else {
                android.util.Log.e("RevenueCat", "RevenueCat API key is empty or invalid. Key must start with 'goog_'. Paywall will not work.")
            }
        } catch (e: Exception) {
            android.util.Log.e("RevenueCat", "RevenueCat initialization failed", e)
        }

        try {
            com.google.android.gms.ads.MobileAds.initialize(this) {
                com.example.ui.AdManager.loadInterstitial(this)
                com.example.ui.AdManager.loadRewarded(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
    
    val auth = try { com.google.firebase.auth.FirebaseAuth.getInstance() } catch(e: Exception) { null }
    val isUserLoggedIn = auth?.currentUser != null
    
    var appOpenCount by remember { mutableStateOf(prefs.getInt("app_open_count", 0)) }
    var hasSeenOnboarding by remember { mutableStateOf(prefs.getBoolean("has_seen_onboarding", false)) }
    
    LaunchedEffect(Unit) {
        if (hasSeenOnboarding) {
            appOpenCount++
            prefs.edit().putInt("app_open_count", appOpenCount).apply()
        }
    }
    
    val startDestination = when {
        !hasSeenOnboarding -> "onboarding"
        !isUserLoggedIn -> "login"
        else -> "main"
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("onboarding") {
            com.example.ui.OnboardingScreen(
                onGetStarted = {
                    prefs.edit().putBoolean("has_seen_onboarding", true).apply()
                    appOpenCount = 1
                    prefs.edit().putInt("app_open_count", 1).apply()
                    if (isUserLoggedIn) {
                        navController.navigate("main") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    } else {
                        navController.navigate("login") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                }
            )
        }
        composable("login") {
            LoginScreen(onLoginSuccess = {
                navController.navigate("main") {
                    popUpTo("login") { inclusive = true }
                }
            })
        }
        composable("main") {
            MainScreen(rootNavController = navController)
        }
    }
}

@Composable
fun MainScreen(rootNavController: NavController) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = NavBarBg) {
                val navColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TextPrimary,
                    selectedTextColor = TextPrimary,
                    unselectedIconColor = TextPrimary.copy(alpha = 0.4f),
                    unselectedTextColor = TextPrimary.copy(alpha = 0.4f),
                    indicatorColor = SurfaceVariant
                )
                
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = currentDestination?.hierarchy?.any { it.route == "home" } == true,
                    onClick = {
                        navController.navigate("home") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = navColors
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.AddCircle, contentDescription = "Scan") },
                    label = { Text("Scan") },
                    selected = currentDestination?.hierarchy?.any { it.route == "scan" } == true,
                    onClick = {
                        navController.navigate("scan") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = navColors
                )
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Collection") },
                    label = { Text("Collection") },
                    selected = currentDestination?.hierarchy?.any { it.route == "dictionary" } == true,
                    onClick = {
                        navController.navigate("dictionary") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = navColors
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.AccountCircle, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = currentDestination?.hierarchy?.any { it.route == "profile" } == true,
                    onClick = {
                        navController.navigate("profile") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = navColors
                )
                NavigationBarItem(
                    icon = { Text("✨", fontSize = 20.sp) },
                    label = { Text("AI") },
                    selected = currentDestination?.hierarchy?.any { it.route == "aichat" } == true,
                    onClick = {
                        navController.navigate("aichat") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = navColors
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            composable("home") {
                HomeScreen(
                    onNavigateToScan = { navController.navigate("scan") },
                    onNavigateToPlant = { plantName -> 
                        val encoded = android.net.Uri.encode(plantName)
                        navController.navigate("plantProfile/$encoded") 
                    },
                    onNavigateToProfile = { navController.navigate("profile") }
                )
            }
            composable("scan") {
                ScannerScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("rescan/{plantName}") { backStackEntry ->
                val plantName = backStackEntry.arguments?.getString("plantName")
                ScannerScreen(
                    existingPlantName = plantName,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("dictionary") {
                CollectionScreen(
                    onPlantClick = { plant ->
                        val encoded = android.net.Uri.encode(plant.name)
                        navController.navigate("plantProfile/$encoded")
                    }
                )
            }
            composable(
                route = "aichat?prompt={prompt}",
                arguments = listOf(androidx.navigation.navArgument("prompt") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                })
            ) { backStackEntry ->
                val prompt = backStackEntry.arguments?.getString("prompt")
                com.example.ui.AiChatScreen(
                    initialPrompt = prompt,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("plantProfile/{plantName}") { backStackEntry ->
                val plantName = backStackEntry.arguments?.getString("plantName") ?: ""
                com.example.ui.PlantProfileScreen(
                    plantName = plantName,
                    onBack = { navController.popBackStack() },
                    onRescanClick = {
                        val encoded = android.net.Uri.encode(plantName)
                        navController.navigate("rescan/$encoded")
                    },
                    onChatClick = { prompt ->
                        val encoded = android.net.Uri.encode(prompt)
                        navController.navigate("aichat?prompt=$encoded")
                    }
                )
            }
            composable("profile") {
                ProfileScreen(
                    onLogout = {
                        try {
                            com.example.ui.PointsManager.clearLocalData()
                            com.example.ui.ScanHistoryManager.clearLocalData()
                            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                        } catch(e: Exception) {}
                        
                        rootNavController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onUpgradeClick = { 
                        navController.navigate("paywall")
                    },
                    onScanHistoryClick = {
                        navController.navigate("scanHistory")
                    },
                    onPointsHistoryClick = {
                        navController.navigate("pointsHistory")
                    }
                )
            }
            composable("scanHistory") {
                com.example.ui.ScanHistoryScreen(onBackClick = { navController.popBackStack() })
            }
            composable("pointsHistory") {
                com.example.ui.PointsHistoryScreen(onBackClick = { navController.popBackStack() })
            }
            dialog("paywall") {
                if (com.revenuecat.purchases.Purchases.isConfigured) {
                    com.revenuecat.purchases.ui.revenuecatui.Paywall(
                        options = com.revenuecat.purchases.ui.revenuecatui.PaywallOptions.Builder(
                            dismissRequest = { navController.popBackStack() }
                        )
                        .setListener(
                            object : com.revenuecat.purchases.ui.revenuecatui.PaywallListener {
                                override fun onPurchaseCompleted(customerInfo: com.revenuecat.purchases.CustomerInfo, storeTransaction: com.revenuecat.purchases.models.StoreTransaction) {
                                    android.util.Log.d("RevenueCat", "Purchase completed")
                                    com.example.ui.PointsManager.addPoints(100)
                                    navController.popBackStack()
                                }
                            }
                        )
                        .build()
                    )
                } else {
                    androidx.compose.material3.Card(
                        modifier = androidx.compose.ui.Modifier.padding(24.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface)
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = androidx.compose.ui.Modifier.padding(24.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            androidx.compose.material3.Text("RevenueCat is not configured.")
                        }
                    }
                }
            }
        }
    }
}
