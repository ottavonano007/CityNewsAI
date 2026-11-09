package it.citynewsai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private data class Province(val name: String, val code: String, val region: String)

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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CityNewsApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CityNewsApp() {
    var selectedProvince by remember { mutableStateOf(italianProvinces.first { it.code == "PC" }) }
    var provinceQuery by remember { mutableStateOf("") }
    var territoryMenuExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val articles = demoArticles.filter { it.territory == selectedProvince.name }

    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = Color(0xFF174B68),
            secondary = Color(0xFF477D91),
            background = Color(0xFFF3F6F8),
            surface = Color.White
        )
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("CityNewsAI", fontWeight = FontWeight.Bold)
                            Text("Notizie locali, vicino a te", style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF17384A)
                    ),
                    actions = {
                        TextButton(onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Versione dimostrativa: le notizie live saranno collegate in una fase successiva.")
                            }
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Aggiorna")
                            Text(" Aggiorna")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Seleziona la provincia",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF52636D)
                )
                Spacer(Modifier.height(6.dp))
                ExposedDropdownMenuBox(
                    expanded = territoryMenuExpanded,
                    onExpandedChange = { expanded ->
                        territoryMenuExpanded = expanded
                        if (expanded) provinceQuery = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = if (territoryMenuExpanded) provinceQuery else selectedProvince.name,
                        onValueChange = { value ->
                            provinceQuery = value
                            territoryMenuExpanded = true
                        },
                        singleLine = true,
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Cerca e seleziona una provincia")
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = territoryMenuExpanded,
                        onDismissRequest = {
                            territoryMenuExpanded = false
                            provinceQuery = ""
                        }
                    ) {
                        val filteredProvinces = italianProvinces.filter { province ->
                            province.name.contains(provinceQuery, ignoreCase = true) ||
                                province.code.contains(provinceQuery, ignoreCase = true) ||
                                province.region.contains(provinceQuery, ignoreCase = true)
                        }
                        if (filteredProvinces.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Nessuna provincia trovata") },
                                enabled = false,
                                onClick = {}
                            )
                        } else {
                            filteredProvinces.forEach { province ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(province.name)
                                            Text(
                                                "${province.region} • ${province.code}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF71818A)
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedProvince = province
                                        provinceQuery = ""
                                        territoryMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Surface(
                    color = Color(0xFFFFF3D8),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("VERSIONE DIMOSTRATIVA  •  0.1.0", color = Color(0xFF79551A), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        Text("Le schede qui sotto sono esempi grafici, non notizie reali. Le fonti saranno attivate solo dopo le verifiche.")
                        }
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(selectedProvince.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("${articles.size} esempi", color = Color(0xFF647783), style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(10.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(articles) { article -> ArticleCard(article) }
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "CityNewsAI • Prima versione di prova",
                            color = Color(0xFF71818A),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleCard(article: DemoArticle) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.background(Color(0xFFE6F0F4), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(article.category, color = Color(0xFF24566D), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.weight(1f))
                Text("ESEMPIO", color = Color(0xFF9A6A1B), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(10.dp))
            Text(article.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF203744))
            Spacer(Modifier.height(7.dp))
            Text(article.summary, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF52636D))
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(article.source, style = MaterialTheme.typography.labelMedium, color = Color(0xFF174B68), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("Data non applicabile", style = MaterialTheme.typography.labelSmall, color = Color(0xFF7B898F))
            }
        }
    }
}
