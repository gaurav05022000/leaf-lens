package com.example

import androidx.compose.runtime.LaunchedEffect
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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
            if (auth.currentUser == null) {
                auth.signInAnonymously()
            }
        } catch(e: Exception) {}

        try {
            com.example.ui.PointsManager.initialize(this)
            com.example.ui.ScanHistoryManager.initialize(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (BuildConfig.REVENUECAT_API_KEY.isNotEmpty() && !BuildConfig.REVENUECAT_API_KEY.contains("fake")) {
                com.revenuecat.purchases.Purchases.logLevel = com.revenuecat.purchases.LogLevel.DEBUG
                Purchases.configure(PurchasesConfiguration.Builder(this, BuildConfig.REVENUECAT_API_KEY).build())
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
        !isUserLoggedIn && appOpenCount > 2 -> "login"
        else -> "main"
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("onboarding") {
            com.example.ui.OnboardingScreen(
                onGetStarted = {
                    prefs.edit().putBoolean("has_seen_onboarding", true).apply()
                    appOpenCount = 1
                    prefs.edit().putInt("app_open_count", 1).apply()
                    navController.navigate("main") {
                        popUpTo("onboarding") { inclusive = true }
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
                    }
                )
            }
            composable("scan") {
                ScannerScreen(
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
            composable("aichat") {
                com.example.ui.AiChatScreen()
            }
            composable("plantProfile/{plantName}") { backStackEntry ->
                val plantName = backStackEntry.arguments?.getString("plantName") ?: ""
                com.example.ui.PlantProfileScreen(
                    plantName = plantName,
                    onBack = { navController.popBackStack() }
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
            composable("paywall") {
                com.revenuecat.purchases.ui.revenuecatui.Paywall(
                    options = com.revenuecat.purchases.ui.revenuecatui.PaywallOptions.Builder(
                        dismissRequest = { navController.popBackStack() }
                    )
                    .setListener(
                        object : com.revenuecat.purchases.ui.revenuecatui.PaywallListener {
                            override fun onPurchaseCompleted(customerInfo: com.revenuecat.purchases.CustomerInfo, storeTransaction: com.revenuecat.purchases.models.StoreTransaction) {
                                // Add 100 points on successful purchase
                                com.example.ui.PointsManager.addPoints(100)
                                navController.popBackStack()
                            }
                        }
                    )
                    .build()
                )
            }
        }
    }
}
