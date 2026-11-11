package it.citynewsai

import android.net.Uri
import android.text.Html
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.text.Normalizer
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal data class BriefingArticle(val title:String,val summary:String,val source:String,val url:String,val publishedAt:Date,val category:String,val corroborationCount:Int=1,val localSource:Boolean=false,val necrology:Boolean=false)
internal data class BriefingResult(val articles:List<BriefingArticle>,val generatedAt:String,val feedsRead:Int)
private data class FeedSpec(val name:String,val url:String,val localSource:Boolean=false)

private val regionalNewsDomains = mapOf(
 "Piemonte" to listOf("lastampa.it","quotidianopiemontese.it","ilpiccolo.net","atnews.it","targatocn.it","cuneodice.it","gazzettadalba.it","lanuovaprovincia.it","ecodibiella.it","novaratoday.it","ossolanews.it","torinoggi.it","valsusaoggi.it","vercellioggi.it"),
 "Valle d'Aosta" to listOf("aostasera.it","aostanews24.it","aostaoggi.it","valledaostaglocal.it","courmayeurnews.it"),
 "Lombardia" to listOf("ilgiorno.it","ecodibergamo.it","bergamonews.it","giornaledibrescia.it","bresciaoggi.it","varesenews.it","prealpina.it","cremonaoggi.it","cremonasera.it","laprovinciadicomo.it","laprovinciadilecco.it","laprovinciadisondrio.it","laprovinciadivarese.it","laprovinciacr.it","ilcittadino.it","ilcittadinomb.it","mantovauno.it","mbnews.it","primapavia.it","sondriotoday.it"),
 "Trentino-Alto Adige" to listOf("altoadige.it","giornaletrentino.it","ildolomiti.it","ladige.it","salto.bz","suedtirolnews.it","trentotoday.it","questotrentino.it"),
 "Veneto" to listOf("ilgazzettino.it","corrierealpi.it","larena.it","mattinopadova.it","ilgiornaledivicenza.it","tribunatreviso.it","nuovavenezia.gelocal.it","polesine24.it","oggitreviso.it","padovanews.it","lapiazzaweb.it","bellunopress.it","altovicentinonline.it","newsinquota.it","veronasera.it"),
 "Friuli-Venezia Giulia" to listOf("messaggeroveneto.it","ilpiccolo.it","pordenoneoggi.it","pordenonetoday.it","triesteprima.it","udinetoday.it","ilgoriziano.it","udinews.it"),
 "Liguria" to listOf("ilsecoloxix.it","genova24.it","ivg.it","cittadellaspezia.com","riviera24.it","sanremonews.it","savonanews.it","levantenews.it","genova3000.it","laspeziaoggi.it"),
 "Emilia-Romagna" to listOf("ilrestodelcarlino.it","gazzettadiparma.it","gazzettadimodena.it","gazzettadireggio.it","liberta.it","ravennanotizie.it","corriereromagna.it","bologna2000.com","estense.com","modena2000.it","lapressa.it","reggionline.com","newsrimini.it","chiamamicitta.it","forli24ore.it","piacenzasera.it","ilpiacenza.it","ravenna24ore.it"),
 "Toscana" to listOf("lanazione.it","iltirreno.it","gonews.it","firenzetoday.it","pisatoday.it","arezzonotizie.it","ilgiunco.net","luccaindiretta.it","livornotoday.it","notiziediprato.it","sienanews.it","voceapuana.com","maremmanews.it","reportpistoia.com","quinewspisa.it","grossetonotizie.com"),
 "Umbria" to listOf("tuttoggi.info","umbriadomani.it","perugiatoday.it","ternitoday.it","umbria24.it","umbriajournal.com","iltamtam.it","orvietonews.it"),
 "Marche" to listOf("corriereadriatico.it","anconatoday.it","cronacheancona.it","cronachemaceratesi.it","cronachefermane.it","cronachepicene.it","centropagina.it","picchionews.it","vivereancona.it","youtvrs.it"),
 "Lazio" to listOf("ilmessaggero.it","romatoday.it","latinatoday.it","viterbotoday.it","frosinonetoday.it","ciociariaoggi.it","tusciaweb.eu","rietilife.com","rietinvetrina.it","h24notizie.com","newtuscia.it","ilfaroonline.it"),
 "Abruzzo" to listOf("ilcentro.it","chietitoday.it","ilpescara.it","cityrumors.it","emmelle.it","ilcapoluogo.it","abruzzoweb.it","marsicalive.it","zonalocale.it","certastampa.it","notiziedabruzzo.it","abruzzosera.it"),
 "Molise" to listOf("primonumero.it","isnews.it","termolionline.it","cblive.it","quotidianomolise.com","informamolise.com"),
 "Campania" to listOf("ilmattino.it","napolitoday.it","salernotoday.it","casertanews.it","avellinotoday.it","beneventonews24.it","anteprima24.it","orticalab.it","irpinianews.it","internapoli.it","salernonotizie.it","metropolisweb.it","cronachedellacampania.it","pupia.tv"),
 "Puglia" to listOf("lagazzettadelmezzogiorno.it","quotidianodipuglia.it","baritoday.it","lecceprima.it","brindisireport.it","foggiatoday.it","corrieresalentino.it","leccenews24.it","statoquotidiano.it","limmediato.net","tarantotoday.it","andrialive.it","barlettaviva.it","norbaonline.it"),
 "Basilicata" to listOf("lasiritide.it","sassilive.it","potenzanews.net","basilicata24.it","trmtv.it","materanews.net"),
 "Calabria" to listOf("gazzettadelsud.it","corrieredellacalabria.it","catanzaroinforma.it","cosenzachannel.it","reggiotoday.it","lacnews24.it","ilvibonese.it","zoom24.it","quicosenza.it","ilcrotonese.it","ilreggino.it","citynow.it"),
 "Sicilia" to listOf("gds.it","lasicilia.it","palermotoday.it","cataniaoggi.it","agrigentonotizie.it","tp24.it","messinatoday.it","siracusanews.it","ragusanews.com","livesicilia.it","blogsicilia.it","meridionews.it","ilsicilia.it","qds.it"),
 "Sardegna" to listOf("unionesarda.it","lanuovasardegna.it","cagliaritoday.it","sassarinotizie.com","olbianova.it","linkoristano.it","oristanonoi.it","vistanet.it","sardiniapost.it","castedduonline.it")
)

