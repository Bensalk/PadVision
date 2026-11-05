package com.bensal.tvlauncher

import android.app.Activity
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.zip.GZIPInputStream
import android.speech.RecognizerIntent
import androidx.compose.ui.input.pointer.PointerEventPass
import android.media.audiofx.LoudnessEnhancer
import androidx.media3.exoplayer.DefaultLoadControl
import org.json.JSONArray
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.net.Uri
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import android.view.View
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import android.app.PictureInPictureParams
import android.util.Rational
import org.json.JSONObject
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.layout.ContentScale
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

data class Channel(val name: String, val url: String, val group: String, val logo: String?, val tvgId: String? = null)
data class AppItem(val label: String, val pkg: String, val icon: ImageBitmap, val intent: Intent, val isGame: Boolean)
data class Lib(val section: String, val name: String, val url: String)
const val IPTV = "https://iptv-org.github.io/iptv/"
val library = listOf(
    Lib("Popular", "All channels (very large, slow to load)", IPTV + "index.m3u"),
    Lib("Popular", "India", IPTV + "countries/in.m3u"),
    Lib("Popular", "Malayalam", IPTV + "languages/mal.m3u"),
    Lib("Popular", "English", IPTV + "languages/eng.m3u"),
    Lib("TV Shows & More", "Series", IPTV + "categories/series.m3u"),
    Lib("TV Shows & More", "Animation", IPTV + "categories/animation.m3u"),
    Lib("TV Shows & More", "Family", IPTV + "categories/family.m3u"),
    Lib("TV Shows & More", "Classic", IPTV + "categories/classic.m3u"),
    Lib("TV Shows & More", "Culture", IPTV + "categories/culture.m3u"),
    Lib("TV Shows & More", "Lifestyle", IPTV + "categories/lifestyle.m3u"),
    Lib("TV Shows & More", "Cooking", IPTV + "categories/cooking.m3u"),
    Lib("TV Shows & More", "Travel", IPTV + "categories/travel.m3u"),
    Lib("Categories", "News", IPTV + "categories/news.m3u"),
    Lib("Categories", "Music", IPTV + "categories/music.m3u"),
    Lib("Categories", "Sports", IPTV + "categories/sports.m3u"),
    Lib("Categories", "Movies", IPTV + "categories/movies.m3u"),
    Lib("Categories", "Kids", IPTV + "categories/kids.m3u"),
    Lib("Categories", "Documentary", IPTV + "categories/documentary.m3u"),
    Lib("Categories", "Entertainment", IPTV + "categories/entertainment.m3u"),
    Lib("Categories", "Comedy", IPTV + "categories/comedy.m3u"),
    Lib("Languages", "Hindi", IPTV + "languages/hin.m3u"),
    Lib("Languages", "Tamil", IPTV + "languages/tam.m3u"),
    Lib("Languages", "Telugu", IPTV + "languages/tel.m3u"),
    Lib("Languages", "Kannada", IPTV + "languages/kan.m3u"),
    Lib("Languages", "Arabic", IPTV + "languages/ara.m3u"),
    Lib("Countries", "United Arab Emirates", IPTV + "countries/ae.m3u"),
    Lib("Countries", "Qatar", IPTV + "countries/qa.m3u"),
    Lib("Countries", "Saudi Arabia", IPTV + "countries/sa.m3u"),
    Lib("Countries", "United Kingdom", IPTV + "countries/uk.m3u"),
    Lib("Countries", "United States", IPTV + "countries/us.m3u"),
    Lib("Countries", "Pakistan", IPTV + "countries/pk.m3u"),
    Lib("Countries", "Bangladesh", IPTV + "countries/bd.m3u"),
    Lib("Countries", "Sri Lanka", IPTV + "countries/lk.m3u"),
    Lib("Countries", "Nepal", IPTV + "countries/np.m3u"),
    Lib("Countries", "Singapore", IPTV + "countries/sg.m3u"),
    Lib("Countries", "Malaysia", IPTV + "countries/my.m3u"),
    Lib("Countries", "Indonesia", IPTV + "countries/id.m3u"),
    Lib("Countries", "Philippines", IPTV + "countries/ph.m3u"),
    Lib("Countries", "Australia", IPTV + "countries/au.m3u"),
    Lib("Countries", "Canada", IPTV + "countries/ca.m3u"),
    Lib("Countries", "Germany", IPTV + "countries/de.m3u"),
    Lib("Countries", "France", IPTV + "countries/fr.m3u"),
    Lib("Countries", "Spain", IPTV + "countries/es.m3u"),
    Lib("Countries", "Italy", IPTV + "countries/it.m3u"),
    Lib("Countries", "Turkey", IPTV + "countries/tr.m3u"),
    Lib("Countries", "Russia", IPTV + "countries/ru.m3u"),
    Lib("Countries", "Japan", IPTV + "countries/jp.m3u"),
    Lib("Countries", "South Korea", IPTV + "countries/kr.m3u"),
    Lib("Countries", "South Africa", IPTV + "countries/za.m3u"),
    Lib("Countries", "Nigeria", IPTV + "countries/ng.m3u"),
    Lib("Countries", "Kenya", IPTV + "countries/ke.m3u"),
    Lib("Countries", "Egypt", IPTV + "countries/eg.m3u"),
    Lib("Countries", "Kuwait", IPTV + "countries/kw.m3u"),
    Lib("Countries", "Oman", IPTV + "countries/om.m3u"),
    Lib("Countries", "Bahrain", IPTV + "countries/bh.m3u"),
    Lib("Languages", "Bengali", IPTV + "languages/ben.m3u"),
    Lib("Languages", "Urdu", IPTV + "languages/urd.m3u"),
    Lib("Languages", "Marathi", IPTV + "languages/mar.m3u"),
    Lib("Languages", "Punjabi", IPTV + "languages/pan.m3u"),
    Lib("Languages", "Gujarati", IPTV + "languages/guj.m3u"),
    Lib("Languages", "Spanish", IPTV + "languages/spa.m3u"),
    Lib("Languages", "French", IPTV + "languages/fra.m3u"),
    Lib("Languages", "German", IPTV + "languages/deu.m3u"),
    Lib("Languages", "Portuguese", IPTV + "languages/por.m3u"),
    Lib("Languages", "Russian", IPTV + "languages/rus.m3u"),
    Lib("Languages", "Turkish", IPTV + "languages/tur.m3u"),
    Lib("Languages", "Indonesian", IPTV + "languages/ind.m3u"),
    Lib("Languages", "Chinese", IPTV + "languages/zho.m3u"),
    Lib("Languages", "Japanese", IPTV + "languages/jpn.m3u"),
    Lib("Languages", "Korean", IPTV + "languages/kor.m3u"),
    Lib("Categories", "General", IPTV + "categories/general.m3u"),
    Lib("Categories", "Religious", IPTV + "categories/religious.m3u"),
    Lib("Categories", "Education", IPTV + "categories/education.m3u"),
    Lib("Categories", "Science", IPTV + "categories/science.m3u"),
    Lib("Categories", "Business", IPTV + "categories/business.m3u"),
    Lib("Categories", "Weather", IPTV + "categories/weather.m3u"),
    Lib("Categories", "Outdoor", IPTV + "categories/outdoor.m3u"),
    Lib("Categories", "Relax", IPTV + "categories/relax.m3u"),
    Lib("Everything (huge, slow)", "All channels by country", IPTV + "index.country.m3u"),
    Lib("Everything (huge, slow)", "All channels by category", IPTV + "index.category.m3u"),
    Lib("Everything (huge, slow)", "All channels by language", IPTV + "index.language.m3u")
)
data class PTheme(val name: String, val bg: Long, val surface: Long, val accent: Long, val glow: Long)

