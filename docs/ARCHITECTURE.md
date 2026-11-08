# Architettura iniziale

## Decisioni
- Android e backend separati: le credenziali AI restano esclusivamente lato server.
- Feed RSS/Atom come prima modalità di acquisizione.
- SQLite per il prototipo locale; PostgreSQL previsto per il servizio condiviso.
- Ogni articolo conserva URL e attribuzione della fonte.
- Matching territoriale conservativo: un articolo non viene assegnato a un territorio solo perché la testata ha sede lì.
- Deduplicazione deterministica tramite URL canonico; il matching semantico tra articoli di fonti diverse sarà una fase successiva.
- Nessuna fonte è attiva finché feed e condizioni d'uso non sono stati verificati.

## Limiti noti della prima iterazione
- Il matching territoriale usa alias espliciti in titolo e descrizione; non copre ancora tutti i comuni delle province.
- Il riepilogo RSS può contenere HTML o testo incompleto: va normalizzato prima di inviarlo a un modello AI.
- Non si scaricano né si ripubblicano gli articoli integrali.
- Il collector non è ancora un servizio schedulato né espone API HTTP.
