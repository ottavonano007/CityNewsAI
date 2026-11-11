package it.citynewsai

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.Date
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


internal data class Province(val name: String, val code: String, val region: String)

private val italianProvinces = listOf(
    Province("Agrigento", "AG", "Sicilia"),
    Province("Alessandria", "AL", "Piemonte"),
    Province("Ancona", "AN", "Marche"),
    Province("Arezzo", "AR", "Toscana"),
    Province("Ascoli Piceno", "AP", "Marche"),
    Province("Asti", "AT", "Piemonte"),
    Province("Avellino", "AV", "Campania"),
    Province("Bari", "BA", "Puglia"),
    Province("Barletta-Andria-Trani", "BT", "Puglia"),
    Province("Belluno", "BL", "Veneto"),
    Province("Benevento", "BN", "Campania"),
    Province("Bergamo", "BG", "Lombardia"),
    Province("Biella", "BI", "Piemonte"),
    Province("Bologna", "BO", "Emilia-Romagna"),
    Province("Bolzano", "BZ", "Trentino-Alto Adige"),
    Province("Brescia", "BS", "Lombardia"),
    Province("Brindisi", "BR", "Puglia"),
    Province("Cagliari", "CA", "Sardegna"),
    Province("Caltanissetta", "CL", "Sicilia"),
    Province("Campobasso", "CB", "Molise"),
    Province("Caserta", "CE", "Campania"),
    Province("Catania", "CT", "Sicilia"),
    Province("Catanzaro", "CZ", "Calabria"),
    Province("Chieti", "CH", "Abruzzo"),
    Province("Como", "CO", "Lombardia"),
    Province("Cosenza", "CS", "Calabria"),
    Province("Cremona", "CR", "Lombardia"),
    Province("Crotone", "KR", "Calabria"),
    Province("Cuneo", "CN", "Piemonte"),
    Province("Enna", "EN", "Sicilia"),
    Province("Fermo", "FM", "Marche"),
    Province("Ferrara", "FE", "Emilia-Romagna"),
    Province("Firenze", "FI", "Toscana"),
    Province("Foggia", "FG", "Puglia"),
    Province("Forlì-Cesena", "FC", "Emilia-Romagna"),
    Province("Frosinone", "FR", "Lazio"),
    Province("Gallura Nord-Est Sardegna", "OT", "Sardegna"),
    Province("Genova", "GE", "Liguria"),
    Province("Gorizia", "GO", "Friuli-Venezia Giulia"),
    Province("Grosseto", "GR", "Toscana"),
    Province("Imperia", "IM", "Liguria"),
    Province("Isernia", "IS", "Molise"),
    Province("L'Aquila", "AQ", "Abruzzo"),
    Province("La Spezia", "SP", "Liguria"),
    Province("Latina", "LT", "Lazio"),
    Province("Lecce", "LE", "Puglia"),
    Province("Lecco", "LC", "Lombardia"),
    Province("Livorno", "LI", "Toscana"),
    Province("Lodi", "LO", "Lombardia"),
    Province("Lucca", "LU", "Toscana"),
    Province("Macerata", "MC", "Marche"),
    Province("Mantova", "MN", "Lombardia"),
    Province("Massa-Carrara", "MS", "Toscana"),
    Province("Matera", "MT", "Basilicata"),
    Province("Medio Campidano", "VS", "Sardegna"),
    Province("Messina", "ME", "Sicilia"),
    Province("Milano", "MI", "Lombardia"),
    Province("Modena", "MO", "Emilia-Romagna"),
    Province("Monza e della Brianza", "MB", "Lombardia"),
    Province("Napoli", "NA", "Campania"),
    Province("Novara", "NO", "Piemonte"),
    Province("Nuoro", "NU", "Sardegna"),
    Province("Ogliastra", "OG", "Sardegna"),
    Province("Oristano", "OR", "Sardegna"),
    Province("Padova", "PD", "Veneto"),
    Province("Palermo", "PA", "Sicilia"),
    Province("Parma", "PR", "Emilia-Romagna"),
    Province("Pavia", "PV", "Lombardia"),
    Province("Perugia", "PG", "Umbria"),
    Province("Pesaro e Urbino", "PU", "Marche"),
    Province("Pescara", "PE", "Abruzzo"),
    Province("Piacenza", "PC", "Emilia-Romagna"),
    Province("Pisa", "PI", "Toscana"),
    Province("Pistoia", "PT", "Toscana"),
    Province("Pordenone", "PN", "Friuli-Venezia Giulia"),
    Province("Potenza", "PZ", "Basilicata"),
    Province("Prato", "PO", "Toscana"),
    Province("Ragusa", "RG", "Sicilia"),
    Province("Ravenna", "RA", "Emilia-Romagna"),
    Province("Reggio Calabria", "RC", "Calabria"),
    Province("Reggio nell'Emilia", "RE", "Emilia-Romagna"),
    Province("Rieti", "RI", "Lazio"),
    Province("Rimini", "RN", "Emilia-Romagna"),
    Province("Roma", "RM", "Lazio"),
    Province("Rovigo", "RO", "Veneto"),
    Province("Salerno", "SA", "Campania"),
    Province("Sassari", "SS", "Sardegna"),
    Province("Savona", "SV", "Liguria"),
    Province("Siena", "SI", "Toscana"),
    Province("Siracusa", "SR", "Sicilia"),
    Province("Sondrio", "SO", "Lombardia"),
    Province("Sulcis Iglesiente", "SU", "Sardegna"),
    Province("Taranto", "TA", "Puglia"),
    Province("Teramo", "TE", "Abruzzo"),
    Province("Terni", "TR", "Umbria"),
    Province("Torino", "TO", "Piemonte"),
    Province("Trapani", "TP", "Sicilia"),
    Province("Trento", "TN", "Trentino-Alto Adige"),
    Province("Treviso", "TV", "Veneto"),
    Province("Trieste", "TS", "Friuli-Venezia Giulia"),
    Province("Udine", "UD", "Friuli-Venezia Giulia"),
    Province("Valle d'Aosta", "AO", "Valle d'Aosta"),
    Province("Varese", "VA", "Lombardia"),
    Province("Venezia", "VE", "Veneto"),
    Province("Verbano-Cusio-Ossola", "VB", "Piemonte"),
    Province("Vercelli", "VC", "Piemonte"),
    Province("Verona", "VR", "Veneto"),
    Province("Vibo Valentia", "VV", "Calabria"),
    Province("Vicenza", "VI", "Veneto"),
    Province("Viterbo", "VT", "Lazio")
)