val themes = listOf(
    PTheme("Midnight", 0xFF0B0D14, 0xFF151826, 0xFF7C5CFF, 0xFF1A1440),
    PTheme("AMOLED", 0xFF000000, 0xFF111111, 0xFFFFFFFF, 0xFF000000),
    PTheme("Neon", 0xFF07060F, 0xFF140F2A, 0xFF00F0FF, 0xFF2A0F3A),
    PTheme("Sunset", 0xFF1A0B12, 0xFF2A1420, 0xFFFF7A45, 0xFF4A1D2E),
    PTheme("Ocean", 0xFF06121C, 0xFF0D2233, 0xFF2EA8FF, 0xFF0B3A57),
    PTheme("Forest", 0xFF07130D, 0xFF0F2318, 0xFF3DDC84, 0xFF123B26)
)

data class Prog(val start: Long, val stop: Long, val title: String)
object Epg {
    var data: Map<String, List<Prog>> = emptyMap()
    var hint: String? = null
}
var epgTick by mutableStateOf(0)
val HM = SimpleDateFormat("HH:mm", Locale.getDefault())

fun nowNext(c: Channel): Pair<Prog?, Prog?> {
    val l = c.tvgId?.let { Epg.data[it] } ?: return null to null
    val t = System.currentTimeMillis()
    val i = l.indexOfFirst { it.stop > t }
    if (i < 0) return null to null
    val p = l[i]
    return if (p.start <= t) p to l.getOrNull(i + 1) else null to p
}

suspend fun loadEpg(u: String) = withContext(Dispatchers.IO) {
    runCatching {
        val c = URL(u).openConnection() as HttpURLConnection
        c.connectTimeout = 15000; c.readTimeout = 60000
        var ins: InputStream = c.inputStream
        if (u.substringBefore('?').endsWith(".gz")) ins = GZIPInputStream(ins)
        val parser = Xml.newPullParser()
        parser.setInput(ins, null)
        val fmt = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)
        val now = System.currentTimeMillis(); val limit = now + 24 * 3600_000L
        val map = HashMap<String, MutableList<Prog>>()
        var ch: String? = null; var st = 0L; var en = 0L; var title = ""
        var ev = parser.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG) {
                if (parser.name == "programme") {
                    ch = parser.getAttributeValue(null, "channel")
                    st = runCatching { fmt.parse(parser.getAttributeValue(null, "start"))!!.time }.getOrDefault(0L)
                    en = runCatching { fmt.parse(parser.getAttributeValue(null, "stop"))!!.time }.getOrDefault(0L)
                    title = ""
                } else if (parser.name == "title" && title.isEmpty()) {
                    title = parser.nextText()
                }
            } else if (ev == XmlPullParser.END_TAG && parser.name == "programme") {
                if (ch != null && en > now && st < limit && title.isNotEmpty()) map.getOrPut(ch) { mutableListOf() }.add(Prog(st, en, title))
            }
            ev = parser.next()
        }
        map.values.forEach { it.sortBy { p -> p.start } }
        Epg.data = map
    }
}

fun bufferControl(level: Int) = when (level) {
    0 -> DefaultLoadControl.Builder().setBufferDurationsMs(5_000, 15_000, 1_000, 2_000).build()
    2 -> DefaultLoadControl.Builder().setBufferDurationsMs(30_000, 120_000, 2_500, 5_000).build()
    else -> DefaultLoadControl.Builder().build()
}

suspend fun alive(u: String): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val c = URL(u).openConnection() as HttpURLConnection
        c.connectTimeout = 4000; c.readTimeout = 4000
        c.setRequestProperty("Range", "bytes=0-512")
        val code = c.responseCode
        c.disconnect()
        code in 200..399
    }.getOrDefault(false)
}

val kidsWords = Regex("(?i)kid|child|cartoon|animation|family|junior|baby|toon")
val qRegex = Regex("(?i)(\\d{3,4})p|\\b4k\\b|\\buhd\\b|\\b2160\\b")
fun quality(c: Channel): Int {
    val m = qRegex.find(c.name) ?: return 0
    val g = m.groupValues[1]
    return if (g.isNotEmpty()) g.toInt() else 2160
}
val vodWords = Regex("(?i)movie|series|drama|show|film|vod|cinema")
fun isVod(c: Channel): Boolean {
    val u = c.url.substringBefore('?').lowercase()
    return u.endsWith(".mp4") || u.endsWith(".mkv") || u.endsWith(".avi") || u.endsWith(".mov") || vodWords.containsMatchIn(c.group)
}

fun parseM3u(text: String): List<Channel> {
    val out = mutableListOf<Channel>()
    var name = ""; var group = "Other"; var logo: String? = null; var tvg: String? = null
    val attr = Regex("([\\w-]+)=\"([^\"]*)\"")
    for (raw in text.lineSequence()) {
        val l = raw.trim()
        if (l.startsWith("#EXTM3U")) {
            val h = attr.findAll(l).associate { it.groupValues[1] to it.groupValues[2] }
            (h["url-tvg"] ?: h["x-tvg-url"])?.split(",")?.firstOrNull()?.trim()?.takeIf { it.startsWith("http") }?.let { Epg.hint = it }
        }
        if (l.startsWith("#EXTINF")) {
            val a = attr.findAll(l).associate { it.groupValues[1] to it.groupValues[2] }
            group = a["group-title"].takeUnless { it.isNullOrBlank() } ?: "Other"
            logo = a["tvg-logo"]?.takeIf { it.isNotBlank() }
            tvg = a["tvg-id"]?.takeIf { it.isNotBlank() }
            name = l.substringAfterLast(",").trim()
        } else if (l.isNotEmpty() && !l.startsWith("#")) {
            out += Channel(name.ifBlank { l }, l, group, logo, tvg)
            name = ""; group = "Other"; logo = null; tvg = null
        }
    }
    return out
}

object AppCtx { lateinit var ctx: Context }
val noHide: (Channel) -> Unit = {}
val LocalHide = compositionLocalOf { noHide }

suspend fun fetch(u: String): List<Channel> = withContext(Dispatchers.IO) {
    runCatching {
        val text = if (u.startsWith("content://")) {
            AppCtx.ctx.contentResolver.openInputStream(Uri.parse(u))!!.bufferedReader().readText()
        } else {
            val c = URL(u).openConnection() as HttpURLConnection
            c.connectTimeout = 15000; c.readTimeout = 30000
            c.inputStream.bufferedReader().readText()
        }
        parseM3u(text)
    }.getOrDefault(emptyList())
}

