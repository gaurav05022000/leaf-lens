package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ui.theme.*

@Composable
fun ProfileScreen(
    onLoginClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    onUpgradeClick: () -> Unit = {},
    onScanHistoryClick: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val loginViewModel: LoginViewModel = viewModel()
    var userName by remember { mutableStateOf("Guest User") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }

    val scanHistory by ScanHistoryManager.history.collectAsState()
    val scanCount = scanHistory.size

    val isPremium by SubscriptionManager.isPremium.collectAsState()
    val isTrialActive by SubscriptionManager.isTrialActive.collectAsState()
    val trialDaysRemaining by SubscriptionManager.trialDaysRemaining.collectAsState()
    val dailyFreeScansRemaining by SubscriptionManager.dailyFreeScansRemaining.collectAsState()
    val activePlanName by SubscriptionManager.activePlanName.collectAsState()
    val expirationDate by SubscriptionManager.expirationDate.collectAsState()

    LaunchedEffect(Unit) {
        SubscriptionManager.updateTrialState()
        try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val user = auth.currentUser
            if (user != null) {
                isLoggedIn = true
                userName = user.displayName?.takeIf { it.isNotBlank() } ?: user.email ?: "User"
                photoUri = user.photoUrl
            } else {
                isLoggedIn = false
                userName = "Guest User"
                photoUri = null
            }
        } catch (e: Exception) {
            isLoggedIn = false
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = BackgroundLight) {
        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("Edit Profile") },
                text = {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        userName = editName
                        showEditDialog = false
                        try {
                            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                            user?.updateProfile(
                                com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                    .setDisplayName(editName)
                                    .build()
                            )
                        } catch (e: Exception) {}
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showDeleteAccountDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAccountDialog = false },
                title = { Text("Delete Account", color = MaterialTheme.colorScheme.error) },
                text = {
                    Text("Are you sure you want to permanently delete your account? All your plants, scan history, diagnosis reports, and cloud data will be erased immediately. This cannot be undone.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteAccountDialog = false
                            loginViewModel.deleteAccount(context) { success, error ->
                                if (success) {
                                    Toast.makeText(context, "Account deleted successfully", Toast.LENGTH_LONG).show()
                                    onLogout()
                                } else {
                                    Toast.makeText(context, error ?: "Could not delete account. Please re-login and try again.", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete Permanently", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAccountDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 40.dp)
        ) {
            // Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(HeroCardBg.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (photoUri != null) {
                                AsyncImage(
                                    model = photoUri,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(userName.take(1).uppercase(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = HeroCardBg)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(userName, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = EnvTextPrimary, modifier = Modifier.weight(1f, fill = false))
                                if (isLoggedIn) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(onClick = { 
                                        editName = userName
                                        showEditDialog = true 
                                    }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = EnvTextPrimary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            if (!isLoggedIn) {
                                Text("Sign in to save plants & sync across devices", fontSize = 12.sp, color = TextSecondary)
                            } else {
                                Text("Total Scans: $scanCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HeroCardBg)
                            }
                            
                            if (isPremium) {
                                Text("LeafLens Premium: Active ✓", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HeroCardBg)
                                activePlanName?.let { plan ->
                                    Text("Plan: $plan", fontSize = 12.sp, color = TextSecondary)
                                }
                            } else if (!isLoggedIn) {
                                Text("Guest Account 🌿", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HeroCardBg)
                                Text("Sign in to activate your 3-Day Free Trial.", fontSize = 12.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = onUpgradeClick,
                                        colors = ButtonDefaults.buttonColors(containerColor = HeroCardBg),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f).height(38.dp)
                                    ) {
                                        Text("Subscribe Now", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    OutlinedButton(
                                        onClick = onLoginClick,
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, HeroCardBg),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f).height(38.dp)
                                    ) {
                                        Text("Log In", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HeroCardBg)
                                    }
                                }
                            } else if (isTrialActive) {
                                Text("3-Day Free Trial: Active ✨", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                Text("$dailyFreeScansRemaining/${SubscriptionManager.MAX_FREE_DAILY_SCANS} scans today • $trialDaysRemaining day${if (trialDaysRemaining > 1) "s" else ""} left", fontSize = 12.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = onUpgradeClick,
                                        colors = ButtonDefaults.buttonColors(containerColor = HeroCardBg),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f).height(38.dp)
                                    ) {
                                        Text("Purchase Plan", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            } else {
                                Text("Free Trial Expired 🔒", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AlertTextPrimary)
                                Text("Purchase a plan to continue scanning plants.", fontSize = 12.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = onUpgradeClick,
                                        colors = ButtonDefaults.buttonColors(containerColor = HeroCardBg),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f).height(38.dp)
                                    ) {
                                        Text("Subscribe Now", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Subscription Card
            item {
                Text("Subscription & Billing", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(top = 16.dp))
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🌿 LeafLens Premium", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                    if (isPremium) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Active ✓", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HeroCardBg)
                                    }
                                }
                                if (isPremium) {
                                    Text(
                                        "Plan: ${activePlanName ?: "Premium"} • Status: Active",
                                        fontSize = 13.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    expirationDate?.let { exp ->
                                        Text(
                                            "Renews/Expires: $exp",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                } else if (!isLoggedIn) {
                                    Text(
                                        "Guest Mode • 3-Day Free Trial Available",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF2E7D32),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    Text(
                                        "Sign in to start your 3-day free trial or choose a plan.",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                } else if (isTrialActive) {
                                    Text(
                                        "3-Day Free Trial ($trialDaysRemaining day${if (trialDaysRemaining > 1) "s" else ""} left)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF2E7D32),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    Text(
                                        "Purchase a subscription plan to retain unlimited AI scans.",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                } else {
                                    Text(
                                        "Trial expired. Choose a subscription plan to unlock unlimited scans.",
                                        fontSize = 13.sp,
                                        color = AlertTextPrimary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onUpgradeClick,
                                modifier = Modifier.weight(1f).height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = HeroCardBg),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    if (isPremium) "Change Plan" else "View Plans",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    SubscriptionManager.restorePurchases { active, message ->
                                        val msg = if (active) "Purchases restored! LeafLens Premium is active." else message
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Restore", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            }
                        }
                    }
                }
            }

            if (isLoggedIn) {
                item {
                    Text("Features & Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(top = 16.dp))
                }
                
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
                    ) {
                        Column {
                            SettingRow(icon = Icons.Default.Star, title = "Scan History", onClick = onScanHistoryClick)
                            HorizontalDivider(color = TextSecondary.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                            SettingRow(icon = Icons.Default.Notifications, title = "Smart Reminders")
                            HorizontalDivider(color = TextSecondary.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                            SettingRow(icon = Icons.Default.PrivacyTip, title = "Privacy Policy")
                            HorizontalDivider(color = TextSecondary.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                            SettingRow(
                                icon = Icons.Default.DeleteForever,
                                title = "Delete Account & Data",
                                titleColor = MaterialTheme.colorScheme.error,
                                onClick = { showDeleteAccountDialog = true }
                            )
                        }
                    }
                }
            } else {
                item {
                    Text("Features & Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(top = 16.dp))
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
                    ) {
                        Column {
                            SettingRow(icon = Icons.Default.Star, title = "Scan History", onClick = onScanHistoryClick)
                            HorizontalDivider(color = TextSecondary.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                            SettingRow(icon = Icons.Default.Notifications, title = "Smart Reminders")
                            HorizontalDivider(color = TextSecondary.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                            SettingRow(icon = Icons.Default.PrivacyTip, title = "Privacy Policy")
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
                if (isLoggedIn) {
                    Button(
                        onClick = {
                            SubscriptionManager.resetUser()
                            try {
                                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                            } catch (e: Exception) {}
                            onLogout()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AlertCardBg, contentColor = AlertTextPrimary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Log Out", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = onLoginClick,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HeroCardBg),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Log In / Sign Up", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

@Composable
fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    titleColor: Color = TextPrimary,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = titleColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = titleColor, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextSecondary)
    }
}