private fun googleNewsFeed(name:String, query:String):FeedSpec =
 FeedSpec(name,"https://news.google.com/rss/search?q="+Uri.encode(query)+"&hl=it&gl=IT&ceid=IT%3Ait")

// Fonti di prossimità: testate locali e portali territoriali con una redazione identificabile.
private val provincialNewsDomains = mapOf(
 "AG" to listOf("agrigentonotizie.it","grandangoloagrigento.it","teleacras.it"),
 "AL" to listOf("alessandrianews.it","corriereal.info","radiogold.it"),
 "AN" to listOf("anconatoday.it","cronacheancona.it","anconanotizie.it"),
 "AR" to listOf("arezzonotizie.it","arezzoweb.it"),
 "AP" to listOf("picenooggi.it","cronachepicene.it","ascolinotizie.it"),
 "AT" to listOf("atnews.it","gazzettadasti.it","lanuovaprovincia.it"),
 "AV" to listOf("avellinotoday.it","orticalab.it","irpinianews.it"),
 "BA" to listOf("baritoday.it","barilive.it","quintopotere.it"),
 "BT" to listOf("andrialive.it","barlettaviva.it","tranilive.it"),
 "BL" to listOf("bellunopress.it","corrierealpi.it","newsinquota.it"),
 "BN" to listOf("beneventonews24.it","ilsannioquotidiano.it","infosannio.com"),
 "BG" to listOf("bergamonews.it","primabergamo.it","ecodibergamo.it"),
 "BI" to listOf("ecodibiella.it","primabiella.it","laprovinciadibiella.it"),
 "BO" to listOf("bolognatoday.it","bologna2000.com","bologna24ore.it"),
 "BZ" to listOf("altoadige.it","salto.bz","suedtirolnews.it"),
 "BS" to listOf("bresciaoggi.it","giornaledibrescia.it","primabrescia.it"),
 "BR" to listOf("brindisireport.it","brindisilibera.it","brindisisera.it"),
 "CA" to listOf("cagliaritoday.it","castedduonline.it","cagliarioggi.it"),
 "CL" to listOf("ilfattonisseno.it","vivienna.it","ilsicilia.it"),
 "CB" to listOf("primonumero.it","cblive.it","termolionline.it"),
 "CE" to listOf("casertanews.it","casertace.net","pupia.tv"),
 "CT" to listOf("cataniaoggi.it","cataniatoday.it","meridionews.it"),
 "CZ" to listOf("catanzaroinforma.it","catanzarochannel.it"),
 "CH" to listOf("chietitoday.it","zonalocale.it","histonium.net"),
 "CO" to listOf("ciaocomo.it","comozero.it","primacomo.it"),
 "CS" to listOf("cosenzachannel.it","quicosenza.it","cosenzaok.it"),
 "CR" to listOf("cremonaoggi.it","primacremona.it","laprovinciacr.it"),
 "KR" to listOf("ilcrotonese.it","crotonenews.com","crotonenews.com"),
 "CN" to listOf("targatocn.it","cuneodice.it","laguida.it"),
 "EN" to listOf("ennalive.it","ilfattonisseno.it","vivienna.it"),
 "FM" to listOf("cronachefermane.it","fermonotizie.info","laprovinciadifermo.com"),
 "FE" to listOf("estense.com","ferraratoday.it","lanuovaferrara.it"),
 "FI" to listOf("firenzetoday.it","gonews.it","nove.firenze.it"),
 "FG" to listOf("foggiatoday.it","foggiasera.it","limmediato.net"),
 "FC" to listOf("forlitoday.it","forli24ore.it","forlinotizie.it"),
 "FR" to listOf("frosinonetoday.it","tunews24.it","ciociariaoggi.it"),
 "OT" to listOf("olbianova.it","olbia24.it","galluraoggi.it"),
 "GE" to listOf("genova24.it","genova3000.it","genovadice.it"),
 "GO" to listOf("ilgoriziano.it","gorizianews.com","messaggeroveneto.it"),
 "GR" to listOf("ilgiunco.net","grossetonotizie.com","maremmanews.it"),
 "IM" to listOf("riviera24.it","sanremonews.it","lavocediimperia.it"),
 "IS" to listOf("isnews.it","primonumero.it","altomolise.net"),
 "AQ" to listOf("ilcapoluogo.it","marsicalive.it"),
 "SP" to listOf("cittadellaspezia.com","laspeziaoggi.it","levantenews.it"),
 "LT" to listOf("latinatoday.it","h24notizie.com","latinaoggi.eu"),
 "LE" to listOf("lecceprima.it","corrieresalentino.it","leccenews24.it"),
 "LC" to listOf("lecconotizie.com","leccotoday.it","primalecco.it"),
 "LI" to listOf("livornotoday.it","quilivorno.it"),
 "LO" to listOf("ilcittadino.it","primalodi.it","lodinews.it"),
 "LU" to listOf("luccaindiretta.it","lagazzettadilucca.it"),
 "MC" to listOf("cronachemaceratesi.it","picchionews.it","maceratanotizie.it"),
 "MN" to listOf("gazzettadimantova.it","mantovauno.it","primamantova.it"),
 "MS" to listOf("voceapuana.com","gazzettadimassaecarrara.it"),
 "MT" to listOf("materanews.net","sassilive.it","materalife.it"),
 "VS" to listOf("sulcisiglesienteoggi.com","viverecarbonia.it"),
 "ME" to listOf("messinatoday.it","tempostretto.it","letteraemme.it"),
 "MI" to listOf("milanotoday.it","milanopost.info"),
 "MO" to listOf("modenatoday.it","lapressa.it","sassuolo2000.it"),
 "MB" to listOf("monzatoday.it","primamonza.it","mbnews.it"),
 "NA" to listOf("napolitoday.it","internapoli.it","metropolisweb.it"),
 "NO" to listOf("novaratoday.it","newsnovara.it","corrieredinovara.it"),
 "NU" to listOf("nuoronews.it","nuorooggi.it"),
 "OG" to listOf("vistanet.it","ogliastraweb.it"),
 "OR" to listOf("linkoristano.it","oristanonoi.it"),
 "PD" to listOf("padovaoggi.it","lapiazzaweb.it","padovanews.it"),
 "PA" to listOf("palermotoday.it","livesicilia.it","blogsicilia.it"),
 "PR" to listOf("parmatoday.it","gazzettadiparma.it","lungoparma.com"),
 "PV" to listOf("laprovinciapavese.gelocal.it","primapavia.it","ecodipavia.it"),
 "PG" to listOf("perugiatoday.it","tuttoggi.info","iltamtam.it"),
 "PU" to listOf("fanoinforma.it","viverepesaro.it","pesarourbinonotizie.it"),
 "PE" to listOf("ilpescara.it","cityrumors.it","pescaranews.net"),
 "PC" to listOf("liberta.it","piacenzasera.it","ilpiacenza.it"),
 "PI" to listOf("pisatoday.it","pisanews.net","quinewspisa.it"),
 "PT" to listOf("reportpistoia.com","pistoiasport.com"),
 "PN" to listOf("pordenonetoday.it","pordenoneoggi.it"),
 "PZ" to listOf("potenzanews.net","lasiritide.it","basilicata24.it"),
 "PO" to listOf("notiziediprato.it","pratotoday.it","lanazione.it"),
 "RG" to listOf("ragusanews.com","corrierediragusa.it","ragusah24.it"),
 "RA" to listOf("ravennatoday.it","ravennanotizie.it","ravenna24ore.it"),
 "RC" to listOf("citynow.it","reggiotoday.it","ilreggino.it"),
 "RE" to listOf("reggionline.com","primareggioemilia.it","reggio2000.it"),
 "RI" to listOf("rietilife.com","rietinvetrina.it","ilterritorio.net"),
 "RN" to listOf("riminitoday.it","newsrimini.it","chiamamicitta.it"),
 "RM" to listOf("romatoday.it","romadailynews.it"),
 "RO" to listOf("polesine24.it","rovigooggi.it","rovigo.news"),
 "SA" to listOf("salernotoday.it","salernonotizie.it","lacittadisalerno.it"),
 "SS" to listOf("sassaritoday.it","sassarinotizie.com"),
 "SV" to listOf("ivg.it","savonanews.it"),
 "SI" to listOf("sienanews.it","ilcittadinoonline.it","sienafree.it"),
 "SR" to listOf("siracusanews.it","webmarte.tv","siracusaoggi.it"),
 "SO" to listOf("sondriotoday.it","laprovinciadisondrio.it","valtellinanews.com"),
 "SU" to listOf("sulcisiglesienteoggi.com","viverecarbonia.it","sardegnaoggi.it"),
 "TA" to listOf("tarantotoday.it","blunote.it","tarantosera.it"),
 "TE" to listOf("certastampa.it","emmelle.it","ilmartino.it"),
 "TR" to listOf("ternitoday.it","umbriaon.it","umbria24.it"),
 "TO" to listOf("torinotoday.it","torinoggi.it","torinocronaca.it"),
 "TP" to listOf("tp24.it","trapanisi.it","marsalace.it"),
 "TN" to listOf("trentotoday.it","ildolomiti.it","giornaletrentino.it"),
 "TV" to listOf("trevisotoday.it","oggitreviso.it","qdpnews.it"),
 "TS" to listOf("triesteprima.it","trieste.news","ilpiccolo.it"),
 "UD" to listOf("udinetoday.it","udinews.it"),
 "AO" to listOf("aostasera.it","aostanews24.it","aostaoggi.it"),
 "VA" to listOf("varesenews.it","malpensanews.it","prealpina.it"),
 "VE" to listOf("veneziatoday.it","nuovavenezia.gelocal.it"),
 "VB" to listOf("verbanonews.it","ossolanews.it","vconews.it"),
 "VC" to listOf("vercellioggi.it","infovercelli24.it","primavercelli.it"),
 "VR" to listOf("veronasera.it","veronatoday.it","larena.it"),
 "VV" to listOf("ilvibonese.it","zoom24.it"),
 "VI" to listOf("vicenzatoday.it","primavicenza.it","altovicentinonline.it"),
 "VT" to listOf("viterbotoday.it","tusciaweb.eu","newtuscia.it")
)

