# Localizzazione UI Desktop (i18n)

Le stringhe della UI desktop vivono qui, nel **formato XML di Android**
(`values-*/strings.xml`), perche' e' il formato che gestiscono i servizi di
traduzione (Crowdin, Lokalise, Transifex, OneSky...). Il desktop NON usa le
risorse Android: e' JVM (Compose Desktop), quindi questi file sono semplici
risorse del classpath Java che `DesktopI18n.kt` carica e parsa a runtime.

## Struttura

```
composeApp/src/jvmMain/resources/i18n/
├── values/strings.xml          ← INGLESE (fonte, lingua di DEFAULT)
├── values-it/strings.xml       ← Italiano
├── values-de/strings.xml       ← Deutsch
├── values-pt-rBR/strings.xml   ← Portugues do Brasil
├── values-zh-rCN/strings.xml   ← 简体中文
├── values-zh-rTW/strings.xml   ← 繁體中文
├── values-in/strings.xml       ← Bahasa Indonesia (codice Android legacy)
├── values-iw/strings.xml       ← עברית (codice Android legacy)
├── values-sr/strings.xml       ← Српски (cirillico)
├── values-sr-rCS/strings.xml   ← Srpski latinica
└── ... (47 lingue in tutto, le STESSE dell'app Android)
```

I nomi delle cartelle seguono la **stessa convenzione di
`androidApp/src/main/res/`**, quindi chi gia' lavora con le traduzioni
dell'app Android si ritrova a casa.

## Come funziona a runtime

- all'avvio l'app carica `values/strings.xml` (inglese) e applica sopra l'override
  della lingua scelta; ogni chiave mancante ricade SEMPRE sull'inglese (mai testi
  vuoti), ultima spiaggia la chiave grezza;
- il DEFAULT all'avvio e' la lingua DEL PC (`Locale.getDefault`): se il sistema
  e' italiano parte la UI italiana; se la lingua del PC non e' tra le 47
  supportate parte l'inglese;
- il selettore a GLOBO nella title bar cambia lingua al volo e persiste la
  scelta in `~/.riplay/app.properties` (`ui.language`; "system" = segui il PC);
- il codice cartella per ogni lingua e' mappato in `DesktopI18n.androidResFolder`.

## Regole per i traduttori

1. **`name` non si traduce**: e' la chiave interna della UI (es. `queue.title`).
2. **Segnaposto NON tradotti e NON rimossi**: `{n}`, `{code}`, `{meaning}`,
   `{value}` restano identici nel testo tradotto, spostabili nel punto giusto
   della frase (es. `Coda di riproduzione ({n})` -> `Wiedergabeliste ({n})`).
3. **Escape Android**: l'apostrofo si scrive `\'`, le virgolette `\"`, le
   nuove righe `\n` (es. `L\'applicazione`); i caratteri XML `&`, `<`, `>`
   restano `&amp;`, `&lt;`, `&gt;`.
4. **Niente markup HTML** dentro le stringhe (il desktop le mostra come testo).
5. Una lingua puo' essere PARZIALE: le chiavi mancanti mostrano l'inglese.

> Nota tecnica: `name` contiene la chiave con i punti (es. `queue.title`):
> questi file non passano mai per AAPT (non sono risorse di un modulo Android),
> quindi la restrizione "niente punti nei resource name" non si applica; i
> servizi di traduzione leggono/scrivono l'attributo cosi' com'e'.

## Aggiungere una NUOVA lingua

1. crea `values-<codice>/strings.xml` copiando `values/strings.xml` e traduci;
2. aggiungi la voce all'enum `Languages` in
   `composeApp/src/jvmMain/kotlin/it/fast4x/riplay/enums/Locales.kt`
   (`tag` + `nativeName`, stesse regole dell'app Android);
3. se il codice cartella non e' il semplice codice lingua (es. `pt-rBR`,
   `zh-rCN`, `sr-rCS`...), aggiungi il mapping in `DesktopI18n.androidResFolder`
   e nella sezione `languages_mapping` del `crowdin.yml` alla radice;
4. per l'app Android vale lo stesso percorso in `androidApp/src/main/res/`.

## Verifiche

Prima di rilasciare si puo' ricontrollare la coerenza delle 48 tabelle con:

```
python3 scripts/check-i18n-step3t.py
```

(controlla: XML ben formati, chiavi conosciute, nessuna stringa vuota,
segnaposto coerenti con l'inglese, copertura per lingua)
