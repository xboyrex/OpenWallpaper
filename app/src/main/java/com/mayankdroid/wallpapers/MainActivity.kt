package com.mayankdroid.wallpapers

import android.Manifest
import android.app.DownloadManager
import android.app.WallpaperManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import coil3.request.crossfade
import coil3.size.Size
import com.mayankdroid.wallpapers.data.Wallpaper
import com.mayankdroid.wallpapers.ui.FeedState
import com.mayankdroid.wallpapers.ui.WallpaperViewModel
import com.mayankdroid.wallpapers.work.ApplyWallpaperWorker
import java.util.Locale

private val LightBackground = Color(0xFFFBF8FF)
private val LightSurface = Color(0xFFFFFFFF)
private val LightPrimary = Color(0xFF6848F5)
private val LightText = Color(0xFF19161F)
private val DarkBackground = Color(0xFF0B0A0F)
private val DarkSurface = Color(0xFF17151D)
private val DarkPrimary = Color(0xFFA78BFA)

class MainActivity : ComponentActivity() {
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) Toast.makeText(this, "Storage permission is only needed on Android 9 and below.", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { OpenWallpaperTheme { OpenWallpaperApp(::requestLegacyStoragePermission) } }
    }

    private fun requestLegacyStoragePermission() {
        if (Build.VERSION.SDK_INT <= 28 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
}

@Composable
private fun OpenWallpaperTheme(content: @Composable () -> Unit) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val colors = if (dark) {
        androidx.compose.material3.darkColorScheme(
            primary = DarkPrimary,
            background = DarkBackground,
            surface = DarkSurface,
            onBackground = Color.White,
            onSurface = Color.White
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = LightPrimary,
            background = LightBackground,
            surface = LightSurface,
            onBackground = LightText,
            onSurface = LightText
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun OpenWallpaperApp(
    requestLegacyStoragePermission: () -> Unit,
    vm: WallpaperViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    var selected by remember { mutableStateOf<Wallpaper?>(null) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }

    AnimatedContent(targetState = selected, label = "screen") { current ->
        if (current == null) {
            val allItems = (state as? FeedState.Success)?.items.orEmpty()
            val categories = remember(allItems) { listOf("All") + allItems.map { it.category }.filter { it.isNotBlank() }.distinct().sorted() }
            val filtered = remember(allItems, query, category) {
                allItems.filter { wallpaper ->
                    val matchesCategory = category == "All" || wallpaper.category.equals(category, ignoreCase = true)
                    val q = query.trim()
                    val matchesQuery = q.isBlank() || wallpaper.title.contains(q, true) || wallpaper.category.contains(q, true)
                    matchesCategory && matchesQuery
                }
            }
            HomeScreen(
                state = state,
                refreshing = refreshing,
                query = query,
                onQueryChange = { query = it },
                categories = categories,
                selectedCategory = category,
                onCategoryChange = { category = it },
                items = filtered,
                onRefresh = vm::refresh,
                onOpen = { selected = it }
            )
        } else {
            PreviewScreen(
                wallpaper = current,
                onBack = { selected = null },
                requestLegacyStoragePermission = requestLegacyStoragePermission
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    state: FeedState,
    refreshing: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    items: List<Wallpaper>,
    onRefresh: () -> Unit,
    onOpen: (Wallpaper) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp)
                    .padding(top = 10.dp, bottom = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = .12f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("OpenWallpaper", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-.5).sp)
                        Text("Premium 4K wallpapers", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f))
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, "Refresh feed")
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(17.dp),
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = if (query.isNotEmpty()) {
                        { IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Close, "Clear") } }
                    } else null,
                    placeholder = { Text("Search wallpapers, anime, nature…") }
                )
            }
        }
    ) { padding ->
        when (state) {
            FeedState.Loading -> LoadingState(Modifier.fillMaxSize().padding(padding))
            is FeedState.Error -> ErrorState(state.message, Modifier.fillMaxSize().padding(padding), onRefresh)
            is FeedState.Success -> {
                PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 30.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalItemSpacing = 12.dp,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            CategoryRow(categories, selectedCategory, onCategoryChange)
                        }
                        if (items.isNotEmpty()) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                FeaturedSection(items.take(5), onOpen)
                            }
                        }
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("Explore", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-.4).sp)
                                    Text("${items.size} wallpapers", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                                }
                                if (selectedCategory != "All") {
                                    Surface(shape = RoundedCornerShape(100.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = .10f)) {
                                        Text(selectedCategory, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                        if (items.isEmpty()) {
                            item(span = StaggeredGridItemSpan.FullLine) { EmptyState(query, selectedCategory) }
                        } else {
                            items(items, key = { it.id }) { wallpaper ->
                                WallpaperCard(wallpaper, onOpen)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(categories: List<String>, selected: String, onSelect: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories, key = { it }) { category ->
            FilterChip(
                selected = category == selected,
                onClick = { onSelect(category) },
                label = { Text(category, fontWeight = FontWeight.SemiBold) },
                shape = RoundedCornerShape(100.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun FeaturedSection(items: List<Wallpaper>, onOpen: (Wallpaper) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Featured", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-.4).sp)
                Text("Fresh from the feed", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
            }
            Icon(Icons.Default.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .35f))
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.id }) { wallpaper -> FeaturedCard(wallpaper, onOpen) }
        }
    }
}

@Composable
private fun FeaturedCard(wallpaper: Wallpaper, onOpen: (Wallpaper) -> Unit) {
    Box(
        modifier = Modifier
            .width(286.dp)
            .height(182.dp)
            .clip(RoundedCornerShape(26.dp))
            .shadow(8.dp, RoundedCornerShape(26.dp), clip = false)
            .clickable { onOpen(wallpaper) }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(wallpaper.thumbUrl)
                .size(720, 460)
                .crossfade(true)
                .build(),
            contentDescription = wallpaper.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .76f)))
            )
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
        ) {
            Surface(shape = RoundedCornerShape(100.dp), color = Color.White.copy(alpha = .16f), contentColor = Color.White) {
                Text(wallpaper.category, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(7.dp))
            Text(wallpaper.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${wallpaper.width} × ${wallpaper.height}", color = Color.White.copy(alpha = .75f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun WallpaperCard(wallpaper: Wallpaper, onOpen: (Wallpaper) -> Unit) {
    val ratio = (wallpaper.width.toFloat() / wallpaper.height.toFloat()).coerceIn(.62f, 1.0f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(23.dp))
            .shadow(6.dp, RoundedCornerShape(23.dp), clip = false)
            .clickable { onOpen(wallpaper) }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(wallpaper.thumbUrl)
                .size(520, 760)
                .crossfade(true)
                .build(),
            contentDescription = wallpaper.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .70f)))))
        Surface(
            modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
            shape = RoundedCornerShape(100.dp),
            color = Color.Black.copy(alpha = .34f),
            contentColor = Color.White
        ) {
            Text("4K", modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(13.dp)) {
            Text(wallpaper.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(wallpaper.category, color = Color.White.copy(alpha = .72f), fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(strokeWidth = 3.dp)
            Spacer(Modifier.height(14.dp))
            Text("Loading wallpapers…", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ErrorState(message: String, modifier: Modifier, retry: () -> Unit) {
    Box(modifier.padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = .10f)) {
                Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(18.dp).size(30.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("Couldn’t load the feed", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(6.dp))
            Text(message, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f), fontSize = 13.sp)
            Spacer(Modifier.height(18.dp))
            Button(onClick = retry) { Text("Try again") }
        }
    }
}

@Composable
private fun EmptyState(query: String, category: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 50.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = .10f)) {
            Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(20.dp).size(30.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text("No wallpapers found", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            if (query.isNotBlank()) "Try another search." else "Try another category.",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f),
            fontSize = 13.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewScreen(
    wallpaper: Wallpaper,
    onBack: () -> Unit,
    requestLegacyStoragePermission: () -> Unit
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var showApply by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(wallpaper.fullUrl).crossfade(true).build(),
            contentDescription = wallpaper.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(18.dp), color = Color.Black.copy(alpha = .42f), contentColor = Color.White) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            }
            Spacer(Modifier.width(10.dp))
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                color = Color.Black.copy(alpha = .42f),
                contentColor = Color.White
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(wallpaper.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${wallpaper.width} × ${wallpaper.height}  •  ${wallpaper.category}", fontSize = 10.sp, color = Color.White.copy(alpha = .72f))
                }
            }
            Spacer(Modifier.width(10.dp))
            Surface(shape = RoundedCornerShape(18.dp), color = Color.Black.copy(alpha = .42f), contentColor = Color.White) {
                IconButton(onClick = {
                    requestLegacyStoragePermission()
                    enqueueDownload(context, wallpaper)
                }) { Icon(Icons.Default.Download, "Download") }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    requestLegacyStoragePermission()
                    enqueueDownload(context, wallpaper)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Default.Download, null)
                Spacer(Modifier.width(7.dp))
                Text("Download", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { showApply = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Wallpaper, null)
                Spacer(Modifier.width(7.dp))
                Text("Apply", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showApply) {
        ApplySheet(wallpaper, onDismiss = { showApply = false }) { which ->
            showApply = false
            enqueueApply(context, wallpaper, which)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApplySheet(wallpaper: Wallpaper, onDismiss: () -> Unit, onApply: (Int) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var expanded by remember { mutableStateOf(false) }
    var selectedLabel by remember { mutableStateOf("Both") }
    val options = listOf(
        "Home screen" to WallpaperManager.FLAG_SYSTEM,
        "Lock screen" to WallpaperManager.FLAG_LOCK,
        "Both" to (WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
    )

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("Apply wallpaper", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text("Choose where ${wallpaper.title} should appear.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f), fontSize = 13.sp)
            Spacer(Modifier.height(18.dp))
            Box {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Text(selectedLabel, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowForward, null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { (label, _) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            leadingIcon = { if (label == selectedLabel) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = { selectedLabel = label; expanded = false }
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { onApply(options.first { it.first == selectedLabel }.second) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Wallpaper, null)
                Spacer(Modifier.width(8.dp))
                Text("Apply now", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun enqueueDownload(context: Context, wallpaper: Wallpaper) {
    val extension = Uri.parse(wallpaper.fullUrl).lastPathSegment?.substringAfterLast('.', "jpg")?.lowercase(Locale.US)?.takeIf { it in setOf("jpg", "jpeg", "png", "webp") } ?: "jpg"
    val mime = when (extension) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        else -> "image/jpeg"
    }
    val filename = sanitizeFileName("${wallpaper.title}-${wallpaper.id}.$extension")
    val request = DownloadManager.Request(Uri.parse(wallpaper.fullUrl))
        .setTitle(wallpaper.title)
        .setDescription("4K wallpaper")
        .setMimeType(mime)
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(false)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "Wallpapers/$filename")

    runCatching {
        context.getSystemService(DownloadManager::class.java).enqueue(request)
        Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
    }.onFailure {
        Toast.makeText(context, "Download failed: ${it.message}", Toast.LENGTH_LONG).show()
    }
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
    Toast.makeText(context, "Applying wallpaper…", Toast.LENGTH_SHORT).show()
}

private fun sanitizeFileName(input: String): String = input.replace(Regex("[^A-Za-z0-9._-]+"), "_").take(120)