private val provincialSearchAliases = mapOf(
 "AG" to listOf("agrigentino","agrigentina","Agrigento"),
 "AP" to listOf("ascolano","ascolana","piceno","Ascoli"),
 "BT" to listOf("Barletta","Andria","Trani","BAT"),
 "BZ" to listOf("Bolzano/Bozen","Alto Adige","Provincia autonoma di Bolzano","Bolzano","Bozen","Sudtirolo"),
 "CO" to listOf("comasco","comasca","comaschi"),
 "FI" to listOf("Firenze","fiorentino","fiorentina"),
 "FC" to listOf("Forli-Cesena","Forli","Cesena","forlivese","cesenate"),
 "OT" to listOf("Gallura","Olbia","Tempio","Olbia-Tempio"),
 "GO" to listOf("Goriziano","Gorizia"),
 "AQ" to listOf("Aquila","aquilano","aquilana","Marsica"),
 "LC" to listOf("lecchese","lecchesi"),
 "MS" to listOf("Massa Carrara","Massa","Carrara","apuano","apuana"),
 "VS" to listOf("Campidano","Medio Campidano","Villacidro","Sanluri"),
 "MB" to listOf("Monza","Brianza","brianzolo","brianzola"),
 "PU" to listOf("Pesaro","Urbino","pesarese","urbinate"),
 "PC" to listOf("piacentino","piacentina"),
 "RC" to listOf("Reggio Calabria","reggino","reggina"),
 "RE" to listOf("Reggio Emilia","Reggiano","Reggiana"),
 "SU" to listOf("Sulcis-Iglesiente","Sulcis","Iglesiente","Carbonia","Iglesias"),
 "TN" to listOf("Provincia autonoma di Trento","Trentino"),
 "AO" to listOf("Aosta","Valle d'Aosta/Vallée d'Aoste","Valle d'Aosta","Vallee d'Aoste"),
 "VB" to listOf("Verbano","Cusio","Ossola","Verbania","Domodossola"),
 "AL" to listOf("Alessandrino","Alessandria"), "AT" to listOf("Astigiano","Asti"),
 "BI" to listOf("Biellese","Biella"), "CN" to listOf("Cuneese","Cuneo"),
 "NO" to listOf("Novarese","Novara"), "TO" to listOf("Torinese","Torino"),
 "VC" to listOf("Vercellese","Vercelli"), "PV" to listOf("Pavese","Pavia"),
 "VA" to listOf("Varesotto","Varese"), "SO" to listOf("Valtellina","Sondrio"),
 "CR" to listOf("Cremonese","Cremona"), "MN" to listOf("Mantovano","Mantova"),
 "LO" to listOf("Lodigiano","Lodi"), "MI" to listOf("Milanese","Milano"),
 "BG" to listOf("Bergamasco","Bergamo"), "BS" to listOf("Bresciano","Brescia"),
 "VR" to listOf("Veronese","Verona"), "VI" to listOf("Vicentino","Vicenza"),
 "TV" to listOf("Trevigiano","Treviso"), "VE" to listOf("Veneziano","Venezia"),
 "PD" to listOf("Padovano","Padova"), "RO" to listOf("Polesine","Rovigo"),
 "PN" to listOf("Pordenonese","Pordenone"), "TS" to listOf("Triestino","Trieste"),
 "UD" to listOf("Friulano","Udine"), "GE" to listOf("Genovese","Genova"),
 "SV" to listOf("Savonese","Savona"), "IM" to listOf("Imperia"),
 "SP" to listOf("Spezzino","La Spezia"), "PR" to listOf("Parmense","Parma"),
 "MO" to listOf("Modenese","Modena"), "BO" to listOf("Bolognese","Bologna"),
 "FE" to listOf("Ferrarese","Ferrara"), "RA" to listOf("Ravennate","Ravenna"),
 "RN" to listOf("Riminese","Rimini"), "AR" to listOf("Aretino","Arezzo"),
 "GR" to listOf("Maremma","Grosseto"), "LI" to listOf("Livornese","Livorno"),
 "LU" to listOf("Lucchese","Lucca"), "PI" to listOf("Pisano","Pisa"),
 "PT" to listOf("Pistoiese","Pistoia"), "PO" to listOf("Pratese","Prato"),
 "SI" to listOf("Senese","Siena"), "PG" to listOf("Perugino","Perugia"),
 "TR" to listOf("Ternano","Terni"), "RM" to listOf("Romano","Roma"),
 "VT" to listOf("Tuscia","Viterbo"), "RI" to listOf("Reatino","Rieti"),
 "LT" to listOf("Pontino","Latina"), "FR" to listOf("Ciociaria","Frosinone"),
 "CH" to listOf("Chietino","Chieti"), "PE" to listOf("Pescarese","Pescara"),
 "TE" to listOf("Teramano","Teramo"), "CB" to listOf("Molisano","Campobasso"),
 "IS" to listOf("Isernino","Isernia"), "NA" to listOf("Napoletano","Napoli"),
 "CE" to listOf("Casertano","Caserta"), "SA" to listOf("Salernitano","Salerno"),
 "AV" to listOf("Irpinia","Avellinese","Avellino"), "BN" to listOf("Sannio","Beneventano","Benevento"),
 "FG" to listOf("Dauno","Foggiano","Foggia"), "BR" to listOf("Brindisino","Brindisi"),
 "LE" to listOf("Salentino","Lecce"), "TA" to listOf("Tarantino","Taranto"),
 "PZ" to listOf("Potentino","Potenza"), "MT" to listOf("Materano","Matera"),
 "CZ" to listOf("Catanzarese","Catanzaro"), "CS" to listOf("Cosentino","Cosenza"),
 "KR" to listOf("Crotonese","Crotone"), "VV" to listOf("Vibonese","Vibo Valentia"),
 "PA" to listOf("Palermitano","Palermo"), "CT" to listOf("Etneo","Catanese","Catania"),
 "ME" to listOf("Messinese","Messina"), "SR" to listOf("Siracusano","Siracusa"),
 "RG" to listOf("Ragusano","Ragusa"), "TP" to listOf("Trapanese","Trapani"),
 "CL" to listOf("Nisseno","Caltanissetta"), "EN" to listOf("Ennese","Enna"),
 "CA" to listOf("Cagliaritano","Cagliari"), "SS" to listOf("Sassarese","Sassari"),
 "NU" to listOf("Nuorese","Nuoro"), "OR" to listOf("Oristanese","Oristano"),
 "OG" to listOf("Ogliastra","Tortolì","Lanusei")
)

