# CityNewsAI Android

Prima app Android dimostrativa realizzata con Kotlin e Jetpack Compose.

## Requisiti
- Android Studio recente
- JDK 17
- Gradle 8.9
- Android SDK 35

## Build locale
Da questa cartella:

```bash
gradle assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Compatibilità
- Minimo Android 7.0 (API 24).
- Versione 0.3.0.
- I contenuti sono esclusivamente dimostrativi: nessun titolo o fonte rappresenta una notizia reale.
- Ricerca province tramite tastiera, con elenco filtrato per nome, sigla e regione.
- Avvio con provincia preferita o rilevamento della provincia corrente (permesso posizione approssimativa richiesto).
- Tema chiaro, scuro o secondo le impostazioni di sistema.
- Icona launcher con sagoma bianca dell'Italia, fondo verde e lettering rosso.
