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

## Principi
- Non copiare o ripubblicare integralmente gli articoli.
- Conservare sempre URL e attribuzione della testata.
- Non inventare riassunti o dettagli mancanti.
- Le chiavi API AI non saranno mai incluse nell'app Android.
- Una fonte RSS non è considerata approvata finché non viene verificata.