private fun feedSpecs(p:Province, period:String):List<FeedSpec>{
 val after=" after:"+SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(Date(periodStartMillis(period)))
 val aliases=provincialSearchAliases[p.code].orEmpty()
 val terms=(listOf(p.name,"provincia di "+p.name)+aliases).distinct()
 val territory="("+terms.joinToString(" OR "){"\""+it+"\"" }+")"
 val feeds=mutableListOf<FeedSpec>()

 // Ricerca territoriale ampia: non richiede che l'articolo nomini anche la regione.
 feeds+=googleNewsFeed("Ricerca generale · "+p.name,territory+after)

 // Seconda ricerca con temi locali: intercetta cronaca, servizi, viabilità e vita dei comuni.
 val localTopics="(cronaca OR comune OR sindaco OR viabilità OR scuola OR ospedale OR territorio OR lavori OR eventi OR provincia)"
 feeds+=googleNewsFeed("Cronaca e territorio · "+p.name,territory+" "+localTopics+after)

 // Ricerca dedicata: intercetta gli annunci funebri anche quando il feed generale non li evidenzia.
 val necrologyTerms="(necrologi OR necrologio OR epigrafi OR epigrafe OR \"annunci funebri\" OR \"camera ardente\" OR \"ultimo saluto\" OR \"è mancato\" OR \"è mancata\")"
 feeds+=googleNewsFeed("Necrologi · "+p.name,territory+" "+necrologyTerms+after)

 // Le fonti provinciali hanno già un focus geografico: non imponiamo un altro vincolo testuale.
 val localDomains=provincialNewsDomains[p.code].orEmpty().distinct()
 if(localDomains.isNotEmpty()){
  val sites=localDomains.joinToString(" OR "){"site:"+it}
  feeds+=FeedSpec("Fonti provinciali · "+p.name,googleNewsFeed("","("+sites+")"+after).url,true)
  feeds+=FeedSpec("Necrologi locali · "+p.name,googleNewsFeed("","("+sites+") "+necrologyTerms+after).url,true)
  // Secondo passaggio dedicato ai centri minori: frazioni, località, contrade e borgate.
  val minorPlaces="(frazione OR frazioni OR località OR localita OR borgata OR borgate OR contrada OR contrade OR paese OR paesi OR quartiere OR quartieri OR rione OR rioni OR territorio comunale)"
  feeds+=FeedSpec("Comuni e frazioni · "+p.name,googleNewsFeed("", "("+sites+") "+minorPlaces+after).url,true)
 }

 // Le testate regionali sono interrogate in gruppi piccoli, evitando query troppo lunghe.
 val regionalDomains=(regionalNewsDomains[p.region].orEmpty()-localDomains.toSet()).distinct()
 regionalDomains.chunked(7).forEachIndexed{index,group->
  val sites=group.joinToString(" OR "){"site:"+it}
  feeds+=googleNewsFeed("Fonti regionali "+(index+1)+" · "+p.name,territory+" ("+sites+")"+after)
 }

 when(p.code){
  "PC"->feeds+=FeedSpec("PiacenzaSera","https://www.piacenzasera.it/feed/",true)
  "CO"->{
   feeds+=FeedSpec("CiaoComo","https://www.ciaocomo.it/feed/",true)
   feeds+=FeedSpec("ComoZero","https://comozero.it/feed/",true)
   feeds+=FeedSpec("Prima Como","https://primacomo.it/feed/",true)
  }
 }
 return feeds.distinctBy{it.url}
}
private fun periodStartMillis(period:String, now:Long=System.currentTimeMillis()):Long {
 val calendar=java.util.Calendar.getInstance().apply{timeInMillis=now}
 when(period){
  "ultimora"->{calendar.set(java.util.Calendar.HOUR_OF_DAY,0);calendar.set(java.util.Calendar.MINUTE,0);calendar.set(java.util.Calendar.SECOND,0);calendar.set(java.util.Calendar.MILLISECOND,0)}
  "24h"->calendar.add(java.util.Calendar.HOUR_OF_DAY,-24)
  "3d"->calendar.add(java.util.Calendar.DAY_OF_YEAR,-3)
  "7d"->calendar.add(java.util.Calendar.DAY_OF_YEAR,-7)
  "1m"->calendar.add(java.util.Calendar.MONTH,-1)
  "1y"->calendar.add(java.util.Calendar.YEAR,-1)
  else->calendar.add(java.util.Calendar.HOUR_OF_DAY,-24)
 }
 return calendar.timeInMillis
}

