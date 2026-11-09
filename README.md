# City News AI

Piattaforma di notizie locali con app Android e motore centrale di raccolta, deduplicazione e sintesi AI.

## Territori iniziali
- Piacenza e provincia
- Como e provincia

## Architettura
- `backend/`: raccolta RSS, normalizzazione, classificazione territoriale e API.
- `android/`: applicazione Android (fase successiva).
- `docs/`: decisioni architetturali e procedure operative.

## Stato del progetto
Fase 1: motore di raccolta. In questa prima iterazione si implementano normalizzazione, matching territoriale, deduplicazione e persistenza SQLite. Le fonti giornalistiche saranno abilitate solo dopo averne verificato feed, affidabilità e condizioni d'uso.

## Avvio rapido
Richiede Python 3.11 o superiore.

```bash
cd backend
python -m venv .venv
# Windows: .venv\\Scripts\\activate
# Linux/macOS: source .venv/bin/activate
pip install -r requirements.txt
python -m citynews.cli --help
```

## Copertura territoriale e fonti

Il catalogo territoriale contiene 110 unità provinciali e territoriali equivalenti, basate sul registro ISTAT SITUAS e aggiornabili nel tempo. La selezione della provincia nell'app è ricercabile per nome, sigla o regione.

Le fonti sono valutate con priorità per:
- quotidiani locali storici e registrati, con redazione identificabile;
- testate giornalistiche professionali e gruppi editoriali riconosciuti;
- editori con informazioni legali, responsabilità editoriale e contatti verificabili.

L'affidabilità editoriale viene valutata separatamente dalla disponibilità tecnica: una testata autorevole può essere approvata come fonte anche se il suo RSS non è ancora disponibile. In quel caso si cerca un canale tecnico alternativo, senza attivare scraping non verificato o aggirare paywall e restrizioni. Libertà è registrata come fonte ad alta affidabilità per Piacenza; il feed resta in attesa di verifica tecnica.

## Principi
- Non copiare o ripubblicare integralmente gli articoli.
- Conservare sempre URL e attribuzione della testata.
- Non inventare riassunti o dettagli mancanti.
- Le chiavi API AI non saranno mai incluse nell'app Android.
- Una fonte RSS non è considerata approvata finché non viene verificata.
