package it.citynewsai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale

class BriefingActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val province=Province(intent.getStringExtra("province_name")?:"Provincia",intent.getStringExtra("province_code")?:"",intent.getStringExtra("province_region")?:"")
  val period=intent.getStringExtra("period_id")?:"24h"
  val prefs=getSharedPreferences("citynews_preferences",MODE_PRIVATE)
  val themeMode=intent.getStringExtra("theme_mode")?:prefs.getString("theme_mode","system")?:"system"
  val palette=intent.getIntExtra("color_palette",prefs.getInt("color_palette",0))
  setContent { BriefingPage(province,period,themeMode,palette,onBack={finish()}) }
 }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BriefingPage(province:Province,periodId:String,themeMode:String,palette:Int,onBack:()->Unit) {
 val context=LocalContext.current
 val scope=rememberCoroutineScope()
 val labels=mapOf("24h" to "Ultime 24 ore","3d" to "Ultimi 3 giorni","7d" to "Ultima settimana","1m" to "Ultimo mese","6m" to "Ultimi 6 mesi","1y" to "Ultimo anno")
 var articles by remember { mutableStateOf(emptyList<BriefingArticle>()) }
 var loading by remember { mutableStateOf(true) }
 var error by remember { mutableStateOf("") }
 var updatedAt by remember { mutableStateOf("") }
 var feedsRead by remember { mutableIntStateOf(0) }
 var keywordQuery by remember { mutableStateOf("") }
 val refresh:()->Unit={
  scope.launch {
   loading=true;error=""
   try { val result=fetchFreeBriefing(province,periodId);articles=result.articles;updatedAt=result.generatedAt;feedsRead=result.feedsRead }
   catch(e:Exception){error=e.message?:"Impossibile aggiornare la rassegna."}
   finally{loading=false}
  }
 }
 LaunchedEffect(province.code,periodId){refresh()}
 val isDark=when(themeMode){"dark"->true;"light"->false;else->isSystemInDarkTheme()}
 val colors=cityNewsColorScheme(isDark,palette)
 val filteredArticles=remember(articles,keywordQuery) {
  val terms=keywordQuery.trim().lowercase(Locale.ITALY).split(Regex("\\s+")).filter{it.isNotBlank()}
  if(terms.isEmpty()) articles else articles.filter { article ->
   val searchable=listOf(article.title,article.summary,article.source,article.category).joinToString(" ").lowercase(Locale.ITALY)
   terms.all { searchable.contains(it) }
  }
 }
 val sections=filteredArticles.groupBy{it.category}
 val listState=rememberLazyListState()
 var showScrollThumb by remember { mutableStateOf(false) }
 LaunchedEffect(listState.isScrollInProgress) {
  if(listState.isScrollInProgress) showScrollThumb=true else { delay(900); showScrollThumb=false }
 }
 MaterialTheme(colorScheme=colors) {
 Scaffold(containerColor=MaterialTheme.colorScheme.background,topBar={TopAppBar(
  navigationIcon={IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,contentDescription="Torna alla selezione")}},
  title={Column{Text(province.name,fontWeight=FontWeight.Bold);Text("Rassegna intelligente • "+(labels[periodId]?:"Ultime 24 ore"),style=MaterialTheme.typography.labelSmall)}},
  actions={IconButton(onClick=refresh,enabled=!loading){Icon(Icons.Default.Refresh,contentDescription="Aggiorna rassegna")}}
 )}){padding->
  Box(Modifier.fillMaxSize().padding(padding)) {
  LazyColumn(state=listState,modifier=Modifier.fillMaxSize().padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(15.dp)){
   item{Spacer(Modifier.height(2.dp))}
   item {
    OutlinedTextField(
     value=keywordQuery,
     onValueChange={keywordQuery=it},
     modifier=Modifier.fillMaxWidth(),
     label={Text("CERCA PAROLE CHIAVE")},
     placeholder={Text("Es. viabilità, scuola, comune…")},
     singleLine=true,
     trailingIcon={
      if(keywordQuery.isNotEmpty()) IconButton(onClick={keywordQuery=""}) {
       Icon(Icons.Default.Close,contentDescription="Cancella ricerca")
      }
     }
    )
   }
   if(keywordQuery.isNotBlank()&&!loading&&error.isBlank())item {
    Text(
     if(filteredArticles.isEmpty()) "Nessuna notizia corrisponde alla ricerca." else "Risultati: ${filteredArticles.size} notizie",
     style=MaterialTheme.typography.labelMedium,
     color=MaterialTheme.colorScheme.onSurfaceVariant
    )
   }
   if(loading)item{Card(Modifier.fillMaxWidth()){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){
    CircularProgressIndicator(Modifier.size(26.dp),strokeWidth=3.dp);Spacer(Modifier.width(14.dp))
    Column{Text("Sto raccogliendo le notizie…",fontWeight=FontWeight.SemiBold);Text("Consultazione delle fonti disponibili.",style=MaterialTheme.typography.bodySmall)}
   }}}
   if(error.isNotBlank())item{Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.errorContainer),modifier=Modifier.fillMaxWidth()){
    Column(Modifier.padding(16.dp)){Text("Rassegna non disponibile",fontWeight=FontWeight.Bold);Spacer(Modifier.height(6.dp));Text(error);TextButton(onClick=refresh){Text("RIPROVA")}}
   }}
   if(!loading&&error.isBlank()&&articles.isEmpty())item{Card(Modifier.fillMaxWidth()){
    Column(Modifier.padding(16.dp)){Text("Nessuna notizia trovata nel periodo selezionato.",fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(6.dp));Text("I feed disponibili potrebbero non essere aggiornati oppure non contenere notizie per questo intervallo. Prova ad ampliare il periodo.",style=MaterialTheme.typography.bodySmall)}
   }}
   if(!loading&&error.isBlank()&&articles.isNotEmpty()&&filteredArticles.isEmpty())item {
    Card(Modifier.fillMaxWidth()) {
     Column(Modifier.padding(16.dp)) {
      Text("Nessun risultato",fontWeight=FontWeight.SemiBold)
      Text("Prova con un'altra parola chiave oppure cancella la ricerca.",style=MaterialTheme.typography.bodySmall)
     }
    }
   }
   sections.forEach{(category,categoryArticles)->
    item(key="heading_"+category){Text(category,style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.ExtraBold)}
    items(categoryArticles,key={it.url}){article->
     Card(Modifier.fillMaxWidth().clickable{runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(article.url)))}}){
      Column(Modifier.padding(17.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
       Text(article.title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
       if(article.summary.isNotBlank())Text(article.summary,style=MaterialTheme.typography.bodyLarge)
       Row(verticalAlignment=Alignment.CenterVertically){
        Text(article.source,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.SemiBold)
        if(article.corroborationCount>1)Text("  •  "+article.corroborationCount+" fonti",style=MaterialTheme.typography.labelSmall)
       }
       Text(SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.ITALY).format(article.publishedAt),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
       Text("APRI ARTICOLO  ›",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)
      }
     }
    }
   }
   if(articles.isNotEmpty())item{
    Text("Aggiornata: "+updatedAt+" • Feed con risultati: "+feedsRead,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    Text("Rassegna automatica basata su feed e fonti pubbliche. Le notizie non sono verificate da una redazione: apri gli articoli originali per controllare i dettagli.",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(20.dp))
   }
  }
  val totalItems=listState.layoutInfo.totalItemsCount
  val visibleItems=listState.layoutInfo.visibleItemsInfo.size
  if(showScrollThumb && totalItems>visibleItems && totalItems>0) {
   Canvas(
    modifier=Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(32.dp)
     .pointerInput(totalItems) {
      var dragRemainder=0f
      detectDragGestures { change, dragAmount ->
       change.consumeAllChanges()
       dragRemainder += dragAmount.y / size.height.coerceAtLeast(1).toFloat() * totalItems
       val itemDelta=dragRemainder.toInt()
       if(itemDelta!=0) {
        dragRemainder-=itemDelta
        val maxFirst=(totalItems-visibleItems).coerceAtLeast(0)
        val target=(listState.firstVisibleItemIndex+itemDelta).coerceIn(0,maxFirst)
        scope.launch { listState.scrollToItem(target) }
       }
      }
     }
   ) {
    val thumbHeight=(size.height*visibleItems.toFloat()/totalItems.toFloat()).coerceAtLeast(72.dp.toPx()).coerceAtMost(size.height)
    val maxFirst=(totalItems-visibleItems).coerceAtLeast(1)
    val fraction=listState.firstVisibleItemIndex.coerceIn(0,maxFirst).toFloat()/maxFirst
    val top=(size.height-thumbHeight)*fraction
    drawRoundRect(
     color=Color.Black.copy(alpha=0.42f),
     topLeft=Offset(size.width-20.dp.toPx(),top),
     size=Size(14.dp.toPx(),thumbHeight),
     cornerRadius=androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
    )
   }
  }
  }
 }
 }
}
