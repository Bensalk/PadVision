package com.bensal.tvlauncher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Channel(val name: String, val url: String, val group: String, val logo: String?)
data class AppItem(val label: String, val pkg: String, val icon: ImageBitmap, val intent: Intent)

fun parseM3u(text: String): List<Channel> {
    val out = mutableListOf<Channel>()
    var name = ""; var group = "Other"; var logo: String? = null
    val attr = Regex("([\\w-]+)=\"([^\"]*)\"")
    for (raw in text.lineSequence()) {
        val l = raw.trim()
        if (l.startsWith("#EXTINF")) {
            val a = attr.findAll(l).associate { it.groupValues[1] to it.groupValues[2] }
            group = a["group-title"].takeUnless { it.isNullOrBlank() } ?: "Other"
            logo = a["tvg-logo"]?.takeIf { it.isNotBlank() }
            name = l.substringAfterLast(",").trim()
        } else if (l.isNotEmpty() && !l.startsWith("#")) {
            out += Channel(name.ifBlank { l }, l, group, logo)
            name = ""; group = "Other"; logo = null
        }
    }
    return out
}

suspend fun fetch(u: String): List<Channel> = withContext(Dispatchers.IO) {
    runCatching {
        val c = URL(u).openConnection() as HttpURLConnection
        c.connectTimeout = 15000; c.readTimeout = 30000
        parseM3u(c.inputStream.bufferedReader().readText())
    }.getOrDefault(emptyList())
}

fun loadApps(ctx: Context): List<AppItem> {
    val pm = ctx.packageManager
    val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(i, 0).map {
        val pkg = it.activityInfo.packageName
        AppItem(
            it.loadLabel(pm).toString(), pkg, it.loadIcon(pm).toBitmap(192, 192).asImageBitmap(),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setClassName(pkg, it.activityInfo.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.filter { it.pkg != ctx.packageName }.sortedBy { it.label.lowercase() }
}

class Store(ctx: Context) {
    private val p = ctx.getSharedPreferences("tv", Context.MODE_PRIVATE)
    var playlists: List<String>
        get() = p.getString("pl", "")!!.split("\n").filter { it.isNotBlank() }
        set(v) { p.edit().putString("pl", v.joinToString("\n")).apply() }
    var favs: Set<String>
        get() = p.getStringSet("fav", emptySet())!!.toSet()
        set(v) { p.edit().putStringSet("fav", v).apply() }
    var last: String?
        get() = p.getString("last", null)
        set(v) { p.edit().putString("last", v).apply() }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF7C5CFF), background = Color(0xFF0B0D14),
                    surface = Color(0xFF151826), surfaceVariant = Color(0xFF1B1F31)
                )
            ) { App() }
        }
    }
}

@Composable
fun ChannelCard(c: Channel, fav: Boolean, mod: Modifier = Modifier, onFav: () -> Unit, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = mod, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = c.logo, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.name, maxLines = 1, fontWeight = FontWeight.Bold)
                Text(c.group, maxLines = 1, fontSize = 12.sp, color = Color.Gray)
            }
            Text(if (fav) "★" else "☆", fontSize = 24.sp, modifier = Modifier.clickable { onFav() }.padding(8.dp))
        }
    }
}

@Composable
fun AppTile(a: AppItem) {
    val ctx = LocalContext.current
    Column(
        Modifier.width(96.dp).clip(RoundedCornerShape(16.dp)).clickable { ctx.startActivity(a.intent) }.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(a.icon, null, Modifier.size(64.dp))
        Text(a.label, maxLines = 1, fontSize = 12.sp)
    }
}

