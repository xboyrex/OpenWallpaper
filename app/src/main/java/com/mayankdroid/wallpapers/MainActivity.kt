package com.mayankdroid.wallpapers

import android.Manifest
import android.app.DownloadManager
import android.app.WallpaperManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Size
import com.mayankdroid.wallpapers.data.Wallpaper
import com.mayankdroid.wallpapers.ui.FeedState
import com.mayankdroid.wallpapers.ui.WallpaperViewModel
import com.mayankdroid.wallpapers.work.ApplyWallpaperWorker
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) Toast.makeText(this, "Storage permission is required on Android 9 and below.", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OpenWallpaperApp(::requestLegacyStoragePermission) }
    }

    private fun requestLegacyStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT <= 28 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun OpenWallpaperApp(requestLegacyStoragePermission: () -> Unit, vm: WallpaperViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    var selected by remember { mutableStateOf<Wallpaper?>(null) }

    MaterialTheme {
        if (selected == null) {
            Scaffold(topBar = {
                TopAppBar(
                    title = { Text("OpenWallpaper") },
                    actions = {
                        IconButton(onClick = { vm.refresh() }) { Icon(Icons.Default.Refresh, "Refresh") }
                    }
                )
            }) { padding ->
                when (val s = state) {
                    FeedState.Loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
                    is FeedState.Error -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(s.message)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { vm.refresh() }) { Text("Retry") }
                        }
                    }
                    is FeedState.Success -> {
                        PullToRefreshBox(
                            isRefreshing = refreshing,
                            onRefresh = { vm.refresh() },
                            modifier = Modifier.fillMaxSize().padding(padding)
                        ) {
                            LazyVerticalStaggeredGrid(
                                columns = StaggeredGridCells.Adaptive(170.dp),
                                contentPadding = PaddingValues(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalItemSpacing = 8.dp,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(s.items, key = { it.id }) { wallpaper ->
                                    WallpaperCard(wallpaper) { selected = wallpaper }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            PreviewScreen(
                wallpaper = selected!!,
                onBack = { selected = null },
                requestLegacyStoragePermission = requestLegacyStoragePermission
            )
        }
    }
}

@Composable
private fun WallpaperCard(wallpaper: Wallpaper, onClick: () -> Unit) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(wallpaper.thumbUrl)
            .size(Size.ORIGINAL)
            .precision(Precision.INEXACT)
            .build(),
        contentDescription = wallpaper.title,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .pointerInput(wallpaper.id) { detectTapGestures(onTap = { onClick() }) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewScreen(
    wallpaper: Wallpaper,
    onBack: () -> Unit,
    requestLegacyStoragePermission: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var showApply by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Black,
        topBar = {
            TopAppBar(
                title = { Text(wallpaper.title, color = androidx.compose.ui.graphics.Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = androidx.compose.ui.graphics.Color.White) } },
                actions = {
                    IconButton(onClick = {
                        requestLegacyStoragePermission()
                        enqueueDownload(context, wallpaper)
                    }) { Icon(Icons.Default.Download, "Download", tint = androidx.compose.ui.graphics.Color.White) }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = .88f)).navigationBarsPadding().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(onClick = {
                    requestLegacyStoragePermission()
                    enqueueDownload(context, wallpaper)
                }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Download, null)
                    Spacer(Modifier.size(6.dp))
                    Text("Download")
                }
                Button(onClick = { showApply = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Wallpaper, null)
                    Spacer(Modifier.size(6.dp))
                    Text("Apply")
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val transformState = rememberTransformableState { zoomChange, panChange, _ ->
                scale = (scale * zoomChange).coerceIn(1f, 5f)
                offsetX += panChange.x
                offsetY += panChange.y
            }
            AsyncImage(
                model = ImageRequest.Builder(context).data(wallpaper.fullUrl).crossfade(true).build(),
                contentDescription = wallpaper.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY)
                    .transformable(transformState)
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = {
                            scale = if (scale > 1f) 1f else 2.5f
                            if (scale == 1f) { offsetX = 0f; offsetY = 0f }
                        })
                    }
            )
        }
    }

    if (showApply) {
        ApplySheet(
            wallpaper = wallpaper,
            onDismiss = { showApply = false },
            onApply = { which ->
                showApply = false
                scope.launch { enqueueApply(context, wallpaper, which) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApplySheet(wallpaper: Wallpaper, onDismiss: () -> Unit, onApply: (Int) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var expanded by remember { mutableStateOf(false) }
    var selectedLabel by remember { mutableStateOf("Both") }
    val options = listOf("Home screen" to WallpaperManager.FLAG_SYSTEM, "Lock screen" to WallpaperManager.FLAG_LOCK, "Both" to (WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK))

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("Apply wallpaper", style = MaterialTheme.typography.headlineSmall)
            Text("${wallpaper.width}×${wallpaper.height} • original 4K", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            Box {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(selectedLabel) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { (label, _) -> DropdownMenuItem(text = { Text(label) }, onClick = { selectedLabel = label; expanded = false }) }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { onApply(options.first { it.first == selectedLabel }.second) }, modifier = Modifier.fillMaxWidth()) { Text("Apply") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun enqueueDownload(context: Context, wallpaper: Wallpaper) {
    val filename = sanitizeFileName("${wallpaper.title}-${wallpaper.id}.jpg")
    val request = DownloadManager.Request(Uri.parse(wallpaper.fullUrl))
        .setTitle(wallpaper.title)
        .setDescription("4K wallpaper")
        .setMimeType("image/jpeg")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(false)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "Wallpapers/$filename")

    runCatching {
        context.getSystemService(DownloadManager::class.java).enqueue(request)
        Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
    }.onFailure { Toast.makeText(context, "Download failed: ${it.message}", Toast.LENGTH_LONG).show() }
}

private fun enqueueApply(context: Context, wallpaper: Wallpaper, which: Int) {
    val data = Data.Builder()
        .putString(ApplyWallpaperWorker.KEY_URL, wallpaper.fullUrl)
        .putString(ApplyWallpaperWorker.KEY_ID, wallpaper.id)
        .putInt(ApplyWallpaperWorker.KEY_WHICH, which)
        .build()
    val request = OneTimeWorkRequestBuilder<ApplyWallpaperWorker>()
        .setInputData(data)
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork("apply-${wallpaper.id}-$which", ExistingWorkPolicy.REPLACE, request)
    Toast.makeText(context, "Applying ${wallpaper.title}…", Toast.LENGTH_SHORT).show()
}

private fun sanitizeFileName(input: String): String = input.replace(Regex("[^A-Za-z0-9._-]+"), "_").take(120)

