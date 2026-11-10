package it.citynewsai

import android.net.Uri
import android.text.Html
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal data class BriefingArticle(val title:String,val summary:String,val source:String,val url:String,val publishedAt:Date,val category:String,val corroborationCount:Int=1)
internal data class BriefingResult(val articles:List<BriefingArticle>,val generatedAt:String,val feedsRead:Int)
private data class FeedSpec(val name:String,val url:String)

private val regionalNewsDomains = mapOf(
 "Piemonte" to listOf("quotidianopiemontese.it", "lastampa.it"),
 "Valle d'Aosta" to listOf("aostasera.it", "aostacronaca.it"),
 "Lombardia" to listOf("ilgiorno.it", "giornaledibrescia.it", "ecodibergamo.it", "varesenews.it", "cremonaoggi.it"),
 "Trentino-Alto Adige" to listOf("altoadige.it", "giornaletrentino.it"),
 "Veneto" to listOf("ilgazzettino.it", "larena.it", "mattinopadova.it", "ilgiornaledivicenza.it", "tribunatreviso.it", "nuovavenezia.it", "corrierealpi.it", "lavoce-nuova.it"),
 "Friuli-Venezia Giulia" to listOf("messaggeroveneto.it", "ilpiccolo.it", "pordenoneoggi.it"),
 "Liguria" to listOf("ilsecoloxix.it", "genova24.it", "ivg.it"),
 "Emilia-Romagna" to listOf("ilrestodelcarlino.it", "gazzettadiparma.it", "gazzettadimodena.it", "gazzettadireggio.it", "liberta.it", "ravennanotizie.it", "corriereromagna.it"),
 "Toscana" to listOf("lanazione.it", "gonews.it", "firenzetoday.it", "pisatoday.it", "arezzonotizie.it", "ilgiunco.net"),
 "Umbria" to listOf("tuttoggi.info", "umbriadomani.it", "perugiatoday.it"),
 "Marche" to listOf("corriereadriatico.it", "anconatoday.it", "vivereancona.it"),
 "Lazio" to listOf("ilmessaggero.it", "romatoday.it", "latinatoday.it", "viterbotoday.it"),
 "Abruzzo" to listOf("ilcentro.it", "chietitoday.it", "ilpescara.it", "cityrumors.it", "emmelle.it"),
 "Molise" to listOf("primonumero.it", "isnews.it", "termolionline.it"),
 "Campania" to listOf("ilmattino.it", "napolitoday.it", "salernotoday.it", "casertanews.it", "avellinotoday.it", "beneventonews24.it"),
 "Puglia" to listOf("lagazzettadelmezzogiorno.it", "quotidianodipuglia.it", "baritoday.it", "lecceprima.it"),
 "Basilicata" to listOf("lasiritide.it", "sassilive.it", "potenzanews.net"),
 "Calabria" to listOf("gazzettadelsud.it", "corrieredellacalabria.it", "catanzaroinforma.it", "cosenzachannel.it", "reggiotoday.it"),
 "Sicilia" to listOf("gds.it", "lasicilia.it", "palermotoday.it", "cataniaoggi.it"),
 "Sardegna" to listOf("unionesarda.it", "lanuovasardegna.it", "cagliaritoday.it", "sassarinotizie.com")
)

private fun googleNewsFeed(name:String, query:String):FeedSpec =
 FeedSpec(name,"https://news.google.com/rss/search?q="+Uri.encode(query)+"&hl=it&gl=IT&ceid=IT%3Ait")

private val provincialNewsDomains = mapOf(
 "AG" to listOf("agrigentonotizie.it", "grandangoloagrigento.it", "siciliaonpress.com", "teleacras.it"),
 "PC" to listOf("piacenzasera.it", "liberta.it"),
 "CO" to listOf("ciaocomo.it", "comozero.it", "primacomo.it")
)

