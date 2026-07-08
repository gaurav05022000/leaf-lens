package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    initialPrompt: String? = null,
    onBack: () -> Unit = {},
    viewModel: AiChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    LaunchedEffect(initialPrompt) {
        if (!initialPrompt.isNullOrBlank()) {
            viewModel.sendMessage(initialPrompt)
        }
    }

    val availablePoints by PointsManager.availablePoints.collectAsState()
    
    var inputText by remember { mutableStateOf("") }
    val context = LocalContext.current
    var selectedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            val source = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                android.graphics.ImageDecoder.createSource(context.contentResolver, it)
            } else {
                null
            }
            source?.let { src ->
                val bitmap = android.graphics.ImageDecoder.decodeBitmap(src)
                selectedBitmap = bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
            } ?: run {
                @Suppress("DEPRECATION")
                selectedBitmap = android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI Botanist", fontWeight = FontWeight.Bold)
                        Text("$availablePoints Points Available", fontSize = 12.sp, color = GreenPrimary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = BackgroundLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                reverseLayout = false
            ) {
                items(messages) { message ->
                    ChatBubble(message)
                }
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = GreenPrimary,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
                
                if (availablePoints < 2) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Not enough points. AI Botanist costs 2 points per message.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = { 
                                        AdManager.showRewarded(context as android.app.Activity, 
                                            onRewardEarned = {
                                                android.widget.Toast.makeText(context, "You earned 5 points!", android.widget.Toast.LENGTH_SHORT).show()
                                            },
                                            onAdDismissed = {}
                                        )
                                    }) {
                                        Text("Watch Ad for Points")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                color = SurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (selectedBitmap != null) {
                        Box(modifier = Modifier.padding(bottom = 8.dp)) {
                            coil.compose.AsyncImage(
                                model = selectedBitmap,
                                contentDescription = "Selected image",
                                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            IconButton(
                                onClick = { selectedBitmap = null },
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).background(Color.Black.copy(alpha=0.5f), androidx.compose.foundation.shape.CircleShape)
                            ) {
                                Icon(Icons.Default.Clear, "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = GreenPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Attach image")
                        }
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Ask about plant care...") },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = BackgroundLight,
                                unfocusedContainerColor = BackgroundLight,
                                focusedBorderColor = GreenPrimary,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            singleLine = true,
                            enabled = availablePoints >= 2,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                imeAction = androidx.compose.ui.text.input.ImeAction.Send
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSend = {
                                    if ((inputText.isNotBlank() || selectedBitmap != null) && availablePoints >= 2) {
                                        viewModel.sendMessage(inputText, selectedBitmap)
                                        inputText = ""
                                        selectedBitmap = null
                                    } else if (availablePoints < 2) {
                                        android.widget.Toast.makeText(context, "You need more points!", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if ((inputText.isNotBlank() || selectedBitmap != null) && availablePoints >= 2) {
                                    viewModel.sendMessage(inputText, selectedBitmap)
                                    inputText = ""
                                    selectedBitmap = null
                                } else if (availablePoints < 2) {
                                    android.widget.Toast.makeText(context, "You need more points!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = GreenPrimary)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser
    val bgColor = if (isUser) GreenPrimary else SurfaceVariant
    val textColor = if (isUser) Color.White else TextPrimary
    val align = if (isUser) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = align
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = bgColor,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column {
                if (message.imageBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = message.imageBitmap.asImageBitmap(),
                        contentDescription = "Attached image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
                if (message.content.isNotBlank()) {
                    Text(
                        text = message.content,
                        color = textColor,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}
