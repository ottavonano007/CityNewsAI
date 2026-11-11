package it.citynewsai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
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
private fun isNecrologyArticle(article:BriefingArticle):Boolean {
 return article.necrology || listOf("necrologi","necrologie","necrologia")
  .any{Uri.decode(article.url).contains(it,ignoreCase=true)}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BriefingPage(province:Province,periodId:String,themeMode:String,palette:Int,onBack:()->Unit) {
 val context=LocalContext.current
 val scope=rememberCoroutineScope()
 val labels=mapOf("ultimora" to "Ultim'ora","24h" to "Ultime 24 ore","3d" to "Ultimi 3 giorni","7d" to "Ultima settimana","1m" to "Ultimo mese","1y" to "Ultimo anno")
 var articles by remember { mutableStateOf(emptyList<BriefingArticle>()) }
 var loading by remember { mutableStateOf(true) }
 var error by remember { mutableStateOf("") }
 var updatedAt by remember { mutableStateOf("") }
 var feedsRead by remember { mutableIntStateOf(0) }
 var keywordQuery by remember { mutableStateOf("") }
 val availableCategories=listOf("CRONACA E SICUREZZA","POLITICA E SERVIZI","ECONOMIA E LAVORO","AMBIENTE E METEO","CULTURA","SPORT","TERRITORIO","NECROLOGI")
 val defaultCategories=availableCategories.filter{it!="NECROLOGI"}.toSet()
 var selectedCategories by remember { mutableStateOf(defaultCategories) }
 var showCategoryFilter by remember { mutableStateOf(false) }
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
 val filteredArticles=remember(articles,keywordQuery,selectedCategories) {
  val terms=keywordQuery.trim().lowercase(Locale.ITALY).split(Regex("\\s+")).filter{it.isNotBlank()}
  articles.filter { article ->
   val displayCategory=if(article.necrology||article.category=="NECROLOGI"||isNecrologyArticle(article))"NECROLOGI" else article.category
   (displayCategory in selectedCategories) &&
    (terms.isEmpty() || run {
     val searchable=listOf(article.title,article.summary,article.source,displayCategory).joinToString(" ").lowercase(Locale.ITALY)
     terms.all { searchable.contains(it) }
    })
  }
 }
 val sections=filteredArticles.groupBy{if(it.necrology||it.category=="NECROLOGI"||isNecrologyArticle(it))"NECROLOGI" else it.category}
 val listState=rememberLazyListState()
 var showScrollThumb by remember { mutableStateOf(false) }
 var isThumbDragging by remember { mutableStateOf(false) }
 var showQuickFilters by remember { mutableStateOf(false) }
 LaunchedEffect(listState) {
  var previousIndex=0
  var previousOffset=0
  snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }.collect { (index,offset) ->
   val movingDown=index>previousIndex||(index==previousIndex&&offset>previousOffset)
   val movingUp=index<previousIndex||(index==previousIndex&&offset<previousOffset)
   if(index<=2) showQuickFilters=false
   else if(movingUp) showQuickFilters=true
   else if(movingDown) showQuickFilters=false
   previousIndex=index
   previousOffset=offset
  }
 }
 LaunchedEffect(listState.isScrollInProgress,isThumbDragging) {
  if(listState.isScrollInProgress || isThumbDragging) showScrollThumb=true else { delay(1400); showScrollThumb=false }
 }
 MaterialTheme(colorScheme=colors) {
 if(showCategoryFilter) AlertDialog(
  onDismissRequest={showCategoryFilter=false},
  title={Text("Filtra per argomento")},
  text={
   Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState())) {
    Text("Le sette categorie ordinarie sono attive inizialmente. Seleziona NECROLOGI per aggiungere gli annunci funebri; puoi anche deselezionare tutto e scegliere solo questa categoria.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(10.dp))
    availableCategories.forEach { category ->
     Row(Modifier.fillMaxWidth().clickable {
      selectedCategories=if(category in selectedCategories) selectedCategories-category else selectedCategories+category
     }.padding(vertical=3.dp),verticalAlignment=Alignment.CenterVertically) {
      Checkbox(checked=category in selectedCategories,onCheckedChange=null)
      Text(category,style=MaterialTheme.typography.labelLarge)
     }
    }
    HorizontalDivider(Modifier.padding(vertical=8.dp))
   }
  },
  confirmButton={Row(verticalAlignment=Alignment.CenterVertically){TextButton(onClick={selectedCategories=emptySet()}){Text("DESELEZIONA TUTTO")};TextButton(onClick={showCategoryFilter=false}){Text("APPLICA")}}},
  dismissButton={TextButton(onClick={selectedCategories=defaultCategories;showCategoryFilter=false}){Text("RIPRISTINA DEFAULT")}}
 )
 Scaffold(containerColor=MaterialTheme.colorScheme.background,topBar={TopAppBar(
  navigationIcon={IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,contentDescription="Torna alla selezione")}},
  title={Column{Text(province.name,fontWeight=FontWeight.Bold);Text("Rassegna intelligente • "+(labels[periodId]?:"Ultime 24 ore"),style=MaterialTheme.typography.labelSmall)}},
  actions={IconButton(onClick=refresh,enabled=!loading){Icon(Icons.Default.Refresh,contentDescription="Aggiorna rassegna")}}
 )}){padding->
  Box(Modifier.fillMaxSize().padding(padding)) {
  LazyColumn(state=listState,modifier=Modifier.fillMaxSize().padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(0.dp)){
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
   item {
    OutlinedButton(
     onClick={showCategoryFilter=true},
     modifier=Modifier.fillMaxWidth().padding(top=8.dp),
     colors=ButtonDefaults.outlinedButtonColors(contentColor=MaterialTheme.colorScheme.primary)
    ) {
     Icon(Icons.Default.FilterList,contentDescription=null)
     Spacer(Modifier.width(8.dp))
     Text("ARGOMENTI ATTIVI: "+selectedCategories.size+"/"+availableCategories.size+(if("NECROLOGI" in selectedCategories) " • NECROLOGI" else ""))
    }
   }
   if((keywordQuery.isNotBlank()||selectedCategories!=defaultCategories)&&!loading&&error.isBlank())item {
    Text(
     if(filteredArticles.isEmpty()) "Nessuna notizia corrisponde ai filtri selezionati." else "Risultati: ${filteredArticles.size} notizie",
     style=MaterialTheme.typography.labelMedium,
     color=MaterialTheme.colorScheme.onSurfaceVariant
    )
   }
   if(loading)item{Row(Modifier.fillMaxWidth().padding(vertical=24.dp),verticalAlignment=Alignment.CenterVertically){
    CircularProgressIndicator(Modifier.size(24.dp),strokeWidth=2.dp);Spacer(Modifier.width(14.dp))
    Column{Text("Sto raccogliendo le notizie…",fontWeight=FontWeight.SemiBold);Text("Consultazione delle fonti disponibili.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
   }}
   if(error.isNotBlank())item{
    Column(Modifier.fillMaxWidth().padding(vertical=22.dp)){
     Text("Rassegna non disponibile",fontWeight=FontWeight.Bold)
     Spacer(Modifier.height(6.dp));Text(error,color=MaterialTheme.colorScheme.onSurfaceVariant)
     TextButton(onClick=refresh,colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.primary)){Text("RIPROVA")}
    }
   }
   if(!loading&&error.isBlank()&&articles.isEmpty())item{
    Column(Modifier.fillMaxWidth().padding(vertical=22.dp)){
     Text("Nessuna notizia trovata nel periodo selezionato.",fontWeight=FontWeight.SemiBold)
     Spacer(Modifier.height(6.dp));Text("I feed disponibili potrebbero non essere aggiornati oppure non contenere notizie per questo intervallo. Prova ad ampliare il periodo.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
   }
   if(!loading&&error.isBlank()&&articles.isNotEmpty()&&filteredArticles.isEmpty())item {
    Column(Modifier.fillMaxWidth().padding(vertical=22.dp)) {
     Text("Nessun risultato",fontWeight=FontWeight.SemiBold)
     Text("Modifica gli argomenti oppure cancella la ricerca per visualizzare altre notizie.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
   }
   sections.forEach{(category,categoryArticles)->
    item(key="heading_"+category){
     Column(Modifier.fillMaxWidth().padding(top=20.dp,bottom=4.dp)){
      Text(category.uppercase(Locale.ITALY),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.ExtraBold)
      Spacer(Modifier.height(8.dp))
      HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant,thickness=1.dp)
     }
    }
    items(categoryArticles,key={it.url}){article->
     Column(Modifier.fillMaxWidth().clickable{runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(article.url)))}}.padding(vertical=18.dp)){
      Row(verticalAlignment=Alignment.CenterVertically){
       Text(article.source.uppercase(Locale.ITALY),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)
       if(article.corroborationCount>1)Text("  •  "+article.corroborationCount+" FONTI",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,fontWeight=FontWeight.Medium)
       Spacer(Modifier.weight(1f))
       Text(SimpleDateFormat("dd/MM/yyyy • HH:mm",Locale.ITALY).format(article.publishedAt),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Spacer(Modifier.height(9.dp))
      Text(article.title,style=MaterialTheme.typography.headlineSmall,fontFamily=FontFamily.Serif,fontWeight=FontWeight.Bold,lineHeight=MaterialTheme.typography.headlineSmall.lineHeight)
      if(article.summary.isNotBlank()){
       Spacer(Modifier.height(8.dp))
       Text(article.summary,style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurface,lineHeight=MaterialTheme.typography.bodyLarge.lineHeight)
      }
      Spacer(Modifier.height(10.dp))
      Text("LEGGI L'ARTICOLO  ›",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)
     }
     HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant,thickness=0.7.dp)
    }
   }
   if(articles.isNotEmpty())item{
    Text("Aggiornata: "+updatedAt+" • Feed con risultati: "+feedsRead,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    Text("Rassegna automatica basata su feed e fonti pubbliche. Le notizie non sono verificate da una redazione: apri gli articoli originali per controllare i dettagli.",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(20.dp))
   }
  }
  if(showQuickFilters && listState.firstVisibleItemIndex>2) {
   Column(
    Modifier.align(Alignment.TopCenter).fillMaxWidth()
     .background(MaterialTheme.colorScheme.background)
     .padding(start=16.dp,end=16.dp,top=2.dp,bottom=8.dp)
   ) {
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
    OutlinedButton(
     onClick={showCategoryFilter=true},
     modifier=Modifier.fillMaxWidth().padding(top=8.dp),
     colors=ButtonDefaults.outlinedButtonColors(contentColor=MaterialTheme.colorScheme.primary)
    ) {
     Icon(Icons.Default.FilterList,contentDescription=null)
     Spacer(Modifier.width(8.dp))
     Text("ARGOMENTI ATTIVI: "+selectedCategories.size+"/"+availableCategories.size+(if("NECROLOGI" in selectedCategories) " • NECROLOGI" else ""))
    }
   }
  }
  FloatingActionButton(
   onClick={scope.launch { listState.animateScrollToItem(0);showQuickFilters=false }},
   modifier=Modifier.align(Alignment.BottomEnd).padding(end=68.dp,bottom=20.dp).size(48.dp),
   containerColor=MaterialTheme.colorScheme.primary.copy(alpha=0.72f),
   contentColor=MaterialTheme.colorScheme.onPrimary
  ) {
   Icon(Icons.Default.ArrowUpward,contentDescription="Torna all'inizio della rassegna")
  }
  val totalItems=listState.layoutInfo.totalItemsCount
  val visibleItems=listState.layoutInfo.visibleItemsInfo.size
  if((showScrollThumb || isThumbDragging) && totalItems>visibleItems && totalItems>0) {
   Canvas(
    modifier=Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(56.dp)
     .pointerInput(totalItems) {
      var dragRemainder=0f
      detectDragGestures(
       onDragStart={ isThumbDragging=true; showScrollThumb=true },
       onDragEnd={ isThumbDragging=false; showScrollThumb=true },
       onDragCancel={ isThumbDragging=false; showScrollThumb=true }
      ) { change, dragAmount ->
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
    val thumbHeight=(size.height*visibleItems.toFloat()/totalItems.toFloat()).coerceAtLeast(128.dp.toPx()).coerceAtMost(size.height)
    val maxFirst=(totalItems-visibleItems).coerceAtLeast(1)
    val fraction=listState.firstVisibleItemIndex.coerceIn(0,maxFirst).toFloat()/maxFirst
    val top=(size.height-thumbHeight)*fraction
    drawRoundRect(
     color=Color.Black.copy(alpha=0.42f),
     topLeft=Offset(size.width-30.dp.toPx(),top),
     size=Size(24.dp.toPx(),thumbHeight),
     cornerRadius=androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
    )
   }
  }
  }
 }
 }
}
