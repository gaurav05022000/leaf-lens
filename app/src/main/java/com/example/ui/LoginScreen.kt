package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.example.ui.theme.*

import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, viewModel: LoginViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var insightsEnabled by remember { mutableStateOf(true) }
    var rememberMe by remember { mutableStateOf(false) }
    
    val prefs = context.getSharedPreferences("login_prefs", android.content.Context.MODE_PRIVATE)

    LaunchedEffect(Unit) {
        val savedEmail = prefs.getString("saved_email", "") ?: ""
        if (savedEmail.isNotEmpty()) {
            email = savedEmail
            rememberMe = true
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            com.example.ui.PointsManager.syncFromFirestore()
            com.example.ui.ScanHistoryManager.syncWithFirestore()
            onLoginSuccess()
        }
    }

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    // Colors that match the design
    val leafGreenDark = if (isDark) Color(0xFF679E78) else Color(0xFF38513F)
    val inputBackground = if (isDark) Color(0xFF1E2620) else Color(0xFFF4F6F4)
    val cardBackground = if (isDark) Color(0xFF1A1C1A).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.85f)
    val textColorPrimary = if (isDark) Color.White else Color.Black
    val textColorSecondary = if (isDark) Color.LightGray else Color.Gray

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = com.example.R.drawable.app_logo_1781429616941),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Add a scrim overlay so text is readable
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
        
        // Content Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .background(cardBackground, RoundedCornerShape(32.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Logo Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(leafGreenDark, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.app_logo_1781429616941),
                            contentDescription = "Logo",
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "LeafLens",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = leafGreenDark
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Title and Subtitle
                Text(
                    text = if (isSignUp) "Begin your journey" else "Welcome back",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColorPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isSignUp) "Sync your rhythms with the pulse of nature." else "Find your sanctuary within the light.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = textColorSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Inputs
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (isSignUp) {
                        Text("Full Name", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textColorSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        TextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            placeholder = { Text("Aria Green", color = textColorSecondary) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = textColorSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = textColorPrimary,
                                unfocusedTextColor = textColorPrimary,
                                focusedContainerColor = inputBackground,
                                unfocusedContainerColor = inputBackground,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Text("Email Address", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textColorSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    TextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("aria@leaflens.io", color = textColorSecondary) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = textColorSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = textColorPrimary,
                            unfocusedTextColor = textColorPrimary,
                            focusedContainerColor = inputBackground,
                            unfocusedContainerColor = inputBackground,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(if (isSignUp) "Secure Password" else "Password", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textColorSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("........", color = textColorSecondary) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = textColorSecondary) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Password Visibility",
                                    tint = textColorSecondary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = textColorPrimary,
                            unfocusedTextColor = textColorPrimary,
                            focusedContainerColor = inputBackground,
                            unfocusedContainerColor = inputBackground,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (rememberMe) {
                                prefs.edit().putString("saved_email", email).apply()
                            } else {
                                prefs.edit().remove("saved_email").apply()
                            }
                            if (isSignUp) viewModel.signUp(fullName, email, password)
                            else viewModel.login(email, password)
                        })
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Options Row (Insights or Remember Me)
                if (isSignUp) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(inputBackground, RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(if (isDark) Color(0xFF2E3A33) else Color(0xFFE2EBE2), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = leafGreenDark, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Weekly Nurture Insights", fontSize = 14.sp, color = textColorPrimary, fontWeight = FontWeight.Medium)
                            Text("Personalized botanical wellness tips", fontSize = 10.sp, color = textColorSecondary)
                        }
                        Switch(
                            checked = insightsEnabled,
                            onCheckedChange = { insightsEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = leafGreenDark
                            )
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(checkedColor = leafGreenDark)
                            )
                            Text("Remember me", fontSize = 12.sp, color = textColorPrimary)
                        }
                        Text("Forgot?", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textColorPrimary, modifier = Modifier.clickable { viewModel.resetPassword(email) })
                    }
                }

                if (uiState is LoginUiState.Error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = (uiState as LoginUiState.Error).message,
                        color = AlertTextPrimary,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Button
                Button(
                    onClick = {
                        if (rememberMe) {
                            prefs.edit().putString("saved_email", email).apply()
                        } else {
                            prefs.edit().remove("saved_email").apply()
                        }
                        if (isSignUp) viewModel.signUp(fullName, email, password)
                        else viewModel.login(email, password)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = leafGreenDark),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (uiState == LoginUiState.Loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isSignUp) "Create My Sanctuary" else "Sign In",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            if (isSignUp) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = {
                        val auth = try { com.google.firebase.auth.FirebaseAuth.getInstance() } catch(e: Exception) { null }
                        auth?.signInAnonymously()?.addOnCompleteListener {
                            com.example.ui.PointsManager.syncFromFirestore()
                            com.example.ui.ScanHistoryManager.syncWithFirestore()
                            onLoginSuccess()
                        } ?: onLoginSuccess()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Skip for Now (Continue as Guest)", color = leafGreenDark, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(24.dp))

                // Footer
                Row {
                    Text(
                        text = if (isSignUp) "Already part of the garden? " else "New to the sanctuary? ",
                        fontSize = 14.sp,
                        color = textColorSecondary
                    )
                    Text(
                        text = if (isSignUp) "Sign In" else "Join LeafLens",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = leafGreenDark,
                        modifier = Modifier.clickable { isSignUp = !isSignUp }
                    )
                }
            }
        }
    }
}

