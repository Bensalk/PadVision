package com.bensal.tvlauncher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
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
data class AppItem(val label: String, val pkg: String, val icon: ImageBitmap, val intent: Intent, val isGame: Boolean)
data class PTheme(val name: String, val bg: Long, val surface: Long, val accent: Long, val glow: Long)

val themes = listOf(
    PTheme("Midnight", 0xFF0B0D14, 0xFF151826, 0xFF7C5CFF, 0xFF1A1440),
    PTheme("AMOLED", 0xFF000000, 0xFF111111, 0xFFFFFFFF, 0xFF000000),
    PTheme("Neon", 0xFF07060F, 0xFF140F2A, 0xFF00F0FF, 0xFF2A0F3A),
    PTheme("Sunset", 0xFF1A0B12, 0xFF2A1420, 0xFFFF7A45, 0xFF4A1D2E),
    PTheme("Ocean", 0xFF06121C, 0xFF0D2233, 0xFF2EA8FF, 0xFF0B3A57),
    PTheme("Forest", 0xFF07130D, 0xFF0F2318, 0xFF3DDC84, 0xFF123B26)
)

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
        val game = runCatching { pm.getApplicationInfo(pkg, 0).category == ApplicationInfo.CATEGORY_GAME }.getOrDefault(false)
        AppItem(
            it.loadLabel(pm).toString(), pkg, it.loadIcon(pm).toBitmap(192, 192).asImageBitmap(),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setClassName(pkg, it.activityInfo.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), game
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
    var recents: List<String>
        get() = p.getString("rec", "")!!.split("\n").filter { it.isNotBlank() }
        set(v) { p.edit().putString("rec", v.joinToString("\n")).apply() }
    var theme: Int
        get() = p.getInt("theme", 0).coerceIn(0, themes.size - 1)
        set(v) { p.edit().putInt("theme", v).apply() }
    var pin: String
        get() = p.getString("pin", "")!!
        set(v) { p.edit().putString("pin", v).apply() }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        enableEdgeToEdge()
        setContent { Root() }
    }
}

@Composable
fun Root() {
    val ctx = LocalContext.current
    val store = remember { Store(ctx) }
    var ti by remember { mutableStateOf(store.theme) }
    val t = themes[ti]
    val accent = Color(t.accent)
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accent, onPrimary = if (accent.luminance() > 0.4f) Color.Black else Color.White,
            background = Color(t.bg), surface = Color(t.surface), surfaceVariant = Color(t.surface)
        )
    ) { App(store, ti) { ti = it; store.theme = it } }
}

@Composable
fun TvCard(c: Channel, fav: Boolean, onFav: () -> Unit, onClick: () -> Unit) {
    var f by remember { mutableStateOf(false) }
    val s by animateFloatAsState(if (f) 1.08f else 1f, label = "s")
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier.width(240.dp).height(140.dp).scale(s).clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(if (f) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, shape)
            .onFocusChanged { f = it.isFocused }.clickable(onClick = onClick)
    ) {
        AsyncImage(c.logo, null, Modifier.align(Alignment.Center).size(72.dp))
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color(0x99000000)).padding(8.dp)) {
            Text(c.name, maxLines = 1, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(c.group, maxLines = 1, fontSize = 11.sp, color = Color.LightGray)
        }
        Text(if (fav) "★" else "☆", Modifier.align(Alignment.TopEnd).clickable { onFav() }.padding(8.dp), fontSize = 20.sp)
    }
}

@Composable
fun AppTile(a: AppItem) {
    val ctx = LocalContext.current
    var f by remember { mutableStateOf(false) }
    val s by animateFloatAsState(if (f) 1.12f else 1f, label = "a")
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.width(100.dp).scale(s).clip(shape)
            .border(if (f) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, shape)
            .onFocusChanged { f = it.isFocused }.clickable { ctx.startActivity(a.intent) }.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(a.icon, null, Modifier.size(64.dp))
        Text(a.label, maxLines = 1, fontSize = 12.sp)
    }
}

@Composable
fun Shelf(title: String, content: LazyListScope.() -> Unit) {
    Column {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(vertical = 10.dp), content = content)
    }
}

@Composable
fun Hero(c: Channel?, fav: Boolean, onPlay: () -> Unit, onFav: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(24.dp))
            .background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), MaterialTheme.colorScheme.surface)))
    ) {
        if (c?.logo != null) AsyncImage(c.logo, null, Modifier.align(Alignment.CenterEnd).padding(24.dp).size(200.dp).blur(24.dp).alpha(0.5f))
        Column(Modifier.align(Alignment.CenterStart).padding(32.dp)) {
            Text(c?.name ?: "Welcome to PadVision", fontSize = 36.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(c?.group ?: "Add an M3U link in Settings to get started", color = Color.LightGray)
            if (c != null) {
                Spacer(Modifier.height(16.dp))
                Row {
                    Button(onClick = onPlay) { Text("▶ Play") }
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(onClick = onFav) { Text(if (fav) "★ Favorite" else "☆ Favorite") }
                }
            }
        }
    }
}