private data class NewsArticle(
    val title: String,
    val summary: String,
    val source: String,
    val url: String,
    val publishedAt: String,
    val territoryCodes: Set<String>
)


private fun parseNewsDate(value: String): Date? {
    if (value.isBlank()) return null
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXX",
        "yyyy-MM-dd'T'HH:mm:ssXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd"
    )
    for (pattern in patterns) {
        try {
            val parser = SimpleDateFormat(pattern, Locale.ROOT).apply {
                isLenient = false
                if (pattern.endsWith("'Z'")) timeZone = TimeZone.getTimeZone("UTC")
            }
            val parsed = parser.parse(value)
            if (parsed != null) return parsed
        } catch (_: Exception) { }
    }
    return null
}

private data class NewsPeriod(val id: String, val label: String, val amount: Int, val unit: Int)

private val newsPeriods = listOf(
    NewsPeriod("ultimora", "Ultim'ora", 0, java.util.Calendar.DATE),
    NewsPeriod("24h", "Ultime 24 ore", 24, java.util.Calendar.HOUR_OF_DAY),
    NewsPeriod("3d", "Ultimi 3 giorni", 3, java.util.Calendar.DATE),
    NewsPeriod("7d", "Ultima settimana", 7, java.util.Calendar.DATE),
    NewsPeriod("1m", "Ultimo mese", 1, java.util.Calendar.MONTH),
    NewsPeriod("1y", "Ultimo anno", 1, java.util.Calendar.YEAR)
)

