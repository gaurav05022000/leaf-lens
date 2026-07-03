package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PointsManager
import com.example.ui.theme.EnvTextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.HeroCardBg
import com.example.ui.theme.GreenPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PointsHistoryScreen(onBackClick: () -> Unit) {
    val transactions by PointsManager.transactions.collectAsState()
    val availablePoints by PointsManager.availablePoints.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Points & Payment History", color = EnvTextPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = EnvTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Balance Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(HeroCardBg.copy(alpha = 0.1f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Available Balance", fontSize = 14.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$availablePoints Points", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = HeroCardBg)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Recent Transactions", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = EnvTextPrimary)
            Spacer(modifier = Modifier.height(16.dp))

            if (transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions yet.", color = TextSecondary)
                }
            } else {
                LazyColumn {
                    items(transactions) { trx ->
                        TransactionItem(trx)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: PointsManager.PointTransaction) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val color = if (transaction.isEarned) GreenPrimary else Color(0xFFE57373)
        val icon = if (transaction.isEarned) Icons.Default.Add else Icons.Default.Remove

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = EnvTextPrimary)
            val dateStr = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(Date(transaction.timestamp))
            Text(dateStr, fontSize = 12.sp, color = TextSecondary)
        }
        Text(
            text = "${if (transaction.isEarned) "+" else "-"}${transaction.amount}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