@Composable
fun NavItem(icon: String, label: String, sel: Boolean, expanded: Boolean, onClick: () -> Unit) {
    var f by remember { mutableStateOf(false) }
    val bg = when {
        sel -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        f -> Color.White.copy(alpha = 0.12f)
        else -> Color.Transparent
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(bg)
            .onFocusChanged { f = it.isFocused }.clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 20.sp)
        if (expanded) { Spacer(Modifier.width(12.dp)); Text(label, maxLines = 1) }
    }
}

@Composable
fun Sidebar(tab: Int, expanded: Boolean, onToggle: () -> Unit, onTab: (Int) -> Unit) {
    val w by animateDpAsState(if (expanded) 200.dp else 72.dp, label = "w")
    Column(Modifier.width(w).fillMaxHeight().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)).padding(8.dp)) {
        NavItem("☰", "PadVision", false, expanded) { onToggle() }
        Spacer(Modifier.height(8.dp))
        listOf("Home" to "⌂", "Live TV" to "📺", "Apps" to "▦", "Search" to "⌕", "Settings" to "⚙").forEachIndexed { i, (l, ic) ->
            NavItem(ic, l, tab == i, expanded) { onTab(i) }
        }
        Spacer(Modifier.weight(1f))
        NavItem("ⓘ", "About", tab == 5, expanded) { onTab(5) }
    }
}

@Composable
fun Settings(store: Store, ti: Int, onTheme: (Int) -> Unit, pls: List<String>, onAdd: () -> Unit, onRemove: (String) -> Unit, onRefresh: () -> Unit) {
    var ok by remember { mutableStateOf(store.pin.isEmpty()) }
    var entry by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    if (!ok) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Settings are locked", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            OutlinedTextField(entry, { v -> val n = v.filter { it.isDigit() }.take(8); entry = n; if (n == store.pin) ok = true }, singleLine = true, label = { Text("Enter PIN") })
        }
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Theme", fontWeight = FontWeight.Bold, fontSize = 20.sp) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(themes.size) { i ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onTheme(i) }) {
                        Box(Modifier.size(52.dp).clip(CircleShape).background(Color(themes[i].accent))
                            .border(if (i == ti) 4.dp else 0.dp, Color.White, CircleShape))
                        Text(themes[i].name, fontSize = 12.sp)
                    }
                }
            }
        }
        item { Text("Playlists (M3U links)", fontWeight = FontWeight.Bold, fontSize = 20.sp) }
        items(pls) { u ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(u, Modifier.weight(1f), maxLines = 1)
                TextButton(onClick = { onRemove(u) }) { Text("Remove") }
            }
        }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Button(onClick = onAdd) { Text("Add M3U link") }; OutlinedButton(onClick = onRefresh) { Text("Refresh") } } }
        item { Text("PIN lock for Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(newPin, { newPin = it.filter { c -> c.isDigit() }.take(8) }, singleLine = true, label = { Text("New PIN") })
                Button(onClick = { if (newPin.length >= 4) { store.pin = newPin; newPin = "" } }) { Text("Save") }
                OutlinedButton(onClick = { store.pin = "" }) { Text("Clear") }
            }
        }
    }
}

@Composable
fun About() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("PadVision", fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Text("TV-style launcher and live TV player for tablets")
        Text("Version 0.2", color = Color.LightGray)
        Spacer(Modifier.height(12.dp))
        Text("Created by Bensal", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Device: ${Build.MANUFACTURER} ${Build.MODEL}", color = Color.LightGray)
        Text("Built with Kotlin, Jetpack Compose and Media3", color = Color.LightGray)
    }
}