private fun filterNewsByPeriod(articles: List<NewsArticle>, periodId: String): List<NewsArticle> {
    val period = newsPeriods.firstOrNull { it.id == periodId } ?: newsPeriods.first()
    val now = Date()
    val cutoffCalendar = java.util.Calendar.getInstance().apply {
        time = now
        if (period.id == "ultimora") {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        } else {
            add(period.unit, -period.amount)
        }
    }
    val cutoff = cutoffCalendar.time
    return articles.mapNotNull { article ->
        val date = parseNewsDate(article.publishedAt) ?: return@mapNotNull null
        if (date.before(cutoff) || date.after(now)) null else article to date
    }.sortedByDescending { it.second }.map { it.first }
}

private suspend fun fetchPublishedNews(): Pair<List<NewsArticle>, String> = withContext(Dispatchers.IO) {
    val connection = (URL("https://raw.githubusercontent.com/ottavonano007/CityNewsAI/main/docs/data/news.json").openConnection() as HttpURLConnection)
    try {
        connection.connectTimeout = 12000
        connection.readTimeout = 15000
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "CityNewsAI-Android")
        if (connection.responseCode !in 200..299) throw IllegalStateException("Servizio notizie non disponibile (${connection.responseCode})")
        val payload = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(payload)
        val list = root.optJSONArray("articles")
        val articles = buildList {
            if (list != null) for (i in 0 until list.length()) {
                val item = list.optJSONObject(i) ?: continue
                val url = item.optString("url")
                val title = item.optString("title")
                if (title.isBlank() || !url.startsWith("https://")) continue
                val codesJson = item.optJSONArray("territory_codes")
                val codes = buildSet {
                    if (codesJson != null) for (j in 0 until codesJson.length()) add(codesJson.optString(j).uppercase(Locale.ROOT))
                }
                add(NewsArticle(title, item.optString("summary"), item.optString("source_name"), url,
                    item.optString("published_at"), codes))
            }
        }
        articles to root.optString("generated_at")
    } finally {
        connection.disconnect()
    }
}

private data class DemoArticle(
    val territory: String,
    val category: String,
    val title: String,
    val summary: String,
    val source: String
)

private val demoArticles = listOf(
    DemoArticle("Piacenza", "TERRITORIO", "Uno spazio per raccontare la città", "Questa scheda mostra come apparirà una notizia locale: titolo, breve descrizione e indicazione della fonte.", "Contenuto di prova"),
    DemoArticle("Piacenza", "CULTURA", "In evidenza gli appuntamenti del territorio", "Esempio dimostrativo di una notizia culturale. Non si riferisce a un evento realmente verificato.", "Contenuto di prova"),
    DemoArticle("Como", "TERRITORIO", "Uno spazio per le notizie del Comasco", "Questa scheda è un esempio grafico della futura rassegna dedicata a Como e provincia.", "Contenuto di prova"),
    DemoArticle("Como", "CULTURA", "Le iniziative locali in primo piano", "Esempio dimostrativo: i contenuti reali saranno mostrati solo dopo la verifica delle fonti giornalistiche.", "Contenuto di prova")
)

private const val PREFS_NAME = "citynews_preferences"
private const val KEY_PREFERRED_PROVINCE = "preferred_province"
private const val KEY_STARTUP_MODE = "startup_mode"
private const val KEY_THEME = "theme_mode"
private const val KEY_COLOR_PALETTE = "color_palette"
private const val KEY_FAVORITE_PROVINCES = "favorite_provinces"