suspend fun fetchWeather(city: String): String = withContext(Dispatchers.IO) {
    runCatching {
        val g = JSONObject(URL("https://geocoding-api.open-meteo.com/v1/search?count=1&name=" + java.net.URLEncoder.encode(city, "UTF-8")).readText())
        val r = g.getJSONArray("results").getJSONObject(0)
        val w = JSONObject(URL("https://api.open-meteo.com/v1/forecast?current_weather=true&latitude=${r.getDouble("latitude")}&longitude=${r.getDouble("longitude")}").readText())
        "${r.getString("name")}  ${w.getJSONObject("current_weather").getDouble("temperature").toInt()}°C"
    }.getOrDefault("")
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

class Store(ctx: Context, val profile: Int = 0) {
    private val p = ctx.getSharedPreferences(if (profile == 0) "tv" else "tv_p$profile", Context.MODE_PRIVATE)
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
    var hidden: Set<String>
        get() = p.getStringSet("hid", emptySet())!!.toSet()
        set(v) { p.edit().putStringSet("hid", v).apply() }
    var hiddenCh: Set<String>
        get() = p.getStringSet("hidch", emptySet())!!.toSet()
        set(v) { p.edit().putStringSet("hidch", v).apply() }
    private var accState by mutableStateOf(p.getFloat("accent", -1f))
    var accent: Float
        get() = accState
        set(v) { accState = v; p.edit().putFloat("accent", v).apply() }
    var buffer: Int
        get() = p.getInt("buf", 1)
        set(v) { p.edit().putInt("buf", v).apply() }
    var kids: Boolean
        get() = p.getBoolean("kids", profile == 3)
        set(v) { p.edit().putBoolean("kids", v).apply() }
    var searches: List<String>
        get() = p.getString("srch", "")!!.split("\n").filter { it.isNotBlank() }
        set(v) { p.edit().putString("srch", v.joinToString("\n")).apply() }
    var epgUrl: String
        get() = p.getString("epg", "")!!
        set(v) { p.edit().putString("epg", v).apply() }
    var folders: Map<String, String>
        get() = p.getString("fold", "")!!.lines().filter { it.contains("=") }.associate { it.substringBefore("=") to it.substringAfter("=") }
        set(v) { p.edit().putString("fold", v.entries.joinToString("\n") { it.key + "=" + it.value }).apply() }
    var minQ: Int
        get() = p.getInt("minq", 0)
        set(v) { p.edit().putInt("minq", v).apply() }
    fun bump(u: String) { p.edit().putInt("cnt_" + u, p.getInt("cnt_" + u, 0) + 1).apply() }
    fun clearCounts() { val e = p.edit(); p.all.keys.filter { it.startsWith("cnt_") }.forEach { e.remove(it) }; e.apply() }
    fun top(l: List<Channel>): List<Channel> = l.map { it to p.getInt("cnt_" + it.url, 0) }.filter { it.second > 0 }.sortedByDescending { it.second }.take(15).map { it.first }
    var wall: String
        get() = p.getString("wall", "")!!
        set(v) { p.edit().putString("wall", v).apply() }
    var city: String
        get() = p.getString("city", "")!!
        set(v) { p.edit().putString("city", v).apply() }
    var auto: Boolean
        get() = p.getBoolean("auto", false)
        set(v) { p.edit().putBoolean("auto", v).apply() }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        enableEdgeToEdge()
        AppCtx.ctx = applicationContext
        runCatching {
            @Suppress("DEPRECATION")
            val d = windowManager.defaultDisplay
            val cur = d.mode
            val best = d.supportedModes
                .filter { it.physicalWidth == cur.physicalWidth && it.physicalHeight == cur.physicalHeight }
                .maxByOrNull { it.refreshRate }
            if (best != null) window.attributes = window.attributes.also {
                it.preferredDisplayModeId = best.modeId
                it.preferredRefreshRate = best.refreshRate
            }
        }
        setContent { Root() }
    }
}

