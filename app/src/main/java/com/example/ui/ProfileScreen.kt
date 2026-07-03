package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun ProfileScreen(onLogout: () -> Unit, onUpgradeClick: () -> Unit = {}, onScanHistoryClick: () -> Unit = {}, onPointsHistoryClick: () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var userName by remember { mutableStateOf("Offline Mode") }
    val scanCount by PointsManager.totalScans.collectAsState()
    val availablePoints by PointsManager.availablePoints.collectAsState()
    
    LaunchedEffect(Unit) {
        try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val user = auth.currentUser
            if (user != null) {
                userName = user.displayName?.takeIf { it.isNotBlank() } ?: user.email ?: "User"
            }
        } catch (e: Exception) {
            // Offline
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = BackgroundLight) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Profile", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(vertical = 16.dp))
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = EnvCardBg)
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(64.dp).background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👤", fontSize = 32.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(userName, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = EnvTextPrimary)
                            Text("Total Scans: $scanCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HeroCardBg)
                            
                            Text("Available Points: $availablePoints", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HeroCardBg)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Button(
                                onClick = onUpgradeClick,
                                colors = ButtonDefaults.buttonColors(containerColor = HeroCardBg),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Get More Points", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
            
            item {
                Text("AI Features & Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(top = 16.dp))
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
                        SettingRow(icon = Icons.Default.Star, title = "Points & Payments History", onClick = onPointsHistoryClick)
                        HorizontalDivider(color = TextSecondary.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                        SettingRow(icon = Icons.Default.Star, title = "AI Custom Instructions")
                        HorizontalDivider(color = TextSecondary.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                        SettingRow(icon = Icons.Default.Notifications, title = "Smart Reminders")
                        HorizontalDivider(color = TextSecondary.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                        SettingRow(icon = Icons.Default.Settings, title = "Account Management")
                        HorizontalDivider(color = TextSecondary.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                        SettingRow(icon = Icons.Default.PrivacyTip, title = "Privacy & Data")
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        try {
                            if (com.revenuecat.purchases.Purchases.isConfigured) {
                                com.revenuecat.purchases.Purchases.sharedInstance.logOut()
                            }
                        } catch(e: Exception) {}
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
fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextSecondary)
    }
}