@Composable
fun App() {
    val ctx = LocalContext.current
    val store = remember { Store(ctx) }
    val scope = rememberCoroutineScope()
    var channels by remember { mutableStateOf(listOf<Channel>()) }
    var apps by remember { mutableStateOf(listOf<AppItem>()) }
    var favs by remember { mutableStateOf(store.favs) }
    var pls by remember { mutableStateOf(store.playlists) }
    var playing by remember { mutableStateOf<Channel?>(null) }
    var tab by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    val time by produceState("") {
        while (true) {
            value = SimpleDateFormat("hh:mm a  •  EEE, d MMM", Locale.getDefault()).format(Date())
            delay(1000)
        }
    }
    fun reload() { scope.launch { loading = true; channels = pls.flatMap { fetch(it) }; loading = false } }
    val toggleFav: (Channel) -> Unit = { c ->
        favs = if (c.url in favs) favs - c.url else favs + c.url
        store.favs = favs
    }
    LaunchedEffect(Unit) { apps = withContext(Dispatchers.Default) { loadApps(ctx) }; reload() }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxSize().systemBarsPadding()) {
            NavigationRail(containerColor = Color.Transparent) {
                val icons = listOf("⌂", "📺", "▦", "⚙")
                listOf("Home", "Live TV", "Apps", "Setup").forEachIndexed { i, t ->
                    NavigationRailItem(selected = tab == i, onClick = { tab = i },
                        icon = { Text(icons[i], fontSize = 22.sp) }, label = { Text(t) })
                }
            }
            Box(Modifier.weight(1f).padding(16.dp)) {
                when (tab) {
                    0 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        item { Text(time, fontSize = 40.sp, fontWeight = FontWeight.Light) }
                        val resume = channels.firstOrNull { it.url == store.last }
                        if (resume != null) item {
                            Column {
                                Text("Continue watching", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                ChannelCard(resume, resume.url in favs, Modifier.width(300.dp), { toggleFav(resume) }) { playing = resume }
                            }
                        }
                        val fl = channels.filter { it.url in favs }
                        if (fl.isNotEmpty()) item {
                            Column {
                                Text("Favorites", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(fl) { c -> ChannelCard(c, true, Modifier.width(280.dp), { toggleFav(c) }) { playing = c } }
                                }
                            }
                        }
                        item {
                            Column {
                                Text("Apps", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                LazyRow { items(apps) { AppTile(it) } }
                            }
                        }
                    }
                    1 -> Column {
                        var q by remember { mutableStateOf("") }
                        var g by remember { mutableStateOf<String?>(null) }
                        val groups = channels.map { it.group }.distinct().sorted()
                        val shown = channels.filter {
                            (g == null || it.group == g) && it.name.contains(q, ignoreCase = true)
                        }
                        OutlinedTextField(q, { q = it }, singleLine = true, label = { Text("Search channels") }, modifier = Modifier.fillMaxWidth())
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                            item { FilterChip(g == null, { g = null }, { Text("All") }) }
                            items(groups) { x -> FilterChip(g == x, { g = x }, { Text(x) }) }
                        }
                        if (loading) Text("Loading channels…")
                        else if (channels.isEmpty()) Text("No channels yet. Add an M3U link in Setup.")
                        LazyVerticalGrid(GridCells.Adaptive(260.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(shown) { c -> ChannelCard(c, c.url in favs, Modifier.fillMaxWidth(), { toggleFav(c) }) { playing = c } }
                        }
                    }
                    2 -> LazyVerticalGrid(GridCells.Adaptive(110.dp)) { items(apps) { AppTile(it) } }
                    else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Playlists (M3U links)", fontWeight = FontWeight.Bold)
                        pls.forEach { u ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(u, Modifier.weight(1f), maxLines = 1)
                                TextButton(onClick = { pls = pls - u; store.playlists = pls; reload() }) { Text("Remove") }
                            }
                        }
                        Button(onClick = { showAdd = true }) { Text("Add M3U link") }
                        Button(onClick = { reload() }) { Text("Refresh") }
                    }
                }
            }
        }
        playing?.let { p ->
            PlayerScreen(channels, p, onBack = { playing = null }) { store.last = it.url }
        }
    }
    if (showAdd) {
        var t by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add playlist") },
            text = { OutlinedTextField(t, { t = it }, singleLine = true, label = { Text("M3U URL") }) },
            confirmButton = {
                TextButton(onClick = {
                    if (t.isNotBlank()) { pls = pls + t.trim(); store.playlists = pls; reload() }
                    showAdd = false
                }) { Text("Add") }
            }
        )
    }
}

@Composable
fun PlayerScreen(list: List<Channel>, start: Channel, onBack: () -> Unit, onPlay: (Channel) -> Unit) {
    val ctx = LocalContext.current
    var cur by remember { mutableStateOf(start) }
    var mi by remember { mutableStateOf(0) }
    val modes = listOf(AspectRatioFrameLayout.RESIZE_MODE_FIT, AspectRatioFrameLayout.RESIZE_MODE_FILL, AspectRatioFrameLayout.RESIZE_MODE_ZOOM)
    val exo = remember {
        ExoPlayer.Builder(ctx, DefaultRenderersFactory(ctx).setEnableDecoderFallback(true)).build().also { p ->
            p.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Handler(Looper.getMainLooper()).postDelayed({ p.prepare() }, 3000)
                }
            })
        }
    }
    DisposableEffect(Unit) {
        val w = (ctx as Activity).window
        val c = WindowCompat.getInsetsController(w, w.decorView)
        c.hide(WindowInsetsCompat.Type.systemBars())
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        w.decorView.keepScreenOn = true
        onDispose {
            exo.release()
            c.show(WindowInsetsCompat.Type.systemBars())
            w.decorView.keepScreenOn = false
        }
    }
    LaunchedEffect(cur) {
        exo.setMediaItem(MediaItem.fromUri(cur.url)); exo.prepare(); exo.playWhenReady = true; onPlay(cur)
    }
    fun step(d: Int) {
        val i = list.indexOfFirst { it.url == cur.url }
        if (i >= 0 && list.isNotEmpty()) cur = list[(i + d + list.size) % list.size]
    }
    BackHandler(onBack = onBack)
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { PlayerView(it).apply { player = exo; setShowSubtitleButton(true); controllerShowTimeoutMs = 3000 } },
            update = { it.resizeMode = modes[mi] },
            modifier = Modifier.fillMaxSize()
        )
        Row(Modifier.align(Alignment.TopStart).padding(12.dp)) { Text(cur.name, fontWeight = FontWeight.Bold, color = Color.White) }
        Row(Modifier.align(Alignment.TopEnd).padding(12.dp)) {
            TextButton(onClick = { step(-1) }) { Text("◀ Prev") }
            TextButton(onClick = { step(1) }) { Text("Next ▶") }
            TextButton(onClick = { mi = (mi + 1) % modes.size }) { Text("Aspect") }
            TextButton(onClick = onBack) { Text("✕ Close") }
        }
    }
}