@Composable
fun Root() {
    val ctx = LocalContext.current
    val meta = remember { ctx.getSharedPreferences("meta", Context.MODE_PRIVATE) }
    var prof by remember { mutableStateOf(meta.getInt("prof", 0)) }
    val store = remember(prof) { Store(ctx, prof) }
    var ti by remember(prof) { mutableStateOf(store.theme) }
    val t = themes[ti]
    val accent = if (store.accent >= 0f) Color.hsv(store.accent * 360f, 0.65f, 1f) else Color(t.accent)
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accent, onPrimary = if (accent.luminance() > 0.4f) Color.Black else Color.White,
            background = Color(t.bg), surface = Color(t.surface), surfaceVariant = Color(t.surface)
        )
    ) { CompositionLocalProvider(LocalContentColor provides Color.White) { key(prof) { App(store, ti, { pr -> meta.edit().putInt("prof", pr).apply(); prof = pr }) { ti = it; store.theme = it } } } }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvCard(c: Channel, fav: Boolean, onFav: () -> Unit, onClick: () -> Unit) {
    val hide = LocalHide.current
    var f by remember { mutableStateOf(false) }
    val s by animateFloatAsState(if (f) 1.08f else 1f, label = "s")
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier.width(240.dp).height(140.dp).scale(s).clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(if (f) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, shape)
            .onFocusChanged { f = it.isFocused }.combinedClickable(onLongClick = { hide(c) }, onClick = onClick)
    ) {
        AsyncImage(c.logo, null, Modifier.align(Alignment.Center).size(72.dp))
        val q = quality(c)
        if (q >= 720) Text(if (q >= 2160) "4K" else q.toString() + "p", Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.primary).padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color(0x99000000)).padding(8.dp)) {
            Text(c.name, maxLines = 1, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text((if (epgTick >= 0) nowNext(c).first?.title else null) ?: c.group, maxLines = 1, fontSize = 11.sp, color = Color.LightGray)
        }
        Text(if (fav) "★" else "☆", Modifier.align(Alignment.TopEnd).clickable { onFav() }.padding(8.dp), fontSize = 20.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTile(a: AppItem, onHide: () -> Unit = {}) {
    val ctx = LocalContext.current
    var f by remember { mutableStateOf(false) }
    val s by animateFloatAsState(if (f) 1.12f else 1f, label = "a")
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.width(100.dp).scale(s).clip(shape)
            .border(if (f) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, shape)
            .onFocusChanged { f = it.isFocused }.combinedClickable(onLongClick = onHide) { ctx.startActivity(a.intent) }.padding(8.dp),
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
        if (c?.logo != null) AsyncImage(c.logo, null, Modifier.align(Alignment.CenterEnd).padding(24.dp).size(200.dp).alpha(0.5f))
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
    Column(Modifier.width(w).fillMaxHeight().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)).padding(8.dp)) {
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
fun Settings(store: Store, ti: Int, onTheme: (Int) -> Unit, onProfile: (Int) -> Unit, pls: List<String>, onAdd: () -> Unit, onRemove: (String) -> Unit, onRefresh: () -> Unit, onChange: () -> Unit, onAddUrl: (String) -> Unit) {
    val ctx = LocalContext.current
    var city by remember { mutableStateOf(store.city) }
    var auto by remember { mutableStateOf(store.auto) }
    var buf by remember { mutableStateOf(store.buffer) }
    var libSec by remember { mutableStateOf("Popular") }
    var kidsOn by remember { mutableStateOf(store.kids) }
    var epgText by remember { mutableStateOf(store.epgUrl) }
    var msg by remember { mutableStateOf("") }
    val clip = LocalClipboardManager.current
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            store.wall = uri.toString(); onChange()
        }
    }
    var ok by remember { mutableStateOf(store.pin.isEmpty()) }
    var entry by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    val importPick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            onAddUrl(uri.toString())
        }
    }
    fun clearPositions() {
        val sp = ctx.getSharedPreferences("tv", Context.MODE_PRIVATE)
        val e = sp.edit()
        sp.all.keys.filter { it.startsWith("pos_") }.forEach { e.remove(it) }
        e.apply()
    }
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
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Button(onClick = onAdd) { Text("Add M3U link") }; OutlinedButton(onClick = onRefresh) { Text("Refresh") }; OutlinedButton(onClick = { importPick.launch(arrayOf("*/*")) }) { Text("Import M3U file") } } }
        item { Text("Playlist library", fontWeight = FontWeight.Bold, fontSize = 20.sp) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(library.map { it.section }.distinct()) { sec -> FilterChip(libSec == sec, { libSec = sec }, { Text(sec) }) }
            }
        }
        items(library.filter { it.section == libSec }) { e ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(e.name, Modifier.weight(1f))
                if (e.url in pls) Text("✓ Added", color = Color.LightGray)
                else Button(onClick = { onAddUrl(e.url) }) { Text("Add") }
            }
        }
        item { Text("Program guide (XMLTV link)", fontWeight = FontWeight.Bold, fontSize = 20.sp) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(epgText, { epgText = it }, singleLine = true, label = { Text("Guide URL (.xml or .xml.gz)") })
                Button(onClick = { store.epgUrl = epgText.trim(); onChange() }) { Text("Load guide") }
            }
        }
        item { Text("Privacy: clear history any time", fontWeight = FontWeight.Bold, fontSize = 20.sp) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { store.recents = emptyList(); store.clearCounts(); onChange(); msg = "Watch history cleared" }) { Text("Clear watch history") }
                OutlinedButton(onClick = { store.searches = emptyList(); onChange(); msg = "Search history cleared" }) { Text("Clear search history") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { clearPositions(); msg = "Resume positions cleared" }) { Text("Clear resume positions") }
                OutlinedButton(onClick = { store.recents = emptyList(); store.searches = emptyList(); store.clearCounts(); clearPositions(); onChange(); msg = "All history cleared" }) { Text("Clear everything") }
            }
        }
        item { Text("Streaming buffer", fontWeight = FontWeight.Bold, fontSize = 20.sp) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Low (fast start)", "Normal", "High (weak internet)").forEachIndexed { i, n ->
                    FilterChip(buf == i, { buf = i; store.buffer = i }, { Text(n) })
                }
            }
        }
        item { Text("Custom accent colour", fontWeight = FontWeight.Bold, fontSize = 20.sp) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Slider(value = store.accent.coerceAtLeast(0f), onValueChange = { store.accent = it }, modifier = Modifier.width(260.dp))
                Box(Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                OutlinedButton(onClick = { store.accent = -1f }) { Text("Reset") }
            }
        }
        item { Text("Backup", fontWeight = FontWeight.Bold, fontSize = 20.sp) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = {
                    val o = JSONObject()
                    o.put("pl", JSONArray(store.playlists)); o.put("fav", JSONArray(store.favs.toList()))
                    o.put("hid", JSONArray(store.hidden.toList())); o.put("hidch", JSONArray(store.hiddenCh.toList()))
                    o.put("theme", store.theme); o.put("city", store.city)
                    clip.setText(AnnotatedString(o.toString())); msg = "Backup copied. Paste it somewhere safe."
                }) { Text("Copy backup") }
                OutlinedButton(onClick = {
                    runCatching {
                        val o = JSONObject(clip.getText()?.text ?: "")
                        fun arr(k: String): List<String> = o.optJSONArray(k)?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
                        store.playlists = arr("pl"); store.favs = arr("fav").toSet()
                        store.hidden = arr("hid").toSet(); store.hiddenCh = arr("hidch").toSet()
                        store.city = o.optString("city", "")
                        onTheme(o.optInt("theme", 0).coerceIn(0, themes.size - 1)); onChange(); msg = "Backup restored"
                    }.onFailure { msg = "The clipboard does not hold a valid backup" }
                }) { Text("Restore from clipboard") }
            }
        }
        item { if (msg.isNotEmpty()) Text(msg, color = Color.LightGray) }
        item { Text("Personalize", fontWeight = FontWeight.Bold, fontSize = 20.sp) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { pick.launch(arrayOf("image/*")) }) { Text("Choose wallpaper") }
                OutlinedButton(onClick = { store.wall = ""; onChange() }) { Text("Remove wallpaper") }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(city, { city = it }, singleLine = true, label = { Text("Weather city") })
                Button(onClick = { store.city = city.trim(); onChange() }) { Text("Save") }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Auto-play last channel on launch", Modifier.weight(1f))
                Switch(auto, { auto = it; store.auto = it })
            }
        }
        item { OutlinedButton(onClick = { store.hidden = emptySet(); onChange() }) { Text("Unhide all apps (long-press an app to hide it)") } }
        item { OutlinedButton(onClick = { store.hiddenCh = emptySet(); onChange() }) { Text("Unhide all channels (long-press a channel to hide it)") } }
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
        Text("Version 1.2", color = Color.LightGray)
        Spacer(Modifier.height(12.dp))
        Text("Created by Bensal", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Device: ${Build.MANUFACTURER} ${Build.MODEL}", color = Color.LightGray)
        Text("Built with Kotlin, Jetpack Compose and Media3", color = Color.LightGray)
    }
}

