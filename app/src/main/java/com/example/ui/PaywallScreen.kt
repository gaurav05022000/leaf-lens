package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HeroCardBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions

/**
 * PaywallScreen exclusively renders RevenueCat Paywall UI.
 * Enforces authentication: user must be logged in to purchase and bind their subscription.
 */
@Composable
fun PaywallScreen(
    onDismiss: () -> Unit,
    onSubscribedSuccess: () -> Unit = onDismiss,
    onNavigateToLogin: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val offerings by SubscriptionManager.offerings.collectAsState()
    val isPremium by SubscriptionManager.isPremium.collectAsState()
    val isLoading by SubscriptionManager.isLoading.collectAsState()
    val errorMessage by SubscriptionManager.errorMessage.collectAsState()

    val auth = try { com.google.firebase.auth.FirebaseAuth.getInstance() } catch (e: Exception) { null }
    val isUserLoggedIn = auth?.currentUser != null

    LaunchedEffect(isPremium) {
        if (isPremium) {
            onSubscribedSuccess()
        }
    }

    LaunchedEffect(Unit) {
        com.example.util.AnalyticsHelper.logScreenView("PaywallScreen")
        SubscriptionManager.fetchOfferings()
        SubscriptionManager.refreshCustomerInfo()
    }

    val currentOffering = offerings?.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF7FAF7)
    ) {
        // If user is not logged in, prompt to log in first before purchasing
        if (!isUserLoggedIn) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.06f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color(0xFFE8F5E9), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Login Required",
                            tint = HeroCardBg,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Sign In Required",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Please log in or create an account first so your premium subscription and 3-day trial are linked securely to your account.",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = {
                            if (onNavigateToLogin != null) {
                                onNavigateToLogin()
                            } else {
                                Toast.makeText(context, "Please sign in from the profile screen", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HeroCardBg),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Log In or Sign Up",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = onDismiss) {
                        Text("Not now", color = TextSecondary)
                    }
                }
            }
        } else if (currentOffering != null) {
            // Render 100% RevenueCat-driven paywall with live offerings and Play Store prices
            val options = remember(currentOffering) {
                PaywallOptions.Builder(
                    dismissRequest = onDismiss
                )
                    .setOffering(currentOffering)
                    .setListener(object : PaywallListener {
                        override fun onPurchaseCompleted(
                            customerInfo: CustomerInfo,
                            storeTransaction: StoreTransaction
                        ) {
                            com.example.util.AnalyticsHelper.logSubscriptionEvent(
                                action = "purchase_success",
                                plan = storeTransaction.productIds.firstOrNull()
                            )
                            Toast.makeText(context, "Welcome to LeafLens Premium! 🌿", Toast.LENGTH_SHORT).show()
                            SubscriptionManager.refreshCustomerInfo { active ->
                                if (active) onSubscribedSuccess()
                            }
                        }

                        override fun onRestoreCompleted(customerInfo: CustomerInfo) {
                            Toast.makeText(context, "Purchases restored successfully!", Toast.LENGTH_SHORT).show()
                            SubscriptionManager.refreshCustomerInfo { active ->
                                if (active) onSubscribedSuccess()
                            }
                        }
                    })
                    .build()
            }

            Paywall(options = options)
        } else {
            // Loading or Connection State directly reflecting RevenueCat state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.06f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = HeroCardBg,
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Loading subscription plans...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    } else {
                        Text(
                            text = "🌿",
                            fontSize = 42.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "RevenueCat Offering",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val displayMsg = when {
                            !SubscriptionManager.isBillingAvailable.value || (errorMessage?.contains("Billing", ignoreCase = true) == true) ->
                                "Google Play Billing service is unavailable in this test environment. On a real physical Android device with Google Play Store installed and logged in, your active subscription plans and Paywall will display here seamlessly."
                            else ->
                                errorMessage ?: "Connecting to RevenueCat. Please make sure products and an offering are configured in your RevenueCat Dashboard."
                        }
                        Text(
                            text = displayMsg,
                            fontSize = 14.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                SubscriptionManager.fetchOfferings()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HeroCardBg),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Retry Connection", color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(
                            onClick = {
                                SubscriptionManager.restorePurchases { isSuccess, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    if (isSuccess) onSubscribedSuccess()
                                }
                            }
                        ) {
                            Text("Restore Purchases", color = HeroCardBg)
                        }
                    }
                }
            }
        }
    }
}
