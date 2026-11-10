package it.citynewsai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class BriefingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val province = Province(
            intent.getStringExtra("province_name") ?: "Provincia",
            intent.getStringExtra("province_code") ?: "",
            intent.getStringExtra("province_region") ?: ""
        )
        val period = intent.getStringExtra("period_id") ?: "24h"
        setContent { BriefingPage(province, period, onBack = { finish() }) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BriefingPage(province: Province, periodId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val labels = mapOf(
        "24h" to "Ultime 24 ore",
        "3d" to "Ultimi 3 giorni",
        "7d" to "Ultima settimana",
        "1m" to "Ultimo mese",
        "6m" to "Ultimi 6 mesi",
        "1y" to "Ultimo anno"
    )
    var text by remember { mutableStateOf("") }
    var sources by remember { mutableStateOf(emptyList<BriefingSource>()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var updatedAt by remember { mutableStateOf("") }

    val refresh: () -> Unit = {
        scope.launch {
            loading = true
            error = ""
            text = ""
            sources = emptyList()
            try {
                val result = fetchAiBriefing(province, periodId)
                text = result.text
                sources = result.sources
                updatedAt = result.generatedAt
            } catch (e: Exception) {
                error = e.message ?: "Impossibile generare la rassegna."
            } finally {
                loading = false
            }
        }
    }
    LaunchedEffect(province.code, periodId) { refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Torna alla selezione")
                    }
                },
                title = {
                    Column {
                        Text(province.name, fontWeight = FontWeight.Bold)
                        Text("Rassegna AI • " + (labels[periodId] ?: labels.getValue("24h")),
                            style = MaterialTheme.typography.labelSmall)
                    }
                },
                actions = {
                    IconButton(onClick = refresh, enabled = !loading) {
                        Icon(Icons.Default.Refresh, contentDescription = "Aggiorna rassegna")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(2.dp)) }
            if (loading) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text("Sto preparando la rassegna…", fontWeight = FontWeight.SemiBold)
                                Text("Ricerca delle fonti e verifica delle notizie.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            if (error.isNotBlank()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Rassegna non disponibile", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(error)
                            TextButton(onClick = refresh) { Text("RIPROVA") }
                        }
                    }
                }
            }
            if (text.isNotBlank()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("LE NOTIZIE PRINCIPALI", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(10.dp))
                            Text(text, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                item {
                    Text("Fonti e approfondimenti", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(sources) { source ->
                    Card(Modifier.fillMaxWidth().clickable {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source.url))) }
                    }) {
                        Column(Modifier.padding(14.dp)) {
                            Text(source.title, fontWeight = FontWeight.SemiBold)
                            Text(source.url, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                item {
                    Text("Aggiornata: " + updatedAt.take(16).replace("T", " ") + " UTC",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Le fonti sono riportate per consentire la verifica. Consulta sempre gli articoli originali: una rassegna AI non può garantire l'assenza assoluta di errori.",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}