@Composable
fun App(store: Store, ti: Int, onProfile: (Int) -> Unit, onTheme: (Int) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var channelsRaw by remember { mutableStateOf(listOf<Channel>()) }
    var hiddenCh by remember { mutableStateOf(store.hiddenCh) }
    var kids by remember { mutableStateOf(store.kids) }
    var minQ by remember { mutableStateOf(store.minQ) }
    var epgUrl by remember { mutableStateOf(store.epgUrl) }
    val channels = remember(channelsRaw, hiddenCh, kids, minQ) { channelsRaw.filter { it.url !in hiddenCh && (!kids || kidsWords.containsMatchIn(it.group)) && (minQ == 0 || quality(it) >= minQ) } }
    var apps by remember { mutableStateOf(listOf<AppItem>()) }
    var favs by remember { mutableStateOf(store.favs) }
    var pls by remember { mutableStateOf(store.playlists) }
    var recents by remember { mutableStateOf(store.recents) }
    var recent by remember { mutableStateOf(store.searches) }
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
    fun reload() { scope.launch { loading = true; channelsRaw = pls.flatMap { fetch(it) }.distinctBy { it.url }; loading = false } }
    val toggleFav: (Channel) -> Unit = { c ->
        favs = if (c.url in favs) favs - c.url else favs + c.url
        store.favs = favs
    }
    var hidden by remember { mutableStateOf(store.hidden) }
    var folders by remember { mutableStateOf(store.folders) }
    var menuApp by remember { mutableStateOf<AppItem?>(null) }
    var folderText by remember { mutableStateOf("") }
    var folderSel by remember { mutableStateOf<String?>(null) }
    var multi by remember { mutableStateOf<Channel?>(null) }
    var panel by remember { mutableStateOf(false) }
    var wall by remember { mutableStateOf(store.wall) }
    var city by remember { mutableStateOf(store.city) }
    var weather by remember { mutableStateOf("") }
    val visibleApps = remember(apps, hidden) { apps.filter { it.pkg !in hidden } }
    fun hide(pkg: String) { hidden = hidden + pkg; store.hidden = hidden }
    val onChange: () -> Unit = { hidden = store.hidden; wall = store.wall; city = store.city; hiddenCh = store.hiddenCh; kids = store.kids; minQ = store.minQ; recents = store.recents; recent = store.searches; epgUrl = store.epgUrl; favs = store.favs; if (store.playlists != pls) { pls = store.playlists; reload() } }
    var status by remember { mutableStateOf("") }
    fun checkLinks(list: List<Channel>) {
        scope.launch {
            val l = list.filter { it.url.startsWith("http") }.take(300)
            var done = 0
            val dead = mutableListOf<String>()
            l.chunked(20).forEach { chunk ->
                chunk.map { c -> async { c.url to alive(c.url) } }.awaitAll().forEach { if (!it.second) dead += it.first }
                done += chunk.size
                status = "Checked $done/${l.size}, dead so far ${dead.size}"
            }
            hiddenCh = hiddenCh + dead; store.hiddenCh = hiddenCh
            status = "Done: hid ${dead.size} dead channels out of ${l.size} checked"
        }
    }
    var lastTouch by remember { mutableStateOf(System.currentTimeMillis()) }
    var ambient by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (true) { delay(30 * 60_000L); reload() } }
    LaunchedEffect(Unit) { while (true) { delay(5000); if (playing == null && multi == null && System.currentTimeMillis() - lastTouch > 120_000) ambient = true } }
    LaunchedEffect(epgUrl, channelsRaw.size) {
        val u = epgUrl.ifBlank { Epg.hint ?: "" }
        if (u.isNotBlank()) { loadEpg(u); epgTick++ }
    }
    LaunchedEffect(city) { weather = if (city.isBlank()) "" else fetchWeather(city) }
    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.Default) { loadApps(ctx) }
        loading = true; channelsRaw = pls.flatMap { fetch(it) }.distinctBy { it.url }; loading = false
        if (store.auto) store.recents.firstOrNull()?.let { u -> channelsRaw.firstOrNull { it.url == u }?.let { playing = it } }
    }
    val cs = MaterialTheme.colorScheme
    var q by remember { mutableStateOf("") }
    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { q = it; tab = 3 }
    }
    val localPick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            playing = Channel(uri.lastPathSegment?.substringAfterLast('/') ?: "Local video", uri.toString(), "Local", null)
        }
    }

    CompositionLocalProvider(LocalHide provides { c -> hiddenCh = hiddenCh + c.url; store.hiddenCh = hiddenCh }) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(themes[ti].glow), cs.background))).pointerInput(Unit) {
        awaitPointerEventScope { while (true) { awaitPointerEvent(PointerEventPass.Initial); lastTouch = System.currentTimeMillis() } }
    }) {
        if (wall.isNotEmpty()) {
            AsyncImage(wall, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Color(0xAA000000)))
        }
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            Box(Modifier.fillMaxSize().padding(start = 88.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)) {
                when (tab) {
                    0 -> {
                        val byUrl = remember(channels) { channels.associateBy { it.url } }
                        val recentCh = remember(recents, byUrl) { recents.mapNotNull { byUrl[it] } }
                        val hero = recentCh.firstOrNull() ?: channels.firstOrNull()
                        val byGroup = remember(channels) { channels.groupBy { it.group }.toSortedMap() }
                        val fl = remember(channels, favs) { channels.filter { it.url in favs } }
                        val kidsList = remember(channels) { channels.filter { kidsWords.containsMatchIn(it.group) }.take(30) }
                        val topList = remember(channels, recents) { store.top(channels) }
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            item { Row(verticalAlignment = Alignment.CenterVertically) { Text(time, fontSize = 36.sp, fontWeight = FontWeight.Light); if (weather.isNotEmpty()) { Spacer(Modifier.width(24.dp)); Text("🌤 $weather", fontSize = 22.sp) }; Spacer(Modifier.width(24.dp)); TextButton(onClick = { panel = true }) { Text("⚡ Quick panel") } } }
                            item { Hero(hero, hero != null && hero.url in favs, { hero?.let { playing = it } }, { hero?.let { toggleFav(it) } }) }
                            if (recentCh.isNotEmpty()) item {
                                Shelf("Continue Watching") { items(recentCh) { c -> TvCard(c, c.url in favs, { toggleFav(c) }) { playing = c } } }
                            }
                            if (fl.isNotEmpty()) item {
                                Shelf("Favorites") { items(fl) { c -> TvCard(c, true, { toggleFav(c) }) { playing = c } } }
                            }
                            if (topList.isNotEmpty()) item { Shelf("Most Watched") { items(topList) { c -> TvCard(c, c.url in favs, { toggleFav(c) }) { playing = c } } } }
                            if (kidsList.isNotEmpty() && !kids) item { Shelf("Kids & Family") { items(kidsList) { c -> TvCard(c, c.url in favs, { toggleFav(c) }) { playing = c } } } }
                            val games = visibleApps.filter { it.isGame }
                            if (games.isNotEmpty()) item { Shelf("Games") { items(games) { AppTile(it) { menuApp = it } } } }
                            item { Shelf("Apps") { items(visibleApps) { AppTile(it) { menuApp = it } } } }
                            folders.values.distinct().sorted().forEach { fn ->
                                item { Shelf("📁 " + fn) { items(visibleApps.filter { folders[it.pkg] == fn }) { AppTile(it) { menuApp = it } } } }
                            }
                            byGroup.entries.take(15).forEach { (g, glist) ->
                                item { Shelf(g) { items(glist.take(30)) { c -> TvCard(c, c.url in favs, { toggleFav(c) }) { playing = c } } } }
                            }
                        }
                    }
                    1 -> Column {
                        var g by remember { mutableStateOf<String?>(null) }
                        var vod by remember { mutableStateOf(false) }
                        var az by remember { mutableStateOf(false) }
                        var guide by remember { mutableStateOf(false) }
                        val base = remember(channels, vod, az, minQ) { channels.filter { isVod(it) == vod }.let { l -> if (az) l.sortedBy { it.name.lowercase() } else if (minQ > 0) l.sortedByDescending { quality(it) } else l } }
                        val groups = remember(base) { base.map { it.group }.distinct().sorted() }
                        val shown = remember(base, g) { base.filter { g == null || it.group == g } }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                            FilterChip(!vod, { vod = false; g = null }, { Text("📺 Live TV") })
                            FilterChip(vod, { vod = true; g = null }, { Text("🎬 Movies & Series") })
                            FilterChip(az, { az = !az }, { Text("A–Z") })
                            FilterChip(minQ > 0, { val n = when (minQ) { 0 -> 720; 720 -> 1080; 1080 -> 2160; else -> 0 }; minQ = n; store.minQ = n }, { Text(when (minQ) { 0 -> "Quality: any"; 2160 -> "4K only"; else -> minQ.toString() + "p+" }) })
                            FilterChip(false, { localPick.launch(arrayOf("video/*", "audio/*")) }, { Text("📁 Local video") })
                            FilterChip(false, { checkLinks(shown) }, { Text("🧹 Check links") })
                            FilterChip(guide, { guide = !guide }, { Text("📋 Guide") })
                        }
                        if (status.isNotEmpty()) Text(status, fontSize = 12.sp, color = Color.LightGray)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                            item { FilterChip(g == null, { g = null }, { Text("All") }) }
                            items(groups) { x -> FilterChip(g == x, { g = x }, { Text(x) }) }
                        }
                        if (loading) Text("Loading channels…")
                        else if (channels.isEmpty()) Text("No channels yet. Add an M3U link in Settings.")
                        if (guide) {
                            val tick = epgTick
                            if (tick >= 0 && Epg.data.isEmpty()) Text("No guide data yet. Add an XMLTV guide link in Settings, or use a playlist that includes one.", color = Color.LightGray)
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(shown.take(300)) { c ->
                                    val (n, x) = nowNext(c)
                                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { playing = c }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(c.name, Modifier.width(220.dp), maxLines = 1, fontWeight = FontWeight.Bold)
                                        Column {
                                            Text(n?.let { "${HM.format(Date(it.start))}-${HM.format(Date(it.stop))}  ${it.title}" } ?: "No guide data", maxLines = 1)
                                            if (x != null) Text("Next ${HM.format(Date(x.start))}  ${x.title}", maxLines = 1, fontSize = 12.sp, color = Color.LightGray)
                                        }
                                    }
                                }
                            }
                        } else LazyVerticalGrid(GridCells.Adaptive(250.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(shown) { c -> TvCard(c, c.url in favs, { toggleFav(c) }) { playing = c } }
                        }
                    }
                    2 -> Column {
                        val fnames = remember(folders) { folders.values.distinct().sorted() }
                        if (fnames.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                            item { FilterChip(folderSel == null, { folderSel = null }, { Text("All") }) }
                            items(fnames) { n -> FilterChip(folderSel == n, { folderSel = n }, { Text("📁 " + n) }) }
                        }
                        LazyVerticalGrid(GridCells.Adaptive(110.dp)) {
                            items(visibleApps.filter { folderSel == null || folders[it.pkg] == folderSel }) { AppTile(it) { menuApp = it } }
                        }
                    }
                    3 -> Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(q, { q = it }, singleLine = true, label = { Text("Search channels and apps") }, modifier = Modifier.weight(1f))
                            TextButton(onClick = { runCatching { voice.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)) } }) { Text("🎤") }
                        }
                        if (q.isBlank() && recent.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                            items(recent) { r -> FilterChip(false, { q = r }, { Text(r) }) }
                        }
                        if (q.isNotBlank()) {
                            val ar = visibleApps.filter { it.label.contains(q, true) }
                            if (ar.isNotEmpty()) Shelf("Apps") { items(ar) { AppTile(it) { menuApp = it } } }
                            val cr = remember(q, channels) { channels.filter { it.name.contains(q, true) }.take(80) }
                            LazyVerticalGrid(GridCells.Adaptive(250.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(cr) { c -> TvCard(c, c.url in favs, { toggleFav(c) }) { if (q.isNotBlank()) { recent = (listOf(q.trim()) + recent).distinct().take(8); store.searches = recent }; playing = c } }
                            }
                        }
                    }
                    4 -> Settings(store, ti, onTheme, onProfile, pls, { showAdd = true },
                        { u -> pls = pls - u; store.playlists = pls; reload() }, { reload() }, onChange, { u -> pls = pls + u; store.playlists = pls; reload() })
                    else -> About()
                }
            }
            Sidebar(tab, expanded, { expanded = !expanded }) { tab = it }
        }
        if (ambient && playing == null) Box(Modifier.fillMaxSize().background(Color.Black).clickable { ambient = false; lastTouch = System.currentTimeMillis() }, contentAlignment = Alignment.Center) {
            Text(time, fontSize = 72.sp, fontWeight = FontWeight.Light)
        }
        multi?.let { m -> MultiView(channels, m) { multi = null } }
        playing?.let { p ->
            PlayerScreen(channels, p, onBack = { playing = null }, onMulti = { m -> multi = m; playing = null }) { c ->
                store.bump(c.url)
                recents = (listOf(c.url) + recents).distinct().take(15); store.recents = recents
            }
        }
    }
    }
    menuApp?.let { a ->
        AlertDialog(
            onDismissRequest = { menuApp = null },
            title = { Text(a.label) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(folderText, { folderText = it }, singleLine = true, label = { Text("Folder name") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            if (folderText.isNotBlank()) { folders = folders + (a.pkg to folderText.trim()); store.folders = folders }
                            menuApp = null; folderText = ""
                        }) { Text("Add to folder") }
                        if (a.pkg in folders) OutlinedButton(onClick = { folders = folders - a.pkg; store.folders = folders; menuApp = null }) { Text("Remove") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { hide(a.pkg); menuApp = null }) { Text("Hide app") } },
            dismissButton = { TextButton(onClick = { menuApp = null }) { Text("Cancel") } }
        )
    }
    if (panel) {
        val am = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager }
        val mx = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        var vol by remember { mutableStateOf(am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC).toFloat()) }
        var bri by remember { mutableStateOf(0.5f) }
        AlertDialog(
            onDismissRequest = { panel = false },
            title = { Text("Quick panel") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Volume")
                    Slider(vol, { vol = it; am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, it.toInt(), 0) }, valueRange = 0f..mx.toFloat())
                    Text("Brightness")
                    Slider(bri, { v -> bri = v; val w = (ctx as Activity).window; w.attributes = w.attributes.also { a -> a.screenBrightness = v.coerceAtLeast(0.02f) } })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        themes.forEachIndexed { i, th -> Box(Modifier.size(36.dp).clip(CircleShape).background(Color(th.accent)).clickable { onTheme(i) }) }
                    }
                    Button(onClick = { panel = false; ambient = true }) { Text("Screensaver now") }
                }
            },
            confirmButton = { TextButton(onClick = { panel = false }) { Text("Close") } }
        )
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
fun PlayerScreen(list: List<Channel>, start: Channel, onBack: () -> Unit, onMulti: (Channel) -> Unit, onPlay: (Channel) -> Unit) {
    val ctx = LocalContext.current
    val sp = remember { ctx.getSharedPreferences("tv", Context.MODE_PRIVATE) }
    var cur by remember { mutableStateOf(start) }
    var mi by remember { mutableStateOf(sp.getInt("aspect", 0).coerceIn(0, 5)) }
    var panel by remember { mutableStateOf(false) }
    var ui by remember { mutableStateOf(true) }
    var info by remember { mutableStateOf("") }
    var showInfo by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf("") }
    var sleepMin by remember { mutableStateOf(0) }
    var leftSide by remember { mutableStateOf(true) }
    var acc by remember { mutableStateOf(0f) }
    var locked by remember { mutableStateOf(false) }
    var endTick by remember { mutableStateOf(0) }
    var subIdx by remember { mutableStateOf(0) }
    var pv by remember { mutableStateOf<PlayerView?>(null) }
    var errTick by remember { mutableStateOf(0) }
    val tried = remember { mutableSetOf<String>() }
    var numDlg by remember { mutableStateOf(false) }
    var numTxt by remember { mutableStateOf("") }
    var boost by remember { mutableStateOf(0) }
    var enh by remember { mutableStateOf<LoudnessEnhancer?>(null) }
    val hist = remember { mutableStateListOf<Channel>() }
    val names = listOf("Fit", "Stretch", "Zoom", "16:9", "4:3", "21:9")
    val am = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager }
    val exo = remember {
        ExoPlayer.Builder(ctx, DefaultRenderersFactory(ctx).setEnableDecoderFallback(true)).setLoadControl(bufferControl(sp.getInt("buf", 1))).build().also { p ->
            p.trackSelectionParameters = p.trackSelectionParameters.buildUpon().setForceHighestSupportedBitrate(true).build()
            p.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_ENDED) endTick++ }
                override fun onPlayerError(error: PlaybackException) {
                    Handler(Looper.getMainLooper()).postDelayed({ errTick++ }, 3000)
                }
            })
        }
    }
    val subPick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val sub = MediaItem.SubtitleConfiguration.Builder(uri).setMimeType(MimeTypes.APPLICATION_SUBRIP)
                .setLanguage("und").setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build()
            val pos = exo.currentPosition
            exo.setMediaItem(MediaItem.Builder().setUri(cur.url).setSubtitleConfigurations(listOf(sub)).build())
            exo.seekTo(pos); exo.prepare(); exo.playWhenReady = true
            toast = "Subtitles loaded"
        }
    }
    DisposableEffect(Unit) {
        val w = (ctx as Activity).window
        val c = WindowCompat.getInsetsController(w, w.decorView)
        c.hide(WindowInsetsCompat.Type.systemBars())
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        w.decorView.keepScreenOn = true
        onDispose {
            runCatching { enh?.release() }
            exo.release()
            c.show(WindowInsetsCompat.Type.systemBars())
            w.decorView.keepScreenOn = false
        }
    }
    LaunchedEffect(cur) {
        exo.setMediaItem(MediaItem.fromUri(cur.url))
        if (isVod(cur)) exo.seekTo(sp.getLong("pos_" + cur.url, 0L))
        exo.prepare(); exo.playWhenReady = true; onPlay(cur)
        if (hist.lastOrNull()?.url != cur.url) hist.add(cur)
        while (true) {
            delay(5000)
            if (isVod(cur)) sp.edit().putLong("pos_" + cur.url, exo.currentPosition).apply()
        }
    }
    LaunchedEffect(sleepMin) { if (sleepMin > 0) { delay(sleepMin * 60_000L); onBack() } }
    LaunchedEffect(toast) { if (toast.isNotEmpty()) { delay(1500); toast = "" } }
    LaunchedEffect(showInfo) {
        while (showInfo) {
            val v = exo.videoSize
            info = "${v.width}x${v.height}  •  ${((exo.videoFormat?.bitrate ?: 0).coerceAtLeast(0)) / 1000} kbps"
            delay(1000)
        }
        info = ""
    }
    fun applyBoost(b: Int) {
        boost = b
        runCatching {
            if (enh == null) enh = LoudnessEnhancer(exo.audioSessionId)
            enh?.setTargetGain(b * 300)
            enh?.enabled = true
        }
        toast = if (b == 0) "Boost off" else "Volume boost +${b * 3} dB"
    }
    fun setAspect(i: Int) { mi = i; sp.edit().putInt("aspect", i).apply(); toast = "Aspect: ${names[i]}" }
    fun step(d: Int) {
        val pool = if (isVod(cur)) list.filter { it.group == cur.group } else list
        val i = pool.indexOfFirst { it.url == cur.url }
        if (i >= 0 && pool.isNotEmpty()) cur = pool[(i + d + pool.size) % pool.size]
    }
    LaunchedEffect(endTick) { if (endTick > 0 && isVod(cur)) step(1) }
    LaunchedEffect(errTick) {
        if (errTick > 0) {
            tried.add(cur.url)
            val alt = list.firstOrNull { it.name == cur.name && it.url !in tried }
            if (alt != null) { toast = "Trying backup stream"; cur = alt } else { tried.clear(); exo.prepare() }
        }
    }
    BackHandler { if (panel) panel = false else onBack() }
    Box(
        Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) {
            detectVerticalDragGestures(
                onDragStart = { o -> leftSide = o.x < size.width / 2; acc = 0f },
                onVerticalDrag = { _, dy ->
                    if (locked) return@detectVerticalDragGestures
                    if (leftSide) {
                        val w = (ctx as Activity).window
                        val b0 = if (w.attributes.screenBrightness < 0) 0.5f else w.attributes.screenBrightness
                        val nb = (b0 - dy / 900f).coerceIn(0.02f, 1f)
                        w.attributes = w.attributes.also { it.screenBrightness = nb }
                        toast = "Brightness ${(nb * 100).toInt()}%"
                    } else {
                        acc += -dy / 50f
                        val st = acc.toInt()
                        if (st != 0) {
                            acc -= st
                            val mx = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                            val v = (am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) + st).coerceIn(0, mx)
                            am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, v, 0)
                            toast = "Volume $v/$mx"
                        }
                    }
                }
            )
        }.pointerInput(Unit) {
            detectTapGestures(onDoubleTap = { o ->
                if (!locked) {
                    val d = if (o.x < size.width / 2) -10_000L else 10_000L
                    exo.seekTo((exo.currentPosition + d).coerceAtLeast(0L))
                    toast = if (d < 0) "⏪ 10s" else "10s ⏩"
                }
            })
        }
    ) {
        Box(
            Modifier.align(Alignment.Center).then(
                when (mi) {
                    3 -> Modifier.aspectRatio(16f / 9f)
                    4 -> Modifier.aspectRatio(4f / 3f)
                    5 -> Modifier.aspectRatio(21f / 9f)
                    else -> Modifier.fillMaxSize()
                }
            )
        ) {
            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        player = exo; setShowSubtitleButton(true); controllerShowTimeoutMs = 3000
                        setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { v -> ui = v == View.VISIBLE })
                    }.also { v -> pv = v }
                },
                update = {
                    it.resizeMode = when (mi) {
                        0 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        2 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        else -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        if (ui && !locked) {
            Column(Modifier.align(Alignment.TopStart).padding(16.dp)) {
                Text(cur.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                if (info.isNotEmpty()) Text(info, color = Color.LightGray, fontSize = 12.sp)
                if (epgTick >= 0) nowNext(cur).let { (n, x) -> if (n != null) Text("Now: ${n.title}" + (x?.let { "  •  Next: ${it.title}" } ?: ""), color = Color.LightGray, fontSize = 12.sp) }
            }
            Row(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                TextButton(onClick = { panel = !panel }) { Text("☰ Channels") }
                TextButton(onClick = { step(-1) }) { Text("◀") }
                TextButton(onClick = { step(1) }) { Text("▶") }
                TextButton(onClick = { onMulti(cur) }) { Text("⧉") }
                TextButton(onClick = {
                    subIdx = (subIdx + 1) % 3
                    pv?.subtitleView?.setFractionalTextSize(listOf(0.0533f, 0.075f, 0.1f)[subIdx])
                    toast = "Subtitle size " + (subIdx + 1)
                }) { Text("Aa") }
                TextButton(onClick = { numDlg = true }) { Text("#") }
                TextButton(onClick = { applyBoost((boost + 1) % 4) }) { Text(if (boost == 0) "Boost" else "+${boost * 3}dB") }
                TextButton(onClick = { if (hist.size >= 2) cur = hist[hist.size - 2] }) { Text("⇄") }
                TextButton(onClick = { subPick.launch(arrayOf("*/*")) }) { Text("CC+") }
                TextButton(onClick = { locked = true; toast = "🔒 Locked, long-press to unlock" }) { Text("🔒") }
                TextButton(onClick = { showInfo = !showInfo }) { Text("Info") }
                TextButton(onClick = { sleepMin = listOf(0, 15, 30, 60)[(listOf(0, 15, 30, 60).indexOf(sleepMin) + 1) % 4] }) {
                    Text(if (sleepMin == 0) "Sleep" else "Sleep ${sleepMin}m")
                }
                TextButton(onClick = { runCatching { (ctx as Activity).enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()) } }) { Text("PiP") }
                TextButton(onClick = onBack) { Text("✕") }
            }
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                names.forEachIndexed { i, n -> FilterChip(mi == i, { setAspect(i) }, { Text(n) }) }
            }
        }
        if (numDlg) AlertDialog(
            onDismissRequest = { numDlg = false },
            title = { Text("Go to channel number") },
            text = { OutlinedTextField(numTxt, { numTxt = it.filter { ch -> ch.isDigit() }.take(5) }, singleLine = true, label = { Text("1 to ${list.size}") }) },
            confirmButton = {
                TextButton(onClick = {
                    numTxt.toIntOrNull()?.let { n -> if (n in 1..list.size) cur = list[n - 1] }
                    numTxt = ""; numDlg = false
                }) { Text("Go") }
            }
        )
        if (locked) Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onLongPress = { locked = false; toast = "Unlocked" }) })
        if (toast.isNotEmpty()) Text(
            toast, Modifier.align(Alignment.Center).clip(RoundedCornerShape(12.dp)).background(Color(0xCC000000)).padding(horizontal = 20.dp, vertical = 10.dp),
            color = Color.White, fontSize = 20.sp
        )
        if (panel) LazyColumn(Modifier.align(Alignment.CenterEnd).width(300.dp).fillMaxHeight().background(Color(0xDD000000)).padding(8.dp)) {
            items(list) { c ->
                Text(c.name, Modifier.fillMaxWidth().clickable { cur = c; panel = false }.padding(10.dp), maxLines = 1,
                    color = if (c.url == cur.url) MaterialTheme.colorScheme.primary else Color.White)
            }
        }
    }
}

