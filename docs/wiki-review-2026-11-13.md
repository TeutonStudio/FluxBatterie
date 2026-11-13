# Wiki-Prüfung — 13. November 2026

## Ergebnis und Umfang

- Kurze englische README mit deutschen/englischen Wiki-Einstiegen, Downloads, Issues und Entwicklungsverweisen.
- 28 Wiki-Markdown-Dateien: 12 vollständige deutsch/englische Anleitungspaare, zwei Einstiegsseiten, Sidebar und Footer.
- 10 unveränderte eingebundene PNGs: acht frische Spielaufnahmen und zwei bestehende synthetische UI-Prüfbilder.
- 10 dokumentierte Funktionsthemen mit passenden Medien und ausdrücklichen Aussagegrenzen.
- Ausführliche bisherige README unter `docs/development-de.md` erhalten; Archivlinks an den neuen Ablageort angepasst.
- Keine Änderung am Produktcode, an Versionen oder Release-Artefakten.

## Quellenbaseline

Veröffentlichtes Flux Battery 0.1.3-beta.2, Commit `1a9e690`.
JAR-SHA-256: `276fc88d7fa117ed27c8dd49fb03979a30a9a2ba4e1ca35b2f17659e4eb1c643`.
Die Release-Assets wurden vor der Wiki-Arbeit als veröffentlicht bestätigt.
Die neuen Anleitungen wurden mit Rezept, Item-/Netzwerklogik, Hostadapter, Ticketcontroller,
Serverkonfiguration und tatsächlichen Release-Nachweisen verglichen.

## Tatsächliche Aufnahme

Temporäres separates Fixture-Mod, neue flache Testwelt, Verzeichnis `build/wiki-capture-run`,
1280 × 800 Pixel und GUI-Skalierung 2. Java 21, Minecraft 1.21.1, NeoForge 21.1.231,
Mekanism 1.21.1-10.7.19.85, KFF 5.9.0, originales Flux Networks 8.0.0 und JEI 19.39.0.372.
Es wurde das unveränderte paketierte Addon verwendet. Keine persönliche Welt wurde geöffnet.

Beobachtungen:

- Zwei echte private Flux-Netzwerke des angemeldeten Spielers angelegt; Konfiguration über den serverseitigen Batterie-Öffnungspfad empfangen.
- Originales Werkbankrezept ausgewertet und Batterie im Ergebnis geprüft.
- Energized Smelter erzeugte tatsächlich zwei Eisenbarren aus Roheisen mit einer Flux-versorgten Batterie im Entladeslot.
- Vollgeladene Batterie im Ladeslot eines Ultimate Energy Cube speiste Energie ein; echter Flux-Speicher meldete 2.000.000 FE.
- Natives Flux-GUI zeigte den virtuellen Plug „Solarspeicher“ und den tatsächlichen Flux-Speicher. Zum Aufnahmezeitpunkt war der Speicher voll und der momentane Transfer null.
- Native NeoForge-Serverkonfiguration geöffnet. Unübersetzte Konfigurationsschlüssel der aktuellen Beta werden in der Anleitung ausdrücklich erwähnt; sie wurden nicht im Bild ersetzt.
- Client meldete `WIKI_CAPTURE_COMPLETE`, beendete sich sauber und speicherte alle Dimensionen.

Die temporäre Fixture stellte Anfangsenergie und Inventare bereit. Das ist weder ein manuell
gebauter Survival-Durchlauf noch eine autonome Solarproduktionsanlage. Screenshotdateien wurden
ohne Pixelbearbeitung kopiert; Hashes stehen im Medieninventar.

## Prüfung

`python3 scripts/verify-wiki.py --require-all-media`: 28 Seiten, 12 Sprachpaare, 10 Bilder und
10/10 dokumentierte Funktionsthemen erfolgreich. `git diff --check` erfolgreich.
`python3 scripts/verify-distribution.py` bestätigt das unveränderte Release-JAR einschließlich
Metadaten, Lizenzen, JSON und unverändertem Original-Flux-Eingang.

Die vorhandenen automatisierten Release-Ergebnisse werden korrekt wiedergegeben, nicht als
neu ausgeführte Wiki-Tests ausgegeben. Echte Zwei-Spieler-Abnahme, tatsächliche Chunk-Entladung
mit Wiederkehr und mehrstündiger Modpackbetrieb bleiben offen.

Die strenge Wiki-Prüfung ist auch als separater GitHub-Actions-Dokumentationsworkflow eingerichtet.

## Nach Veröffentlichung

Die Wiki wird separat vom Hauptrepository übertragen. Online-Darstellung, Sprachwechsel und
Bildauslieferung werden nach dem Push geprüft. Diese Dokumentationsänderung erzeugt keinen
neuen Mod-Release und verändert den vorhandenen Release-Tag nicht.
