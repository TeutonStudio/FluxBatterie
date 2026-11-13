# Changelog

## 0.1.3-beta.2 — 13.11.2026

- Konfiguration im Stil klassischer Flux-Networks-Geräte überarbeitet: dunkles Panel, Rahmen in Netzwerkfarbe, Reiter und kompakte Schiebeschalter.
- Direkte Netzwerkauswahl mit Suche nach Name/ID, Seitenwechsel, vollständigen Namen/IDs im Tooltip und explizitem Trennen.
- Eingaben bleiben bei Reiterwechseln, Netzwerkauswahl und Größenänderungen erhalten. Speichern und Abbrechen behalten ihre bisherige Bedeutung.
- Kompakte Pufferanzeige mit Prozentwert; exakte FE-Werte und Hinweis auf den Stand beim Öffnen im Tooltip. Deutsch und Englisch aktualisiert.
- Netzwerkfarbe im Konfigurations-Snapshot übertragen; Protokoll auf 2 erhöht. Client und Server müssen gemeinsam aktualisiert werden. Gespeicherte Batterie- und Weltdaten bleiben kompatibel.
- Snapshot-Rundlauf als GameTest ergänzt und Client-Prüflauf um Bedienungs- und Layoutszenarien erweitert.
- Weiterhin Beta: Die noch offenen Multiplayer-, Chunk-Unload- und Langzeittests aus dem Abnahmeprotokoll gelten unverändert.

## 0.1.3-beta.1 — 12.11.2026

- Batterie im Mekanism-Item-Ladeslot arbeitet als virtueller Flux Plug: zuerst den lokalen Puffer füllen, dann Überschüsse in das konfigurierte Flux-Netzwerk einspeisen.
- Ohne Netzwerkbedarf weitere Überschussenergie abweisen. Bereits angenommene Übergabeenergie bleibt bei Entnahme, Speichern und Neustart im Batterieitem erhalten.
- Netzwerkrechte, Transferlimits und Bindung an den aktiven Slot auch beim Einspeisen prüfen.
- Neue Ladetests sowie erweiterte Neustart- und Dedicated-Server-Regression. Zusätzlicher Test für Batterieumzug zwischen Overworld und Nether und Ticketkontingente pro Besitzer über Dimensionsgrenzen. Loader-Prozessprüfungen verwenden ebenfalls das fertige Addon-JAR.
- Neustarttests verwenden pro Durchlauf eine frische isolierte Welt, damit zurückgelassene Netzwerke und Kontingente frühere Ergebnisse nicht beeinflussen.
- Loader-Prüfdaten als strikt gültiges JSON schreiben und lesen; Steuerzeichen korrekt maskieren, ohne die erwarteten Bytecode-Strukturen zu ändern. Die Distributionsprüfung prüft alle JSON-Dateien im fertigen JAR.
- Veröffentlichung unter MIT; eigene Lizenz und Flux-Networks-Lizenzhinweis im JAR enthalten.
- Beschreibung und Release-Dokumentation um Point- und Plug-Betrieb ergänzen.

### Bekannte Grenzen

- Nur originales Flux Networks 8.0.0 mit dem dokumentierten SHA-256; Mekanism-Mod-Version 10.7.19, Minecraft 1.21.1 und NeoForge ab 21.1.231 innerhalb 21.1.
- Automatische Netzwerkverbindung nur in unterstützten Mekanism-Slots; keine komplexen Multiblocks. Bei mehreren passenden Batterieslots gewinnt der erste.
- Chunkloading standardmäßig aus; lädt nur den Maschinenchunk. Weitere Miner-Abbauchunks benötigen Mekanisms Anchor-Regeln.
- Echte Multiplayer-GUI-Abnahme, tatsächlicher Chunk-Unload mit Rückkehr, Dimensionswechsel und mehrstündiger Modpackbetrieb noch offen; siehe Abnahmeprotokoll.

## 0.1.2 — 18.10.2026

- Mod-Icon, JEI-Client-Smoke-Test und Korrektur der Ausrüstanimation.
- Nachweis des damaligen Artefakts: `docs/evidence/release-0.1.2.txt`.

## 0.1.1 — 18.10.2026

- Virtueller Flux Point in Mekanism-Energieslots, Konfigurationsoberfläche, Kreativtab-Integration und optionales Chunkloading.
- Nachweis des damaligen Artefakts: `docs/evidence/release-0.1.1.txt`.
