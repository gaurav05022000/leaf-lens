package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    existingPlantName: String? = null,
    onBack: () -> Unit,
    viewModel: ScannerViewModel = viewModel()
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val scanResult by viewModel.scanResult.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val error by viewModel.error.collectAsState()

    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showLimitDialog by remember { mutableStateOf(false) }
    var pendingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    val capturePhoto = {
        val executor = ContextCompat.getMainExecutor(context)
        imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val matrix = Matrix().apply {
                        postRotate(image.imageInfo.rotationDegrees.toFloat())
                    }
                    val originalBitmap = image.toBitmap()
                    val rotatedBitmap = Bitmap.createBitmap(
                        originalBitmap, 0, 0,
                        originalBitmap.width, originalBitmap.height,
                        matrix, true
                    )
                    pendingBitmap = rotatedBitmap
                    showConfirmationDialog = true
                } catch (e: Exception) {
                    Log.e("Scanner", "Error analyzing image", e)
                } finally {
                    image.close()
                }
            }
            override fun onError(exception: ImageCaptureException) {
                Log.e("Scanner", "Capture failed", exception)
            }
        })
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val source = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                android.graphics.ImageDecoder.createSource(context.contentResolver, it)
            } else {
                null
            }
            source?.let { src ->
                val bitmap = android.graphics.ImageDecoder.decodeBitmap(src)
                // convert hardware bitmap to software for processing if needed, but Gemini API needs base64
                val softwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                pendingBitmap = softwareBitmap
                showConfirmationDialog = true
            } ?: run {
                @Suppress("DEPRECATION")
                val bmp = android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                pendingBitmap = bmp
                showConfirmationDialog = true
            }
        }
    }

    Scaffold { padding ->
        if (showLimitDialog) {
            // Nothing to do here since point limits are changed
        }

        if (showConfirmationDialog && pendingBitmap != null) {
            AlertDialog(
                onDismissRequest = { 
                    showConfirmationDialog = false
                    pendingBitmap = null
                },
                title = { Text("Use this photo?") },
                text = { 
                    Column {
                        Text("Do you want to scan this plant photo?", modifier = Modifier.padding(bottom = 16.dp))
                        Image(
                            bitmap = pendingBitmap!!.asImageBitmap(),
                            contentDescription = "Preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val availablePoints = PointsManager.availablePoints.value
                            
                            if (availablePoints < 10) {
                                showConfirmationDialog = false
                                android.widget.Toast.makeText(context, "Low points! You need at least 10 points for your next scan.", android.widget.Toast.LENGTH_LONG).show()
                            } else {
                                selectedBitmap = pendingBitmap
                                viewModel.scanPlant(pendingBitmap!!)
                                showConfirmationDialog = false
                                pendingBitmap = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HeroCardBg)
                    ) {
                        Text("Scan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showConfirmationDialog = false
                        pendingBitmap = null
                    }) {
                        Text("Retake")
                    }
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Background Layer: Camera or Selected Image
            if (selectedBitmap != null) {
                Image(
                    bitmap = selectedBitmap!!.asImageBitmap(),
                    contentDescription = "Selected Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (hasCameraPermission) {
                CameraLiveView(imageCapture = imageCapture, modifier = Modifier.fillMaxSize())
            }

            // Dim overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )

            // Scanning Frame Overlay
            AnimatedScannerOverlay(isScanning = isScanning)

            // Status Pill
            if (isScanning || scanResult == null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 160.dp)
                        .background(HeroCardBg.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.White, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isScanning) "IDENTIFYING SPECIMEN..." else "POINT AT A PLANT",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Results Card at Bottom
            if (scanResult != null) {
                val res = scanResult!!
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.6f),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                    colors = CardDefaults.cardColors(containerColor = BackgroundLight)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 12.dp)
                                .size(40.dp, 4.dp)
                                .background(Color.LightGray, CircleShape)
                                .align(Alignment.CenterHorizontally)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            selectedBitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Thumb",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(res.name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                Text(res.species, color = Color.Gray, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                // Health Status line
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(HeroButtonBg, RoundedCornerShape(12.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, "Status", tint = HeroCardBg, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = res.healthStatus.uppercase(),
                                                color = HeroCardBg,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text("${res.healthScore}% Health", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        
                        // Scrollable content
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (res.disease != null && res.disease != "null" && res.disease.isNotBlank()) {
                                Text("Diagnosis", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Issue: ${res.disease}", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                                        if (res.severityLevel != null) {
                                            Text("Severity: ${res.severityLevel}", color = Color(0xFFC62828))
                                        }
                                        val symptomsList = res.symptoms.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                        if (symptomsList.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Symptoms:", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                            symptomsList.forEach {
                                                Text("• $it", color = Color(0xFFC62828), fontSize = 14.sp)
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            
                            val careTipsList = res.careTips.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            if (careTipsList.isNotEmpty()) {
                                Text("Actionable Care Tips", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        careTipsList.forEach { tip ->
                                            Text("• $tip", color = Color(0xFF2E7D32), fontSize = 14.sp)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            
                            val treatmentList = res.treatmentSteps.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            if (treatmentList.isNotEmpty()) {
                                Text("Recommended Treatment", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                treatmentList.forEachIndexed { index, step ->
                                    Row(modifier = Modifier.padding(bottom = 8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(HeroCardBg, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("${index + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(step, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            
                            Text("About", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(res.description, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                        
                        Button(
                            onClick = { 
                                viewModel.savePlant(res, existingPlantName)
                                AdManager.showInterstitial(context as android.app.Activity) {
                                    onBack() 
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HeroCardBg)
                        ) {
                            Text("Save to Collection")
                        }
                        TextButton(
                            onClick = { 
                                viewModel.reportIssue(res)
                                android.widget.Toast.makeText(context, "Report submitted. Thank you!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Report Incorrect Result", color = Color.Gray)
                        }
                        TextButton(
                            onClick = { 
                                viewModel.clearResult()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Retry Scan", color = HeroCardBg)
                        }
                    }
                }
            } else {
                // Controls
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp)
                        .padding(horizontal = 32.dp)
                        .fillMaxWidth()
                ) {
                    // Gallery Button
                    IconButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, "Gallery", tint = Color.White)
                    }

                    // Capture Button
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(72.dp)
                            .background(HeroCardBg.copy(alpha = 0.8f), CircleShape)
                            .clip(CircleShape)
                            .clickable {
                                if (selectedBitmap == null && !isScanning) {
                                    capturePhoto()
                                } else {
                                    selectedBitmap = null
                                    viewModel.clearResult()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.Transparent, CircleShape)
                                .clip(CircleShape)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    color = Color.White,
                                    radius = size.width / 2,
                                    style = Stroke(width = 4.dp.toPx())
                                )
                            }
                            if (selectedBitmap != null) {
                                // Close button if we already took a photo
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(24.dp)
                                        .background(Color.White, CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            // Error Toast-like
            if (error != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 48.dp)
                        .background(Color.Red, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Text(text = error ?: "", color = Color.White)
                }
            }

            if (isScanning) {
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { /* Do nothing, block dismiss */ },
                    properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = GreenPrimary, modifier = Modifier.size(64.dp), strokeWidth = 6.dp)
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Analyzing...", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Please wait", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraLiveView(imageCapture: ImageCapture, modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    Log.e("CameraLiveView", "Use case binding failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        onRelease = {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(it.context)
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
        },
        modifier = modifier
    )
}

@Composable
fun AnimatedScannerOverlay(isScanning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val scanLineY = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_line_anim"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val boxWidth = size.width * 0.7f
        val boxHeight = size.width * 0.7f
        val left = (size.width - boxWidth) / 2
        val top = (size.height - boxHeight) / 2

        val length = 40.dp.toPx()
        val stroke = 4.dp.toPx()
        val cap = Stroke(width = stroke)

        val color = Color.White.copy(alpha = 0.8f)

        // Top-left
        drawLine(color, start = Offset(left, top), end = Offset(left + length, top), strokeWidth = stroke)
        drawLine(color, start = Offset(left, top), end = Offset(left, top + length), strokeWidth = stroke)

        // Top-right
        drawLine(color, start = Offset(left + boxWidth, top), end = Offset(left + boxWidth - length, top), strokeWidth = stroke)
        drawLine(color, start = Offset(left + boxWidth, top), end = Offset(left + boxWidth, top + length), strokeWidth = stroke)

        // Bottom-left
        drawLine(color, start = Offset(left, top + boxHeight), end = Offset(left + length, top + boxHeight), strokeWidth = stroke)
        drawLine(color, start = Offset(left, top + boxHeight), end = Offset(left, top + boxHeight - length), strokeWidth = stroke)

        // Bottom-right
        drawLine(color, start = Offset(left + boxWidth, top + boxHeight), end = Offset(left + boxWidth - length, top + boxHeight), strokeWidth = stroke)
        drawLine(color, start = Offset(left + boxWidth, top + boxHeight), end = Offset(left + boxWidth, top + boxHeight - length), strokeWidth = stroke)

        if (isScanning) {
            // Draw scanning line
            val currentY = top + (boxHeight * scanLineY.value)
            
            // Draw gradient beam
            val beamHeight = boxHeight * 0.3f
            val startY = (currentY - beamHeight).coerceAtLeast(top)
            val diffY = currentY - startY
            
            if (diffY > 0) {
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF6B9071).copy(alpha = 0.4f)), // GreenPrimary at 40%
                        startY = startY,
                        endY = currentY
                    ),
                    topLeft = Offset(left, startY),
                    size = Size(boxWidth, diffY)
                )
            }
            
            // Draw the leading line
            drawLine(
                color = Color(0xFF6B9071), // GreenPrimary
                start = Offset(left, currentY),
                end = Offset(left + boxWidth, currentY),
                strokeWidth = 3.dp.toPx()
            )
            
            // Draw scanning point in center
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = Offset(size.width / 2, currentY)
            )
            drawCircle(
                color = Color(0xFF6B9071), // GreenPrimary
                radius = 4.dp.toPx(),
                center = Offset(size.width / 2, currentY),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

