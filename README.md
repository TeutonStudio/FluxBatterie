# Flux Battery

Kotlin-Addon für Minecraft **1.21.1 / NeoForge**. Eine konfigurierte Batterie im Energie-Eingabeslot einer Mekanism-Maschine arbeitet als virtueller Flux Point. Die Energie fließt vom normalen Flux-Netzwerk in die Batterie und von dort über Mekanisms Item-Energieadapter in die Maschine. Ein Flux Controller oder zusätzlicher Point an der Maschine ist dafür nicht erforderlich.

## Installation

Auf Client und Server installieren:

- NeoForge **21.1.231**, Java **21**
- Mekanism **1.21.1-10.7.19.85** (interne Mod-Version `10.7.19`)
- Kotlin for Forge **5.9.0** (NeoForge-Version)
- `fluxbattery-0.1.0-dev.jar` aus `build/libs/`
- originales `FluxNetworks-1.21.1-8.0.0.jar` (CurseForge-Datei **6089446**)

**Ausgeliefert wird nur das Flux-Battery-JAR.** Ein Ladezeit-Patch erweitert die originalen Flux-Klassen im Speicher. Ein altes Ersatz-/Bridge-JAR muss aus dem Mods-Ordner entfernt werden. Mod-ID und Flux-Speicherformate bleiben erhalten.

Freigegeben ist ausschließlich das unveränderte Flux Networks **8.0.0**, SHA-256 `29e511b2d76f85bd247aa213a92bb059ecc037fcc3fed82baffcff97b638d3a6`. Version, JAR und Pflichtstrukturen werden geprüft; ein unpassender Stand bricht den Start mit einer konkreten Meldung ab.

## Bedienung

1. Batterie herstellen: `IFI / ETE / IFI`; I = Eisenbarren, F = Flux Core, E = Redstone, T = Mekanism Energy Tablet. Alternativ Kreativtab „Tools & Utilities“.
2. Batterie halten und **Shift-Rechtsklick** ausführen.
3. Ein zugängliches Netzwerk auswählen, Name, Priorität, FE-Limit pro Zyklus, unbegrenztes Limit, Surge und optional Chunkloading einstellen; speichern.
4. Batterie in den Energie-Eingabeslot des Digital Miners einsetzen. Leere konfigurierte Batterien werden ebenfalls angenommen.
5. Netzwerk benötigt eine Energiequelle an einem regulären Flux Plug. Versorgung und Priorisierung erfolgen durch Flux Networks.
6. Entnehmen beendet Verbindung und Chunk-Ticket. Dieselbe Batterie kann anschließend in eine andere geeignete Maschine eingesetzt werden.

Privat-/verschlüsselte Netzwerke erfordern dauerhafte Besitzer- oder Mitgliedsrechte. Passwörter werden nicht gespeichert. Nur der Batterie-Besitzer kann ihre Konfiguration bearbeiten. Netzwerkrechte werden während des Betriebs erneut geprüft.

Getestet: Digital Miner (tatsächlicher Erzabbau), Enrichment Chamber, Energized Smelter. Der Mekanism-Adapter unterstützt weitere Mekanism-Maschinen mit `fill`-/`fillOrConvert`-Energieslots. Komplexe `IMultiblock`-Maschinen sind ausgeschlossen. Fremde FE-Maschinen werden nicht automatisch unterstützt; zusätzliche Mod-Adapter sind ein separates Erweiterungsziel. Bei mehreren Batterieslots arbeitet nur die Batterie im ersten passenden Slot als Point; weitere bleiben normale Energieitems.

## Serverkonfiguration

`fluxbattery-server.toml` (NeoForge bestimmt den weltabhängigen Ablageort):

```toml
allowChunkLoading = false
maxTicketsPerOwner = 4
batteryCapacityFE = 1000000000
```

Chunkloading benötigt zusätzlich die aktivierte Option im Item, gültige Netzwerkrechte und die globale Flux-Networks-Chunkloading-Freigabe. Das Kontingent gilt pro Batterie-Besitzer über alle Dimensionen. Jede Batterie hält nur ihren Maschinenchunk tickend geladen. Ein Digital Miner kann zusätzlich Mekanisms reguläres Anchor-Upgrade benötigen, wenn sein Abbaubereich weitere Chunks umfasst; die Batterie übernimmt diese zusätzlichen Chunks nicht.

## Bauen und prüfen

```bash
python3 scripts/bootstrap-dependencies.py
./gradlew build
./gradlew runGameTestServer
python3 scripts/test-restart.py
python3 scripts/test-loader.py
python3 scripts/test-server.py
./gradlew runClient -PpackagedAddon -PclientSmoke
python3 scripts/verify-distribution.py
```

Java 21 über `JAVA_HOME` auswählen; Gradle kann die Toolchain alternativ herunterladen. Der unveränderte MDK-Wrapper ist Gradle **9.2.1**, ModDevGradle **2.0.141**, Kotlin **2.1.21**. Die Bootstrap-Datei lädt nur das festgelegte Original-Flux-JAR und prüft SHA-256; Gradle lädt Mekanism/KFF aus den festgelegten Maven-Repositories. `build` erzeugt ausschließlich das Addon in `build/libs/`; `patchedFlux` und der Bridge-SourceSet entfallen. Für einen Lauf mit dem gebauten Addon-JAR statt losen Entwicklungsklassen: `./gradlew runGameTestServer -PpackagedAddon`. Die Bytecode-Prüfstrukturen sind eingecheckt; `verifyLoader` prüft Originalklassen sowie absichtlich veränderte und fehlende Methoden und gehört zu `check`. `generateFluxShapes` ist ausschließlich ein Wartungswerkzeug nach bewusst geprüfter Änderung der Zielversion.

Der Client-Smoke-Test zeigt eine synthetische Konfigurationsvorschau, speichert `docs/evidence/battery-screen.png` und beendet den Client.

## Entwicklungsstand

Build- und Laufzeitnachweise, noch manuell zu prüfende Szenarien sowie genaue Einschränkungen stehen in [docs/test-results.md](docs/test-results.md). Die automatisierten Tests ersetzen keinen Langzeit- oder Modpack-Test. Architektur: [docs/architecture.md](docs/architecture.md). Upstream-Abweichungen: [docs/flux-network-bridge.md](docs/flux-network-bridge.md).