internal fun cityNewsColorScheme(dark: Boolean, index: Int): androidx.compose.material3.ColorScheme {
    // Le 15 palette mantengono variazioni di blu, ma rispettano sempre la gamma
    // neutra richiesta per la schermata principale.
    val bluePrimaries = listOf(
        0xFF176B9B, 0xFF1976A8, 0xFF246FA5, 0xFF287FB5, 0xFF12658F,
        0xFF3486B8, 0xFF1D6694, 0xFF398DBB, 0xFF205F8A, 0xFF2C78A8,
        0xFF155F91, 0xFF438FB9, 0xFF1C719F, 0xFF347BA6, 0xFF0E6C9F
    )
    val blueSecondaries = listOf(
        0xFF5A9FC5, 0xFF68A9CC, 0xFF649BC0, 0xFF75B2D2, 0xFF5794B8,
        0xFF80B5D3, 0xFF6099BD, 0xFF8ABBD5, 0xFF548BAF, 0xFF72A9C9,
        0xFF5795BE, 0xFF91BED8, 0xFF659FC3, 0xFF7AAAC9, 0xFF4D91B9
    )
    val lightBackgrounds = listOf(
        0xFFF7F9FB, 0xFFF3F6F8, 0xFFF5F8FA, 0xFFEFF5F9, 0xFFFAFBFC,
        0xFFF1F4F6, 0xFFF6F8FA, 0xFFEDF4F8, 0xFFF8FAFB, 0xFFF2F6F9,
        0xFFF4F5F7, 0xFFEFF3F6, 0xFFF7FAFC, 0xFFF0F5F8, 0xFFFAFCFD
    )
    val i = index.coerceIn(0, 14)
    val primary = Color(bluePrimaries[i])
    val secondary = Color(blueSecondaries[i])

    return if (dark) {
        val background = Color(0xFF171B1F)
        val surface = Color(0xFF20262B)
        val surfaceVariant = Color(0xFF30383F)
        val container = Color(0xFF29343C)
        darkColorScheme(
            primary = Color(blueSecondaries[i]),
            onPrimary = Color(0xFF071923),
            primaryContainer = container,
            onPrimaryContainer = Color(0xFFDCEEF8),
            secondary = primary,
            onSecondary = Color(0xFF071923),
            secondaryContainer = Color(0xFF303B43),
            onSecondaryContainer = Color(0xFFDCE8EF),
            tertiary = secondary,
            onTertiary = Color(0xFF071923),
            tertiaryContainer = Color(0xFF303B43),
            onTertiaryContainer = Color(0xFFDCE8EF),
            background = background,
            onBackground = Color(0xFFE7EAED),
            surface = surface,
            onSurface = Color(0xFFE7EAED),
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = Color(0xFFC0C8CE),
            outline = Color(0xFF82909A),
            outlineVariant = Color(0xFF4B565E),
            inverseSurface = Color(0xFFE7EAED),
            inverseOnSurface = Color(0xFF20262B),
            inversePrimary = Color(0xFF176B9B),
            surfaceTint = secondary
        )
    } else {
        val background = Color(lightBackgrounds[i])
        val surface = Color(0xFFFFFFFF)
        val surfaceVariant = Color(0xFFE9F0F5)
        val paleBlue = Color(0xFFDCECF6)
        lightColorScheme(
            primary = primary,
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = paleBlue,
            onPrimaryContainer = Color(0xFF123B55),
            secondary = secondary,
            onSecondary = Color(0xFF102F42),
            secondaryContainer = Color(0xFFE4EFF6),
            onSecondaryContainer = Color(0xFF183D55),
            tertiary = primary,
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFE4EFF6),
            onTertiaryContainer = Color(0xFF183D55),
            background = background,
            onBackground = Color(0xFF202A32),
            surface = surface,
            onSurface = Color(0xFF202A32),
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = Color(0xFF53636F),
            outline = Color(0xFF8799A5),
            outlineVariant = Color(0xFFC9D5DD),
            inverseSurface = Color(0xFF29343C),
            inverseOnSurface = Color(0xFFF3F6F8),
            inversePrimary = Color(0xFF9BC9E2),
            surfaceTint = primary
        )
    }
}

