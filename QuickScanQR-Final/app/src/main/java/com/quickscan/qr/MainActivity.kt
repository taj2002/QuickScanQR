package com.quickscan.qr

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.webkit.URLUtil
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this)
        setContent { QuickScanApp() }
    }
}

@Composable
fun QuickScanApp() {
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf("") }
    var generatedText by remember { mutableStateOf("") }
    var history by remember { mutableStateOf(listOf<String>()) }

    val addHistory: (String) -> Unit = { value ->
        result = value
        history = (listOf(value) + history).distinct().take(30)
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val image = InputImage.fromFilePath(context, uri)
            BarcodeScanning.getClient().process(image)
                .addOnSuccessListener { codes ->
                    val value = codes.firstOrNull()?.rawValue
                    if (!value.isNullOrEmpty()) addHistory(value) else result = "No QR / Barcode found"
                }
                .addOnFailureListener { result = "Unable to scan image" }
        } catch (_: Exception) { result = "Unable to open image" }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Color(0xFF168CFF),
        background = Color(0xFF07111F),
        surface = Color(0xFF0D1A2B)
    )) {
        Column(Modifier.fillMaxSize().background(Color(0xFF07111F))) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("QuickScan ", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Text("QR", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF168CFF))
            }
            TabRow(selectedTabIndex = tab) {
                Tab(tab == 0, { tab = 0 }, text = { Text("Scan") })
                Tab(tab == 1, { tab = 1 }, text = { Text("Create") })
                Tab(tab == 2, { tab = 2 }, text = { Text("History") })
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                when (tab) {
                    0 -> ScanScreen(result, addHistory, { galleryLauncher.launch("image/*") }) { result = "" }
                    1 -> CreateQrScreen(generatedText) { generatedText = it }
                    2 -> HistoryScreen(history) { history = emptyList() }
                }
            }
            AdBanner()
        }
    }
}

@Composable
fun ScanScreen(result: String, onResult: (String) -> Unit, onGallery: () -> Unit, onClear: () -> Unit) {
    val context = LocalContext.current
    var hasCamera by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCamera = it }
    LaunchedEffect(Unit) { if (!hasCamera) permissionLauncher.launch(Manifest.permission.CAMERA) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Box(Modifier.fillMaxWidth().weight(1f).background(Color.Black, RoundedCornerShape(24.dp)), Alignment.Center) {
            if (hasCamera) CameraPreview(onResult, Modifier.fillMaxSize())
            else Text("Camera permission is required.", color = Color.White, modifier = Modifier.padding(24.dp))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onGallery, Modifier.weight(1f)) { Text("Gallery") }
            OutlinedButton(onClear, Modifier.weight(1f)) { Text("Clear") }
        }
        if (result.isNotEmpty()) {
            Card(Modifier.fillMaxWidth().padding(top = 10.dp), colors = CardDefaults.cardColors(Color(0xFF10243A))) {
                Column(Modifier.padding(14.dp)) {
                    Text("Scan Result", color = Color(0xFF7FC5FF))
                    Spacer(Modifier.height(6.dp))
                    Text(result, color = Color.White)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button({ copyText(context, result) }, Modifier.weight(1f)) { Text("Copy") }
                        Button({ shareText(context, result) }, Modifier.weight(1f)) { Text("Share") }
                    }
                    if (URLUtil.isNetworkUrl(result)) {
                        Spacer(Modifier.height(8.dp))
                        Button({ openLink(context, result) }, Modifier.fillMaxWidth()) { Text("Open Link") }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreview(onResult: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }
    DisposableEffect(Unit) { onDispose { scanner.close(); executor.shutdown() } }
    AndroidView(modifier = modifier, factory = { ctx ->
        val previewView = PreviewView(ctx)
        val providerFuture = ProcessCameraProvider.getInstance(ctx)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            analysis.setAnalyzer(executor) { proxy ->
                val mediaImage = proxy.image
                if (mediaImage == null) { proxy.close(); return@setAnalyzer }
                scanner.process(InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees))
                    .addOnSuccessListener { codes -> codes.firstOrNull()?.rawValue?.let(onResult) }
                    .addOnCompleteListener { proxy.close() }
            }
            provider.unbindAll()
            provider.bindToLifecycle(context as ComponentActivity, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        }, ContextCompat.getMainExecutor(ctx))
        previewView
    })
}

@Composable
fun CreateQrScreen(text: String, onTextChange: (String) -> Unit) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var message by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Create QR Code", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(text, onTextChange, Modifier.fillMaxWidth(), label = { Text("Enter text or URL") })
        Spacer(Modifier.height(16.dp))
        Button({ if (text.isNotBlank()) { bitmap = createQrBitmap(text); message = "" } }) { Text("Generate QR") }
        Spacer(Modifier.height(20.dp))
        bitmap?.let { qr ->
            AndroidView({ ctx -> android.widget.ImageView(ctx).apply { setImageBitmap(qr); adjustViewBounds = true } }, Modifier.size(250.dp))
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ message = if (saveBitmapToGallery(context, qr)) "QR saved to Gallery" else "Unable to save QR" }, Modifier.weight(1f)) { Text("Save") }
                Button({ shareText(context, text) }, Modifier.weight(1f)) { Text("Share") }
            }
        }
        if (message.isNotEmpty()) { Spacer(Modifier.height(10.dp)); Text(message, color = Color(0xFF7FC5FF)) }
    }
}

fun createQrBitmap(text: String): Bitmap {
    val size = 800
    val matrix: BitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    for (x in 0 until size) for (y in 0 until size) bitmap.setPixel(x, y, if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
    return bitmap
}

fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("QR Result", text))
}

fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
    context.startActivity(Intent.createChooser(intent, "Share"))
}

fun openLink(context: Context, text: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(text)))
}

fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Boolean = try {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "QuickScan_QR_${System.currentTimeMillis()}.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/QuickScan QR")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    context.contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) context.contentResolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
    true
} catch (_: Exception) { false }

@Composable
fun HistoryScreen(history: List<String>, onClear: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Scan History", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            TextButton(onClear) { Text("Clear") }
        }
        Spacer(Modifier.height(10.dp))
        if (history.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No scan history", color = Color(0xFF8FA5BB)) }
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(history) { item -> Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color(0xFF10243A))) { Text(item, Color.White, Modifier.padding(16.dp)) } }
        }
    }
}

@Composable
fun AdBanner() {
    AndroidView(Modifier.fillMaxWidth().height(60.dp), factory = { context ->
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = "ca-app-pub-3940256099942544/6300978111"
            loadAd(AdRequest.Builder().build())
        }
    })
}
