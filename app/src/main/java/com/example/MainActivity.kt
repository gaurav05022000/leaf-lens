package com.example

import com.example.ui.SubscriptionManager
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
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
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

private const val APP_CHECK_TAG = "AppCheckInit"
private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"

fun configureAppCheck(context: android.content.Context, intent: android.content.Intent?) {
    try {
        if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
            val options = com.google.firebase.FirebaseOptions.fromResource(context)
            if (options != null) {
                com.google.firebase.FirebaseApp.initializeApp(context, options)
            } else {
                com.google.firebase.FirebaseApp.initializeApp(context)
            }
        }

        if (BuildConfig.DEBUG) {
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)
            if (!debugToken.isNullOrEmpty()) {
                val persistenceKey = com.google.firebase.FirebaseApp.getInstance().persistenceKey
                val prefs = context.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    android.content.Context.MODE_PRIVATE
                )
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                android.util.Log.d(APP_CHECK_TAG, "App Check debug secret configured successfully.")
            } else {
                android.util.Log.w(APP_CHECK_TAG, "No debug token found in Intent extra.")
            }

            com.google.firebase.appcheck.FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory.getInstance()
            )
            android.util.Log.d(APP_CHECK_TAG, "DebugAppCheckProviderFactory installed.")
        } else {
            com.google.firebase.appcheck.FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory.getInstance()
            )
            android.util.Log.d(APP_CHECK_TAG, "PlayIntegrityAppCheckProviderFactory installed.")
        }
    } catch (e: Exception) {
        android.util.Log.e(APP_CHECK_TAG, "Failed to initialize Firebase App Check", e)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        
        // Clean up any corrupt webview / chromium cache from abnormal terminations
        try {
            val webViewCache = java.io.File(cacheDir, "WebView")
            if (webViewCache.exists()) {
                val httpCache = java.io.File(webViewCache, "Default/HTTP Cache")
                if (httpCache.exists()) {
                    httpCache.deleteRecursively()
                }
            }
        } catch (_: Exception) {}

        try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            if (auth.currentUser != null) {
                 android.util.Log.d("FirebaseAuth", "Already signed in. UID: ${auth.currentUser?.uid}")
            }
        } catch(e: Exception) {
            android.util.Log.e("FirebaseAuth", "Auth error", e)
        }

        try {
            com.example.util.AnalyticsHelper.initialize(this)
            com.example.ui.ScanHistoryManager.initialize(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val rcKey = BuildConfig.REVENUECAT_API_KEY
            com.example.ui.SubscriptionManager.initialize(this, rcKey)
            val currentUserId = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid } catch (e: Exception) { null }
            currentUserId?.let { com.example.ui.SubscriptionManager.syncUser(it) }
        } catch (e: Exception) {
            android.util.Log.w("RevenueCat", "SubscriptionManager initialization skipped or failed: ${e.message}")
        }

        try {
            val reqConfig = com.google.android.gms.ads.RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(com.google.android.gms.ads.AdRequest.DEVICE_ID_EMULATOR))
                .build()
            com.google.android.gms.ads.MobileAds.setRequestConfiguration(reqConfig)

            com.google.android.gms.ads.MobileAds.initialize(this) {
                runOnUiThread {
                    com.example.ui.AdManager.loadInterstitial(this)
                    com.example.ui.AdManager.loadRewarded(this)
                }
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
                },
                onLogin = {
                    prefs.edit().putBoolean("has_seen_onboarding", true).apply()
                    appOpenCount = 1
                    prefs.edit().putInt("app_open_count", 1).apply()
                    navController.navigate("login") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate("main") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            )
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

    LaunchedEffect(currentDestination?.route) {
        currentDestination?.route?.let { route ->
            com.example.util.AnalyticsHelper.logScreenView(screenName = route)
        }
    }

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
                        val auth = try { com.google.firebase.auth.FirebaseAuth.getInstance() } catch(e: Exception) { null }
                        if (auth?.currentUser == null) {
                            rootNavController.navigate("login")
                        } else {
                            navController.navigate("scan") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
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
                    onNavigateToScan = { 
                        val auth = try { com.google.firebase.auth.FirebaseAuth.getInstance() } catch(e: Exception) { null }
                        if (auth?.currentUser == null) {
                            rootNavController.navigate("login")
                        } else {
                            navController.navigate("scan")
                        }
                    },
                    onNavigateToPlant = { plantId -> 
                        navController.navigate("plantProfile/$plantId") 
                    },
                    onNavigateToProfile = { navController.navigate("profile") },
                    onNavigateToCollection = {
                        navController.navigate("dictionary") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable("scan") {
                val auth = try { com.google.firebase.auth.FirebaseAuth.getInstance() } catch(e: Exception) { null }
                if (auth?.currentUser == null) {
                    LaunchedEffect(Unit) {
                        rootNavController.navigate("login") {
                            popUpTo("scan") { inclusive = true }
                        }
                    }
                } else {
                    ScannerScreen(
                        onBack = { navController.popBackStack() },
                        onNavigateToPaywall = { navController.navigate("paywall") }
                    )
                }
            }
            composable("rescan/{plantName}") { backStackEntry ->
                val auth = try { com.google.firebase.auth.FirebaseAuth.getInstance() } catch(e: Exception) { null }
                if (auth?.currentUser == null) {
                    LaunchedEffect(Unit) {
                        rootNavController.navigate("login")
                    }
                } else {
                    val plantName = backStackEntry.arguments?.getString("plantName")
                    ScannerScreen(
                        existingPlantName = plantName,
                        onBack = { navController.popBackStack() },
                        onNavigateToPaywall = { navController.navigate("paywall") }
                    )
                }
            }
            composable("dictionary") {
                CollectionScreen(
                    onPlantClick = { plant ->
                        navController.navigate("plantProfile/${plant.id}")
                    }
                )
            }
            composable(
                route = "plantProfile/{plantId}",
                arguments = listOf(androidx.navigation.navArgument("plantId") {
                    type = androidx.navigation.NavType.IntType
                })
            ) { backStackEntry ->
                val plantId = backStackEntry.arguments?.getInt("plantId") ?: 0
                com.example.ui.PlantProfileScreen(
                    plantId = plantId,
                    onBack = { navController.popBackStack() },
                    onRescanClick = { plantName ->
                        val encoded = android.net.Uri.encode(plantName)
                        navController.navigate("rescan/$encoded")
                    }
                )
            }
            composable("profile") {
                ProfileScreen(
                    onLoginClick = {
                        rootNavController.navigate("login")
                    },
                    onLogout = {
                        SubscriptionManager.resetUser()
                        try {
                            com.google.firebase.auth.FirebaseAuth
                                .getInstance()
                                .signOut()
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
                    }
                )
            }
            composable("scanHistory") {
                com.example.ui.ScanHistoryScreen(onBackClick = { navController.popBackStack() })
            }
            composable("paywall") {
                com.example.ui.PaywallScreen(
                    onDismiss = { navController.popBackStack() },
                    onSubscribedSuccess = { navController.popBackStack() },
                    onNavigateToLogin = {
                        navController.popBackStack()
                        rootNavController.navigate("login")
                    }
                )
            }
        }
    }
}