@Composable
fun MultiView(list: List<Channel>, first: Channel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var chA by remember { mutableStateOf(first) }
    var chB by remember { mutableStateOf(list.firstOrNull { it.url != first.url } ?: first) }
    var audio by remember { mutableStateOf(0) }
    var pick by remember { mutableStateOf(-1) }
    var qf by remember { mutableStateOf("") }
    val pa = remember { ExoPlayer.Builder(ctx).build() }
    val pb = remember { ExoPlayer.Builder(ctx).build() }
    DisposableEffect(Unit) { onDispose { pa.release(); pb.release() } }
    LaunchedEffect(chA) { pa.setMediaItem(MediaItem.fromUri(chA.url)); pa.prepare(); pa.playWhenReady = true }
    LaunchedEffect(chB) { pb.setMediaItem(MediaItem.fromUri(chB.url)); pb.prepare(); pb.playWhenReady = true }
    LaunchedEffect(audio) { pa.volume = if (audio == 0) 1f else 0f; pb.volume = if (audio == 1) 1f else 0f }
    BackHandler { if (pick >= 0) pick = -1 else onBack() }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Row(Modifier.fillMaxSize()) {
            for (i in 0..1) {
                val ch = if (i == 0) chA else chB
                Box(Modifier.weight(1f).fillMaxHeight().padding(2.dp).border(if (audio == i) 3.dp else 0.dp, MaterialTheme.colorScheme.primary)) {
                    AndroidView(
                        factory = { c -> PlayerView(c).apply { player = if (i == 0) pa else pb; useController = false } },
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(ch.name, Modifier.align(Alignment.TopStart).padding(8.dp), color = Color.White, maxLines = 1, fontSize = 14.sp)
                    Row(Modifier.align(Alignment.BottomCenter).padding(8.dp)) {
                        TextButton(onClick = { audio = i }) { Text(if (audio == i) "🔊 Audio" else "🔇 Audio") }
                        TextButton(onClick = { pick = i; qf = "" }) { Text("Change") }
                    }
                }
            }
        }
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.TopCenter)) { Text("✕ Close") }
        if (pick >= 0) {
            val shown = remember(qf, list) { list.filter { it.name.contains(qf, true) }.take(200) }
            Column(Modifier.align(Alignment.Center).width(420.dp).fillMaxHeight(0.8f).clip(RoundedCornerShape(16.dp)).background(Color(0xEE000000)).padding(12.dp)) {
                OutlinedTextField(qf, { qf = it }, singleLine = true, label = { Text("Search channels") }, modifier = Modifier.fillMaxWidth())
                LazyColumn {
                    items(shown) { c ->
                        Text(c.name, Modifier.fillMaxWidth().clickable { if (pick == 0) chA = c else chB = c; pick = -1 }.padding(10.dp), maxLines = 1, color = Color.White)
                    }
                }
            }
        }
    }
}
