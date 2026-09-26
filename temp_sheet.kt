
    if (showHistory) {
        val chatSessions by viewModel.chatSessions.collectAsState()
        ModalBottomSheet(onDismissRequest = { showHistory = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Chat History", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                    Button(onClick = { 
                        viewModel.createNewSession()
                        showHistory = false
                    }, colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) {
                        Text("New Chat")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
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
                            colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(session.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    Text(java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(session.timestamp)), fontSize = 12.sp, color = TextSecondary)
                                }
                                IconButton(onClick = { viewModel.deleteSession(session) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                }
                            }
                        }
                    }
                    if (chatSessions.isEmpty()) {
                        item {
                            Text("No past chats found.", color = TextSecondary, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
    }