private suspend fun detectProvince(context: Context): Province? {
    val client = LocationServices.getFusedLocationProviderClient(context)
    val token = CancellationTokenSource()
    val location = suspendCancellableCoroutine<android.location.Location?> { continuation ->
        continuation.invokeOnCancellation { token.cancel() }
        client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, token.token)
            .addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
            .addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    } ?: suspendCancellableCoroutine { continuation ->
        client.lastLocation.addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
            .addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    }
    if (location == null || !Geocoder.isPresent()) return null
    val addresses = withContext(Dispatchers.IO) {
        @Suppress("DEPRECATION")
        Geocoder(context, Locale.ITALY).getFromLocation(location.latitude, location.longitude, 5)
    } ?: return null
    fun clean(value: String?): String = value.orEmpty().lowercase(Locale.ITALY)
        .replace("provincia di ", "").replace("città metropolitana di ", "")
        .replace("citta metropolitana di ", "").trim()
    val candidates = addresses.flatMap { listOf(it.subAdminArea, it.locality, it.subLocality, it.adminArea) }
        .filterNotNull().map(::clean)
    return italianProvinces.firstOrNull { province ->
        val name = clean(province.name)
        candidates.any { candidate ->
            candidate == name ||
                (province.code == "BZ" && candidate in listOf("bolzano", "bozen", "alto adige", "südtirol")) ||
                (province.code == "AO" && candidate in listOf("aosta", "valle d'aosta")) ||
                (province.code == "FC" && candidate in listOf("forli", "forli cesena", "forli-cesena")) ||
                (province.code == "RE" && candidate in listOf("reggio emilia", "reggio nell'emilia"))
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CityNewsApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CityNewsApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var selectedProvince by remember {
        mutableStateOf(italianProvinces.firstOrNull { it.code == prefs.getString(KEY_PREFERRED_PROVINCE, "PC") } ?: italianProvinces.first { it.code == "PC" })
    }
    var provinceQuery by remember { mutableStateOf("") }
    var territoryMenuExpanded by remember { mutableStateOf(false) }
    var periodMenuExpanded by remember { mutableStateOf(false) }
    var startupMode by remember { mutableStateOf(prefs.getString(KEY_STARTUP_MODE, "preferred") ?: "preferred") }
    var themeMode by remember { mutableStateOf(prefs.getString(KEY_THEME, "system") ?: "system") }
    var colorPalette by remember { mutableIntStateOf(prefs.getInt(KEY_COLOR_PALETTE, 0).coerceIn(0, 14)) }
    var showSettings by remember { mutableStateOf(false) }
    var locationBusy by remember { mutableStateOf(false) }
    var liveArticles by remember { mutableStateOf(emptyList<NewsArticle>()) }
    var newsUpdatedAt by remember { mutableStateOf("") }
    var newsLoading by remember { mutableStateOf(false) }
    var newsError by remember { mutableStateOf("") }
    var selectedNewsPeriod by remember { mutableStateOf("24h") }
    var favoriteCodes by remember { mutableStateOf(prefs.getStringSet(KEY_FAVORITE_PROVINCES, emptySet())?.toSet() ?: emptySet()) }
    var showFavorites by remember { mutableStateOf(false) }

    fun saveFavoriteCodes(codes: Set<String>) {
        favoriteCodes = codes.toSet()
        prefs.edit().putStringSet(KEY_FAVORITE_PROVINCES, favoriteCodes).apply()
    }

    val refreshNews: suspend () -> Unit = {
        newsLoading = true
        newsError = ""
        try {
            val result = fetchPublishedNews()
            liveArticles = result.first
            newsUpdatedAt = result.second
        } catch (error: Exception) {
            newsError = error.message ?: "Impossibile aggiornare le notizie."
        } finally {
            newsLoading = false
        }
    }


    val applyDetectedProvince: suspend () -> Unit = {
        locationBusy = true
        try {
            val detected = detectProvince(context)
            if (detected != null) {
                selectedProvince = detected
                prefs.edit().putString(KEY_PREFERRED_PROVINCE, detected.code).apply()
                snackbarHostState.showSnackbar("Provincia rilevata: ${detected.name}")
            } else snackbarHostState.showSnackbar("Non è stato possibile riconoscere la provincia dalla posizione.")
        } catch (_: SecurityException) {
            snackbarHostState.showSnackbar("Permesso di posizione non disponibile.")
        } catch (_: Exception) {
            snackbarHostState.showSnackbar("Posizione non disponibile. Verifica GPS e connessione.")
        } finally { locationBusy = false }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            startupMode = "location"
            prefs.edit().putString(KEY_STARTUP_MODE, "location").apply()
            scope.launch { applyDetectedProvince() }
        } else scope.launch { snackbarHostState.showSnackbar("Permesso negato: resterà attiva la provincia preferita.") }
    }

    LaunchedEffect(territoryMenuExpanded) {
        if (territoryMenuExpanded) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    LaunchedEffect(Unit) {
        if (startupMode == "location" &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) applyDetectedProvince()
    }

    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val colors = cityNewsColorScheme(isDark, colorPalette)

    MaterialTheme(colorScheme = colors) {
        if (showSettings) {
            SettingsPage(
                startupMode = startupMode, themeMode = themeMode, colorPalette = colorPalette,
                locationBusy = locationBusy, onBack = { showSettings = false },
                onStartupModeChange = { mode -> startupMode = mode; prefs.edit().putString(KEY_STARTUP_MODE, mode).apply() },
                onThemeChange = { mode -> themeMode = mode; prefs.edit().putString(KEY_THEME, mode).apply() },
                onPaletteChange = { index -> colorPalette = index; prefs.edit().putInt(KEY_COLOR_PALETTE, index).apply() },
                onDetectLocation = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        scope.launch { applyDetectedProvince() }
                    } else permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            )
        } else {
        val provinceArticles = liveArticles.filter { selectedProvince.code in it.territoryCodes }
        val articles = filterNewsByPeriod(provinceArticles, selectedNewsPeriod)
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("City News AI", fontWeight = FontWeight.Bold)
                            Text("Rassegna locale gratuita", style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Impostazioni")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                )
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !showFavorites, onClick = { showFavorites = false }, label = { Text("Province") })
                    FilterChip(
                        selected = showFavorites,
                        onClick = { showFavorites = true },
                        label = { Text("Preferiti (${favoriteCodes.size}/10)") },
                        leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null) }
                    )
                }
                if (showFavorites) {
                    Text("Le tue città preferite", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Puoi salvare fino a 10 province e riaprire rapidamente la relativa rassegna.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    if (favoriteCodes.isEmpty()) {
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Text("Non hai ancora salvato città. Torna a Province e usa il pulsante Aggiungi ai preferiti.", Modifier.padding(16.dp))
                        }
                    } else {
                        italianProvinces.filter { it.code in favoriteCodes }.forEach { favorite ->
                            Card(Modifier.fillMaxWidth().clickable {
                                selectedProvince = favorite
                                showFavorites = false
                            }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(favorite.name, fontWeight = FontWeight.SemiBold)
                                        Text("${favorite.region} • ${favorite.code}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    TextButton(
                            onClick = { saveFavoriteCodes(favoriteCodes - favorite.code) },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Rimuovi ${favorite.name}")
                                        Text(" Rimuovi")
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                } else {
                Text("Seleziona la provincia", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                ExposedDropdownMenuBox(
                    expanded = territoryMenuExpanded,
                    onExpandedChange = { expanded ->
                        if (expanded) {
                            territoryMenuExpanded = true
                            provinceQuery = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = if (territoryMenuExpanded) provinceQuery else selectedProvince.name,
                        onValueChange = { provinceQuery = it; territoryMenuExpanded = true },
                        placeholder = { Text("Digita provincia, sigla o regione") },
                        singleLine = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Apri ricerca province") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboardController?.show() }),
                        modifier = Modifier.menuAnchor().fillMaxWidth().focusRequester(focusRequester)
                    )
                    ExposedDropdownMenu(
                        expanded = territoryMenuExpanded,
                        onDismissRequest = { /* resta aperto fino alla selezione */ }
                    ) {
                        val filtered = italianProvinces.filter {
                            it.name.contains(provinceQuery, true) || it.code.contains(provinceQuery, true) || it.region.contains(provinceQuery, true)
                        }
                        val selectedIndex = filtered.indexOfFirst { it.code == selectedProvince.code }
                        val menuProvinces = if (provinceQuery.isBlank() && selectedIndex > 0) {
                            filtered.drop(selectedIndex) + filtered.take(selectedIndex)
                        } else filtered
                        if (menuProvinces.isEmpty()) {
                            DropdownMenuItem(text = { Text("Nessuna provincia trovata") }, enabled = false, onClick = {})
                        } else menuProvinces.forEach { province ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(province.name)
                                        Text("${province.region} • ${province.code}", style = MaterialTheme.typography.labelSmall)
                                    }
                                },
                                onClick = {
                                    selectedProvince = province
                                    prefs.edit().putString(KEY_PREFERRED_PROVINCE, province.code).apply()
                                    provinceQuery = ""
                                    territoryMenuExpanded = false
                                    keyboardController?.hide()
                                }
                            )
                        }
                    }
                }

                Text("Periodo della rassegna", style = MaterialTheme.typography.labelLarge)
                ExposedDropdownMenuBox(
                    expanded = periodMenuExpanded,
                    onExpandedChange = { periodMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newsPeriods.first { it.id == selectedNewsPeriod }.label,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Scegli il periodo") },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = periodMenuExpanded, onDismissRequest = { periodMenuExpanded = false }) {
                        newsPeriods.forEach { period ->
                            DropdownMenuItem(
                                text = { Text(period.label) },
                                onClick = { selectedNewsPeriod = period.id; periodMenuExpanded = false }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    onClick = {
                        if (selectedProvince.code in favoriteCodes) {
                            saveFavoriteCodes(favoriteCodes - selectedProvince.code)
                        } else if (favoriteCodes.size < 10) {
                            saveFavoriteCodes(favoriteCodes + selectedProvince.code)
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("Hai già raggiunto il limite di 10 città preferite. Rimuovine una per aggiungerne un'altra.") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(if (selectedProvince.code in favoriteCodes) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null)
                    Text(if (selectedProvince.code in favoriteCodes) "  RIMUOVI DAI PREFERITI" else "  AGGIUNGI AI PREFERITI")
                }
                Spacer(Modifier.height(8.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("Avvio dell'app", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = startupMode == "preferred",
                                onClick = {
                                    startupMode = "preferred"
                                    prefs.edit().putString(KEY_STARTUP_MODE, "preferred").putString(KEY_PREFERRED_PROVINCE, selectedProvince.code).apply()
                                },
                                label = { Text("Provincia preferita") }
                            )
                            FilterChip(
                                selected = startupMode == "location",
                                onClick = {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                        startupMode = "location"
                                        prefs.edit().putString(KEY_STARTUP_MODE, "location").apply()
                                        scope.launch { applyDetectedProvince() }
                                    } else permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                },
                                label = { Text("La mia posizione") },
                                leadingIcon = { Icon(Icons.Default.MyLocation, contentDescription = null) }
                            )
                        }
                        if (startupMode == "location") {
                            TextButton(
                                enabled = !locationBusy, onClick = { scope.launch { applyDetectedProvince() } },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.MyLocation, contentDescription = null)
                                Text(if (locationBusy) " Rilevamento…" else " Rileva nuovamente la posizione")
                            }
                        }

                    }
                }
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Rassegna automatica intelligente", fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Seleziona una provincia e il periodo desiderato, poi premi RASSEGNA per avviare la ricerca. Troverai brevi riepiloghi, categorie, date e collegamenti agli articoli originali.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            onClick = {
                                val briefingIntent = Intent(context, BriefingActivity::class.java).apply {
                                    putExtra("province_name", selectedProvince.name)
                                    putExtra("province_code", selectedProvince.code)
                                    putExtra("province_region", selectedProvince.region)
                                    putExtra("period_id", selectedNewsPeriod)
                                    putExtra("theme_mode", themeMode)
                                    putExtra("color_palette", colorPalette)
                                }
                                context.startActivity(briefingIntent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("RASSEGNA DI " + selectedProvince.name.uppercase(Locale.ITALY)) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                }
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsPage(
    startupMode: String, themeMode: String, colorPalette: Int, locationBusy: Boolean,
    onBack: () -> Unit, onStartupModeChange: (String) -> Unit, onThemeChange: (String) -> Unit,
    onPaletteChange: (Int) -> Unit, onDetectLocation: () -> Unit
) {
    var paletteMenuExpanded by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = scheme.primaryContainer,
        selectedLabelColor = scheme.onPrimaryContainer,
        selectedLeadingIconColor = scheme.primary,
        selectedTrailingIconColor = scheme.primary
    )
    Scaffold(
        containerColor = scheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = scheme.primary)
                    }
                },
                title = { Text("Impostazioni", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = scheme.surface,
                    titleContentColor = scheme.onSurface,
                    navigationIconContentColor = scheme.primary,
                    actionIconContentColor = scheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Aspetto", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                    Text("Tema dell'app", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = themeMode == "system", onClick = { onThemeChange("system") }, label = { Text("Sistema") }, colors = chipColors)
                        FilterChip(selected = themeMode == "light", onClick = { onThemeChange("light") }, label = { Text("Chiaro") }, colors = chipColors)
                        FilterChip(selected = themeMode == "dark", onClick = { onThemeChange("dark") }, label = { Text("Scuro") }, colors = chipColors)
                    }
                    Text("Combinazione colori: ${colorPalette + 1} di 15", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                    ExposedDropdownMenuBox(
                        expanded = paletteMenuExpanded,
                        onExpandedChange = { paletteMenuExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = "Palette ${colorPalette + 1} di 15",
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Scegli combinazione colori", tint = scheme.primary)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = scheme.primary,
                                unfocusedBorderColor = scheme.outline,
                                focusedLabelColor = scheme.primary,
                                cursorColor = scheme.primary
                            ),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = paletteMenuExpanded,
                            onDismissRequest = { paletteMenuExpanded = false }
                        ) {
                            (0..14).forEach { index ->
                                DropdownMenuItem(
                                    text = { Text("Palette ${index + 1}") },
                                    onClick = { onPaletteChange(index); paletteMenuExpanded = false },
                                    colors = MenuDefaults.itemColors(
                                        textColor = scheme.onSurface,
                                        leadingIconColor = scheme.primary,
                                        trailingIconColor = scheme.primary
                                    )
                                )
                            }
                        }
                    }
                    Text("Anteprima della palette attiva", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            Modifier.weight(1f).height(34.dp)
                                .background(scheme.background, RoundedCornerShape(8.dp))
                        )
                        Box(
                            Modifier.weight(1f).height(34.dp)
                                .background(scheme.surface, RoundedCornerShape(8.dp))
                                .then(Modifier.background(scheme.surface.copy(alpha = 0.01f), RoundedCornerShape(8.dp)))
                        )
                        Box(
                            Modifier.weight(1f).height(34.dp)
                                .background(scheme.primary, RoundedCornerShape(8.dp))
                        )
                        Box(
                            Modifier.weight(1f).height(34.dp)
                                .background(scheme.secondaryContainer, RoundedCornerShape(8.dp))
                        )
                    }
                }
            }
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Avvio dell'app", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = startupMode == "preferred",
                            onClick = { onStartupModeChange("preferred") },
                            label = { Text("Provincia preferita") },
                            colors = chipColors
                        )
                        FilterChip(
                            selected = startupMode == "location",
                            onClick = { onStartupModeChange("location"); onDetectLocation() },
                            label = { Text("La mia posizione") },
                            leadingIcon = { Icon(Icons.Default.MyLocation, contentDescription = null) },
                            colors = chipColors
                        )
                    }
                    if (startupMode == "location") TextButton(
                        enabled = !locationBusy,
                        onClick = onDetectLocation,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = scheme.primary,
                            disabledContentColor = scheme.onSurfaceVariant
                        )
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null)
                        Text(if (locationBusy) " Rilevamento…" else " Rileva nuovamente la posizione")
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsArticleCard(article: NewsArticle, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("NOTIZIE", color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.weight(1f))
                Text("LEGGI", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(10.dp))
            Text(article.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (article.summary.isNotBlank()) {
                Spacer(Modifier.height(7.dp))
                Text(article.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(article.source, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(article.publishedAt.take(10), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
