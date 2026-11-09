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
    var selectedTerritory by remember { mutableStateOf("Piacenza") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val articles = demoArticles.filter { it.territory == selectedTerritory }

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
                var territoryMenuExpanded by remember { mutableStateOf(false) }
                Text(
                    "Seleziona il territorio",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF52636D)
                )
                Spacer(Modifier.height(6.dp))
                ExposedDropdownMenuBox(
                    expanded = territoryMenuExpanded,
                    onExpandedChange = { territoryMenuExpanded = !territoryMenuExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = if (selectedTerritory == "Piacenza") "Piacenza e provincia" else "Como e provincia",
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Apri elenco territori")
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = territoryMenuExpanded,
                        onDismissRequest = { territoryMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Piacenza e provincia") },
                            onClick = {
                                selectedTerritory = "Piacenza"
                                territoryMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Como e provincia") },
                            onClick = {
                                selectedTerritory = "Como"
                                territoryMenuExpanded = false
                            }
                        )
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
                    Text(selectedTerritory, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