private fun normalizedTerritoryText(value:String):String =
 Normalizer.normalize(value.lowercase(Locale.ITALY),Normalizer.Form.NFD)
  .replace(Regex("\\p{Mn}+"),"")
  .replace(Regex("[^\\p{L}\\p{N}]+")," ").trim()

private fun isDirectlyRelevant(article:BriefingArticle,province:Province):Boolean {
 val terms=(listOf(province.name)+provincialSearchAliases[province.code].orEmpty())
  .map(::normalizedTerritoryText).filter{it.length>=3}.distinct()
 val title=normalizedTerritoryText(article.title)
 val body=normalizedTerritoryText(article.title+" "+article.summary)
 fun containsTerm(text:String,term:String):Boolean =
  Regex("(^| )"+Regex.escape(term)+"( |$)").containsMatchIn(text)
 if(terms.any{containsTerm(title,it)}) return true
 // Una sola citazione nel testo descrittivo non basta: potrebbe essere marginale.
 val mentions=terms.sumOf{term->Regex("(^| )"+Regex.escape(term)+"( |$)").findAll(body).count()}
 return mentions>=2
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
private fun isNecrologyText(raw:String):Boolean {
 val text=java.text.Normalizer.normalize(raw.lowercase(Locale.ITALY),java.text.Normalizer.Form.NFD)
  .replace(Regex("\\p{Mn}+"),"").replace("’"," ").replace("'"," ")
  .replace(Regex("[^\\p{L}\\p{N}]+")," ").replace(Regex("\\s+")," ").trim()
 val indicators=listOf(
  "necrolog","epigraf","manifesti funebri","annunci funebri","funebr","funeral",
  "esequi","cordogli","condoglian","lutt","decesso","decess","defunt","tumulaz",
  "inumaz","cremaz","salma","camera ardente","onoranze funebri","pompe funebri",
  "servizi funebri","annuncio di morte","annunci di morte","annuncio funebre",
  "e mancato","e mancata","e venuto a mancare","e venuta a mancare",
  "venuto a mancare","venuta a mancare","ci ha lasciato","ci hanno lasciato",
  "si e spento","si e spenta","si e addormentato","si e addormentata",
  "si e ricongiunto","si e ricongiunta","trigesimo","trigesima","in suffragio",
  "partecipazione al lutto","sentite condoglianze","piu sentite condoglianze",
  "ne danno il triste annuncio","ne danno il doloroso annuncio",
  "annunciano la scomparsa","annunciano la morte","i funerali avranno luogo",
  "le esequie avranno luogo","la famiglia ringrazia","ringraziano quanti",
  "in memoria di","in ricordo di","anniversario della morte",
  "anniversario della scomparsa","anniversario della dipartita",
  "ricordano con affetto","lascia nel dolore"
 )
 return indicators.any { text.contains(it) }
}
private fun category(s:String,necrology:Boolean=false):String{
 if(necrology)return "NECROLOGI"
 val normalized=java.text.Normalizer.normalize(s.lowercase(Locale.ITALY),java.text.Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"),"")
 val title=normalized.substringBefore(" || ")
 val summary=normalized.substringAfter(" || ","")
 val groups=linkedMapOf(
  "CRONACA E SICUREZZA" to listOf("incidente","cronaca","polizia","carabinieri","furto","rapina","arrest","indagine","aggress","vigili del fuoco","sicurezza","ferito","feriti","denuncia","truffa","estorsione","spaccio"),
  "POLITICA E SERVIZI" to listOf("comune","sindaco","consiglio comunale","amministrazione","urbanistica","scuola","servizi","sanita","ospedale","giunta","assessore","delibera","municipio","lavori pubblici","trasporto pubblico"),
  "ECONOMIA E LAVORO" to listOf("lavoro","azienda","imprese","economia","commercio","industria","assunzioni","occupazione","sindacato","stipendi","attivita produttive","negozi","artigian"),
  "AMBIENTE E METEO" to listOf("meteo","temporale","allerta","maltempo","neve","pioggia","ambiente","inquinamento","rifiuti","raccolta differenziata","frana","alluvione","fiume","parco","incendio boschivo"),
  "CULTURA" to listOf("teatro","mostra","concerto","cultura","museo","festival","libro","presentazione","spettacolo","cinema","biblioteca","arte","rassegna","letteratura"),
  "SPORT" to listOf("sport","calcio","calcistico","partita","campionato","serie a","serie b","serie c","serie d","eccellenza","promozione","prima categoria","seconda categoria","terza categoria","girone","classifica","allenatore","calciatore","calciatrice","squadra","club","atleta","atleti","gara","torneo","derby","gol","goal","rigore","arbitro","stadio","trasferta","playoff","play off","playout","play out","coppa","vittoria","sconfitta","pareggio","pallavolo","pallacanestro","basket","volley","rugby","ciclismo","ciclista","nuoto","tennis","atletica","pallamano","pallanuoto","scherma","judo","karate","motori","rally","formula 1","formula uno","maratona","podismo","ginnastica","sci","snowboard","pattinaggio","tifosi","tifo","allenamento","retrocessione","capocannoniere")
 )
 fun score(words:List<String>):Int = words.sumOf { word -> (if(word in title) 3 else 0)+(if(word in summary) 1 else 0) }
 val best=groups.maxByOrNull { score(it.value) }
 return if(best!=null&&score(best.value)>=2)best.key else "TERRITORIO"
}
private fun rank(c:String)=when(c){"CRONACA E SICUREZZA"->0;"POLITICA E SERVIZI"->1;"ECONOMIA E LAVORO"->2;"AMBIENTE E METEO"->3;"CULTURA"->4;"SPORT"->5;else->6}
private fun parseFeed(f:FeedSpec):List<BriefingArticle>{
 val c=URL(f.url).openConnection() as HttpURLConnection
 try{
  c.connectTimeout=9000;c.readTimeout=12000;c.instanceFollowRedirects=true
  c.setRequestProperty("User-Agent","Mozilla/5.0 (compatible; CityNewsAI/1.0)")
  if(c.responseCode !in 200..299)return emptyList()
  val x=Xml.newPullParser().apply{setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,false);setInput(c.inputStream,"UTF-8")}
  val out= mutableListOf<BriefingArticle>();var inside=false;var title="";var desc="";var link="";var date="";var source=f.name;var feedCategories=""
  while(x.next()!=XmlPullParser.END_DOCUMENT){
   if(x.eventType==XmlPullParser.START_TAG){
    val tag=x.name.lowercase(Locale.ROOT)
    if(tag=="item"||tag=="entry"){inside=true;title="";desc="";link="";date="";source=f.name;feedCategories=""}
    else if(inside)when(tag){
     "title"->title=readTag(x)
     "description","summary","content","content:encoded","encoded"->{val value=readTag(x);if(value.length>desc.length)desc=value}
     "category","subject"->{
      val term=x.getAttributeValue(null,"term")?:x.getAttributeValue(null,"label")
      val value=if(!term.isNullOrBlank())term else readTag(x)
      if(value.isNotBlank())feedCategories+=" "+value
     }
     "link"->{val href=x.getAttributeValue(null,"href");if(!href.isNullOrBlank())link=href else {val v=readTag(x);if(v.startsWith("http"))link=v}}
     "pubdate","published","updated","date"->date=readTag(x)
     "source"->source=readTag(x).ifBlank{f.name}
    }
   }else if(x.eventType==XmlPullParser.END_TAG){
    val tag=x.name.lowercase(Locale.ROOT)
    if(inside&&(tag=="item"||tag=="entry")){
     val d=parseDate(date)
     if(title.isNotBlank()&&link.startsWith("http")&&d!=null){
      val summary=clean(desc)
      val necrology=isNecrologyText(title+" "+desc+" "+source+" "+link+" "+feedCategories+" "+f.name)
      out+=BriefingArticle(clean(title).ifBlank{title},summary,source,link,d,category(title+" || "+desc,necrology),localSource=f.localSource,necrology=necrology)
     }
     inside=false
    }
   }
  }
  return out
 }finally{c.disconnect()}
}

private fun titleTokens(article:BriefingArticle):Set<String> =
 article.title.lowercase(Locale.ITALY).replace(Regex("[^\\p{L}\\p{N}]+")," ")
  .split(Regex("\\s+")).filter{it.length>2}.toSet()

private fun sameNewsEvent(a:BriefingArticle,b:BriefingArticle):Boolean {
 val first=titleTokens(a);val second=titleTokens(b)
 if(first.isEmpty()||second.isEmpty())return false
 val overlap=first.intersect(second).size.toDouble()/first.union(second).size.toDouble()
 return overlap>=0.62
}

private fun articleDetailScore(article:BriefingArticle):Int {
 val summary=article.summary.trim()
 val title=article.title.trim()
 val detailWords=summary.split(Regex("\\s+")).count{it.length>3}
 val sentenceCount=summary.count{it=='.'||it=='!'||it=='?'}
 return (summary.length.coerceAtMost(2400)*2)+
  (detailWords.coerceAtMost(180)*8)+(sentenceCount.coerceAtMost(8)*35)+
  title.length.coerceAtMost(180)
}

internal suspend fun fetchFreeBriefing(province:Province,periodId:String):BriefingResult=withContext(Dispatchers.IO){
 val feeds=feedSpecs(province,periodId);val all=mutableListOf<BriefingArticle>();var read=0
 val feedResults=coroutineScope{feeds.map{feed->async(Dispatchers.IO){runCatching{parseFeed(feed)}.getOrDefault(emptyList())}}.awaitAll()}
 feedResults.forEach{items->if(items.isNotEmpty())read++;all+=items}
 val now=System.currentTimeMillis()
 val cutoff=periodStartMillis(periodId,now)
 // I risultati delle ricerche provinciali e dei feed locali possono riguardare frazioni o comuni
 // senza citare il capoluogo: non applicare loro il filtro lessicale sul nome della provincia.
 val filtered=all.filter{it.publishedAt.time in cutoff..now && (it.localSource || isDirectlyRelevant(it,province))}
 val remaining=filtered.sortedByDescending{articleDetailScore(it)}
 val merged=mutableListOf<BriefingArticle>()
 val consumed=BooleanArray(remaining.size)
 for(i in remaining.indices){
  if(consumed[i]) continue
  val group=mutableListOf(remaining[i]);consumed[i]=true
  for(j in i+1 until remaining.size){
   if(consumed[j]) continue
   val candidate=remaining[j]
   if(kotlin.math.abs(group.first().publishedAt.time-candidate.publishedAt.time)>36L*60L*60L*1000L) continue
   if(group.any{sameNewsEvent(it,candidate)}){group+=candidate;consumed[j]=true}
  }
  val best=group.maxByOrNull{articleDetailScore(it)}!!
  val isNecrology=group.any{it.necrology}
  merged+=best.copy(corroborationCount=group.map{it.source}.distinct().size,category=if(isNecrology)"NECROLOGI" else best.category,necrology=isNecrology)
 }
 val resultLimit=when(periodId){"ultimora"->60;"24h"->60;"3d"->75;"7d"->90;"1m"->110;"1y"->150;else->60}
 val ordered=if(periodId=="ultimora") merged.sortedByDescending{it.publishedAt.time}
  else merged.sortedWith(compareBy<BriefingArticle>{rank(it.category)}.thenByDescending{it.publishedAt.time})
 val articles=ordered.take(resultLimit)
 BriefingResult(articles,SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.ITALY).format(Date()),read)
}