@Composable
fun App(store: Store, ti: Int, onTheme: (Int) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var channels by remember { mutableStateOf(listOf<Channel>()) }
    var apps by remember { mutableStateOf(listOf<AppItem>()) }
    var favs by remember { mutableStateOf(store.favs) }
    var pls by remember { mutableStateOf(store.playlists) }
    var recents by remember { mutableStateOf(store.recents) }
    var playing by remember { mutableStateOf<Channel?>(null) }
    var tab by remember { mutableStateOf(0) }
    var expanded by remember { mutableStateOf(false) }
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
    val cs = MaterialTheme.colorScheme

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(themes[ti].glow), cs.background)))) {
        Row(Modifier.fillMaxSize().systemBarsPadding()) {
            Sidebar(tab, expanded, { expanded = !expanded }) { tab = it }
            Box(Modifier.weight(1f).padding(16.dp)) {
                when (tab) {
                    0 -> {
                        val recentCh = recents.mapNotNull { u -> channels.firstOrNull { it.url == u } }
                        val hero = recentCh.firstOrNull() ?: channels.firstOrNull()
                        val groups = channels.map { it.group }.distinct().sorted()
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            item { Text(time, fontSize = 36.sp, fontWeight = FontWeight.Light) }
                            item { Hero(hero, hero != null && hero.url in favs, { hero?.let { playing = it } }, { hero?.let { toggleFav(it) } }) }
                            if (recentCh.isNotEmpty()) item {
                                Shelf("Continue Watching") { items(recentCh) { c -> TvCard(c, c.url in favs, { toggleFav(c) }) { playing = c } } }
                            }
                            val fl = channels.filter { it.url in favs }
                            if (fl.isNotEmpty()) item {
                                Shelf("Favorites") { items(fl) { c -> TvCard(c, true, { toggleFav(c) }) { playing = c } } }
                            }
                            val games = apps.filter { it.isGame }
                            if (games.isNotEmpty()) item { Shelf("Games") { items(games) { AppTile(it) } } }
                            item { Shelf("Apps") { items(apps) { AppTile(it) } } }
                            groups.take(15).forEach { g ->
                                item { Shelf(g) { items(channels.filter { it.group == g }.take(30)) { c -> TvCard(c, c.url in favs, { toggleFav(c) }) { playing = c } } } }
                            }
                        }
                    }
                    1 -> Column {
                        var g by remember { mutableStateOf<String?>(null) }
                        val groups = channels.map { it.group }.distinct().sorted()
                        val shown = channels.filter { g == null || it.group == g }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                            item { FilterChip(g == null, { g = null }, { Text("All") }) }
                            items(groups) { x -> FilterChip(g == x, { g = x }, { Text(x) }) }
                        }
                        if (loading) Text("Loading channels…")
                        else if (channels.isEmpty()) Text("No channels yet. Add an M3U link in Settings.")
                        LazyVerticalGrid(GridCells.Adaptive(250.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(shown) { c -> TvCard(c, c.url in favs, { toggleFav(c) }) { playing = c } }
                        }
                    }
                    2 -> LazyVerticalGrid(GridCells.Adaptive(110.dp)) { items(apps) { AppTile(it) } }
                    3 -> Column {
                        var q by remember { mutableStateOf("") }
                        OutlinedTextField(q, { q = it }, singleLine = true, label = { Text("Search channels and apps") }, modifier = Modifier.fillMaxWidth())
                        if (q.isNotBlank()) {
                            val ar = apps.filter { it.label.contains(q, true) }
                            if (ar.isNotEmpty()) Shelf("Apps") { items(ar) { AppTile(it) } }
                            val cr = channels.filter { it.name.contains(q, true) }.take(80)
                            LazyVerticalGrid(GridCells.Adaptive(250.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(cr) { c -> TvCard(c, c.url in favs, { toggleFav(c) }) { playing = c } }
                            }
                        }
                    }
                    4 -> Settings(store, ti, onTheme, pls, { showAdd = true },
                        { u -> pls = pls - u; store.playlists = pls; reload() }, { reload() })
                    else -> About()
                }
            }
        }
        playing?.let { p ->
            PlayerScreen(channels, p, onBack = { playing = null }) { c ->
                recents = (listOf(c.url) + recents).distinct().take(15); store.recents = recents
            }
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
    var panel by remember { mutableStateOf(false) }
    var sleepMin by remember { mutableStateOf(0) }
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
    LaunchedEffect(sleepMin) { if (sleepMin > 0) { delay(sleepMin * 60_000L); onBack() } }
    fun step(d: Int) {
        val i = list.indexOfFirst { it.url == cur.url }
        if (i >= 0 && list.isNotEmpty()) cur = list[(i + d + list.size) % list.size]
    }
    BackHandler { if (panel) panel = false else onBack() }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { PlayerView(it).apply { player = exo; setShowSubtitleButton(true); controllerShowTimeoutMs = 3000 } },
            update = { it.resizeMode = modes[mi] },
            modifier = Modifier.fillMaxSize()
        )
        Text(cur.name, Modifier.align(Alignment.TopStart).padding(16.dp), fontWeight = FontWeight.Bold, color = Color.White)
        Row(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            TextButton(onClick = { panel = !panel }) { Text("☰ Channels") }
            TextButton(onClick = { step(-1) }) { Text("◀") }
            TextButton(onClick = { step(1) }) { Text("▶") }
            TextButton(onClick = { mi = (mi + 1) % modes.size }) { Text("Aspect") }
            TextButton(onClick = { sleepMin = listOf(0, 15, 30, 60)[(listOf(0, 15, 30, 60).indexOf(sleepMin) + 1) % 4] }) {
                Text(if (sleepMin == 0) "Sleep" else "Sleep ${sleepMin}m")
            }
            TextButton(onClick = onBack) { Text("✕") }
        }
        if (panel) LazyColumn(Modifier.align(Alignment.CenterEnd).width(300.dp).fillMaxHeight().background(Color(0xDD000000)).padding(8.dp)) {
            items(list) { c ->
                Text(c.name, Modifier.fillMaxWidth().clickable { cur = c; panel = false }.padding(10.dp), maxLines = 1,
                    color = if (c.url == cur.url) MaterialTheme.colorScheme.primary else Color.White)
            }
        }
    }
}