private fun feedSpecs(p:Province, period:String):List<FeedSpec>{
 val calendar=java.util.Calendar.getInstance()
 val window=when(period){"24h"->" when:1d";"3d"->" when:3d";"7d"->" when:7d";"1m"->" when:1m";else->""}
 val months=when(period){"6m"->6;"1y"->12;else->0}
 if(months>0) calendar.add(java.util.Calendar.MONTH,-months)
 val after=if(months>0) " after:"+java.text.SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(calendar.time) else ""
 val cityTerms=when(p.code){
  "AG"->listOf("\\"Agrigento\\"","\"agrigentino\"","\"provincia di Agrigento\"")
  "PC"->listOf("\\"Piacenza\\"","\"piacentino\"","\"provincia di Piacenza\"")
  "CO"->listOf("\\"Como\\"","\"comasco\"","\"provincia di Como\"")
  else->listOf("\\""+p.name+"\\"","\"provincia di "+p.name+"\"")
 }
 val territory=cityTerms.joinToString(" OR ")
 val feeds=mutableListOf<FeedSpec>()
 feeds+=googleNewsFeed("Google News · "+p.name, "("+territory+")"+window+after)
 val domains=(provincialNewsDomains[p.code].orEmpty()+regionalNewsDomains[p.region].orEmpty()).distinct()
 if(domains.isNotEmpty()){
  val sites=domains.joinToString(" OR "){"site:"+it}
  feeds+=googleNewsFeed("Testate locali selezionate · "+p.name,"("+territory+") ("+sites+")"+window+after)
 }
 when(p.code){
  "PC"->feeds+=FeedSpec("PiacenzaSera","https://www.piacenzasera.it/feed/")
  "CO"->{
   feeds+=FeedSpec("CiaoComo","https://www.ciaocomo.it/feed/")
   feeds+=FeedSpec("ComoZero","https://comozero.it/feed/")
   feeds+=FeedSpec("Prima Como","https://primacomo.it/feed/")
  }
 }
 return feeds.distinctBy{it.url}
}
private fun parseDate(s:String):Date?{
 val patterns=listOf("EEE, dd MMM yyyy HH:mm:ss zzz","EEE, dd MMM yyyy HH:mm:ss Z","yyyy-MM-dd'T'HH:mm:ssXXX","yyyy-MM-dd'T'HH:mm:ss'Z'","yyyy-MM-dd")
 for(p in patterns)try{return SimpleDateFormat(p,Locale.US).apply{isLenient=false;if(p.endsWith("'Z'"))timeZone=TimeZone.getTimeZone("UTC")}.parse(s.trim())}catch(_:Exception){}
 return null
}
private fun readTag(x:XmlPullParser):String{
 val tag=x.name;val depth=x.depth;val out=StringBuilder()
 while(true) when(x.next()){
  XmlPullParser.TEXT,XmlPullParser.CDSECT->out.append(x.text)
  XmlPullParser.END_TAG->if(x.depth==depth&&x.name==tag) return out.toString().trim()
  XmlPullParser.END_DOCUMENT->return out.toString().trim()
 }
}
private fun clean(raw:String):String{
 val s=Html.fromHtml(raw,Html.FROM_HTML_MODE_LEGACY).toString().replace(Regex("\\s+")," ").trim()
 if(s.length<=280)return s
 val end=s.indexOfAny(charArrayOf('.','!','?'),160)
 return s.take(if(end in 160..280)end+1 else 277).trimEnd()+ "…"
}
private fun category(s:String):String{
 val t=s.lowercase(Locale.ITALY)
 return when{
  listOf("incidente","cronaca","polizia","carabinieri","furto","arrest","vigili del fuoco","sicurezza").any{it in t}->"CRONACA E SICUREZZA"
  listOf("comune","sindaco","consiglio","amministrazione","urbanistica","scuola","servizi","sanità","ospedale").any{it in t}->"POLITICA E SERVIZI"
  listOf("lavoro","azienda","imprese","economia","commercio","industria").any{it in t}->"ECONOMIA E LAVORO"
  listOf("meteo","temporale","allerta","maltempo","neve","pioggia").any{it in t}->"AMBIENTE E METEO"
  listOf("teatro","mostra","concerto","cultura","museo","festival","libro").any{it in t}->"CULTURA"
  listOf("calcio","sport","partita","basket","volley").any{it in t}->"SPORT"
  else->"TERRITORIO"
 }
}
private fun rank(c:String)=when(c){"CRONACA E SICUREZZA"->0;"POLITICA E SERVIZI"->1;"ECONOMIA E LAVORO"->2;"AMBIENTE E METEO"->3;"CULTURA"->4;"SPORT"->5;else->6}
private fun parseFeed(f:FeedSpec):List<BriefingArticle>{
 val c=URL(f.url).openConnection() as HttpURLConnection
 try{
  c.connectTimeout=9000;c.readTimeout=12000;c.instanceFollowRedirects=true
  c.setRequestProperty("User-Agent","Mozilla/5.0 (compatible; CityNewsAI/1.0)")
  if(c.responseCode !in 200..299)return emptyList()
  val x=Xml.newPullParser().apply{setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,false);setInput(c.inputStream,"UTF-8")}
  val out= mutableListOf<BriefingArticle>();var inside=false;var title="";var desc="";var link="";var date="";var source=f.name
  while(x.next()!=XmlPullParser.END_DOCUMENT){
   if(x.eventType==XmlPullParser.START_TAG){
    val tag=x.name.lowercase(Locale.ROOT)
    if(tag=="item"||tag=="entry"){inside=true;title="";desc="";link="";date="";source=f.name}
    else if(inside)when(tag){
     "title"->title=readTag(x)
     "description","summary","content"->if(desc.isBlank())desc=readTag(x)
     "link"->{val href=x.getAttributeValue(null,"href");if(!href.isNullOrBlank())link=href else {val v=readTag(x);if(v.startsWith("http"))link=v}}
     "pubdate","published","updated","date"->date=readTag(x)
     "source"->source=readTag(x).ifBlank{f.name}
    }
   }else if(x.eventType==XmlPullParser.END_TAG){
    val tag=x.name.lowercase(Locale.ROOT)
    if(inside&&(tag=="item"||tag=="entry")){
     val d=parseDate(date)
     if(title.isNotBlank()&&link.startsWith("http")&&d!=null){val summary=clean(desc);out+=BriefingArticle(clean(title).ifBlank{title},summary,source,link,d,category(title+" "+summary))}
     inside=false
    }
   }
  }
  return out
 }finally{c.disconnect()}
}
internal suspend fun fetchFreeBriefing(province:Province,periodId:String):BriefingResult=withContext(Dispatchers.IO){
 val feeds=feedSpecs(province,periodId);val all=mutableListOf<BriefingArticle>();var read=0
 feeds.forEach{try{val items=parseFeed(it);if(items.isNotEmpty())read++;all+=items}catch(_:Exception){}}
 val now=System.currentTimeMillis()
 val window=when(periodId){"24h"->86400000L;"3d"->259200000L;"7d"->604800000L;"1m"->2678400000L;"6m"->15811200000L;else->31622400000L}
 val filtered=all.filter{now-it.publishedAt.time in 0..window}
 val articles=filtered.groupBy{it.title.lowercase(Locale.ITALY).replace(Regex("[^a-z0-9]+")," ").trim()}.map{(_,copies)->
  val best=copies.maxByOrNull{it.summary.length}!!;best.copy(corroborationCount=copies.map{it.source}.distinct().size)
 }.sortedWith(compareBy<BriefingArticle>{rank(it.category)}.thenByDescending{it.publishedAt.time}).take(60)
 BriefingResult(articles,SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.ITALY).format(Date()),read)
}
