package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    initialPrompt: String? = null,
    onBack: () -> Unit,
    viewModel: AiChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val chatSessions by viewModel.chatSessions.collectAsState()
    
    var inputText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    var showHistory by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(initialPrompt) {
        if (!initialPrompt.isNullOrBlank()) {
            inputText = initialPrompt
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    if (showHistory) {
        ModalBottomSheet(
            onDismissRequest = { showHistory = false },
            containerColor = BackgroundLight
        ) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Chat History", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f), color = TextPrimary)
                    Button(onClick = { 
                         viewModel.createNewSession()
                         showHistory = false
                    }, colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Chat")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (chatSessions.isEmpty()) {
                    Text("No history yet.", color = TextSecondary, modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(chatSessions) { session ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        viewModel.loadSession(session.id)
                                        showHistory = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(session.title, modifier = Modifier.weight(1f), color = TextPrimary, maxLines = 1)
                                    IconButton(onClick = { viewModel.deleteSession(session) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🌿", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("LeafLens AI", fontWeight = FontWeight.Bold) 
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showHistory = true }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "History")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary,
                    actionIconContentColor = TextPrimary
                )
            )
        },
        containerColor = BackgroundLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 80.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🌿", fontSize = 72.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Ask LeafLens AI", 
                                color = TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "I can identify plants, give care advice, and help diagnose issues.", 
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                }
                
                items(messages) { message ->
                    ChatBubble(message)
                }
                
                if (isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            Surface(
                                shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                                color = SurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = GreenPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Thinking...", color = TextSecondary, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
                
                // Add spacer at the bottom so the last message isn't hidden behind input
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            

            // Ad Banner for 0 points
            val contextForActivity = LocalContext.current
            val activity = generateSequence(contextForActivity) { if (it is android.content.ContextWrapper) it.baseContext else null }.firstOrNull { it is android.app.Activity } as? android.app.Activity
            val points by PointsManager.availablePoints.collectAsState()
            var canWatchAd by remember { mutableStateOf(activity != null && AdManager.canWatchRewardedAd(activity)) }

            if (points < 2 && canWatchAd && activity != null) {
                Surface(
                    color = GardenCardBg,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable {
                            AdManager.showRewarded(activity, onRewardEarned = {
                                canWatchAd = AdManager.canWatchRewardedAd(activity)
                            })
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = GardenTextSecondary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Out of Points?", color = GardenTextPrimary, fontWeight = FontWeight.Bold)
                            Text("Watch a short video to earn 5 points.", color = GardenTextPrimary, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Input Area
            Surface(

                color = BackgroundLight,
                shadowElevation = 16.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { newValue ->
                            if (newValue.contains('\n')) {
                                val cleanText = newValue.replace("\n", "")
                                if (cleanText.isNotBlank() && !isLoading) {
                                    viewModel.sendMessage(cleanText)
                                }
                                inputText = ""
                                focusManager.clearFocus()
                            } else {
                                inputText = newValue
                            }
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask about plants...", color = TextSecondary.copy(alpha=0.7f)) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = Color.LightGray,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = GreenPrimary
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank() && !isLoading) {
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                    focusManager.clearFocus()
                                }
                            }
                        )
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    val isInputValid = inputText.isNotBlank()
                    
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isInputValid) GreenPrimary else Color.LightGray)
                            .clickable(enabled = isInputValid && !isLoading) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                                focusManager.clearFocus()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp).offset(x = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

fun String.cleanMarkdown(): String {
    return this.replace("**", "").replace("## ", "").replace("# ", "")
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
                        contentScale = ContentScale.Crop
                    )
                }
                
                if (message.content.isNotBlank()) {
                    val imageRegex = """!\[.*?\]\((.*?)\)""".toRegex()
                    val matches = imageRegex.findAll(message.content).toList()
                    
                    if (matches.isEmpty()) {
                        Text(
                            text = message.content.cleanMarkdown(),
                            color = textColor,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    } else {
                        var lastIndex = 0
                        Column(modifier = Modifier.padding(12.dp)) {
                            for (match in matches) {
                                val textBefore = message.content.substring(lastIndex, match.range.first)
                                if (textBefore.isNotBlank()) {
                                    Text(
                                        text = textBefore.trim().cleanMarkdown(),
                                        color = textColor,
                                        fontSize = 15.sp,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                                
                                val imageUrl = match.groupValues.getOrNull(1)
                                if (!imageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = "AI generated image",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 200.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                
                                lastIndex = match.range.last + 1
                            }
                            
                            if (lastIndex < message.content.length) {
                                val textAfter = message.content.substring(lastIndex)
                                if (textAfter.isNotBlank()) {
                                    Text(
                                        text = textAfter.trim().cleanMarkdown(),
                                        color = textColor,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
