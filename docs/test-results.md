# Build- und Testbericht — 16.10.2026

## Nachprüfung Bedienung — 18.10.2026

Der Paketnamespace wurde anschließend vollständig auf `de.teutonstudio.fluxbattery` umgestellt, einschließlich Quellpfaden, Gradle-Gruppe, Mixin-Konfigurationen, Coremod-ABI und Prüfwerkzeugen. Sauberer Build mit Testfixture, Distributionsprüfung, alle 14 GameTests und paketierter Client-Smoke-Test waren nach der Umstellung erfolgreich.

Die Konfiguration öffnet jetzt auch per normalem Rechtsklick in die Luft. Beim Anvisieren eines Blocks lässt sich dessen Interaktion mit Shift-Rechtsklick umgehen; auch der Item-Blockinteraktionspfad öffnet die Konfiguration. Deutsche und englische Tooltips erklären die Bedienung und dass vor dem Einsetzen ein Netzwerk gespeichert werden muss. Ohne zugängliche Netzwerke zeigt die Oberfläche einen Hinweis zum Anlegen eines Flux-Netzwerks.

`./gradlew build --offline --no-daemon --max-workers=2` erfolgreich. Alle 14 GameTests mit dem gebauten Addon-JAR erfolgreich. Der erweiterte Konfigurationstest startet mit einem neuen leeren Item, führt normalen Rechtsklick ohne Shift aus, prüft die initialisierte Identität, speichert das erste Netzwerk über den Update-Handler und setzt die weiterhin leere Batterie regulär in eine Enrichment Chamber ein. Anschließend werden 1000 FE vom Flux Plug empfangen und an die Maschine abgegeben. Der paketierte Client-Smoke-Test hat die Konfigurationsvorschau gerendert und sich erfolgreich beendet. Dieser Client-Test bleibt eine synthetische Vorschau; echte Client-Server-Bedienung und Shift-Rechtsklick auf einen Block wurden nicht automatisiert geprüft.

## IntelliJ-Startkorrektur — 18.10.2026

Der delegierte Application-Start `:net.neoforged.devlaunch.Main.main()` scheiterte mit unbekanntem `cpw.mods.securejarhandler` und fehlendem Zugriff auf `MethodHandles.Lookup.IMPL_LOOKUP`. Die automatisch generierten Application-Starts sind jetzt über `disableIdeRun()` deaktiviert. Eingecheckte Gradle-Konfigurationen starten Client, Server und GameTests über die vollständigen ModDev-Aufgaben. Im lokalen IDEA-Arbeitsbereich wurden die alten Application-Starts entfernt und **Flux Battery Client** ausgewählt.

Mit Java 21 und dem vorhandenen Offline-Cache erfolgreich geprüft: `./gradlew build --offline --no-daemon --max-workers=2` einschließlich JUnit und Loader-Prüfung sowie `./gradlew runClient -PclientSmoke --offline --no-daemon --max-workers=2`. Der Client bestätigte alle Pflichtpatches, renderte die Konfigurationsvorschau und beendete sich mit `BUILD SUCCESSFUL`; weder `Unknown module` noch `InaccessibleObjectException` traten auf. Der Start über den IDEA-Run-Button wurde nicht automatisiert geprüft.

## Ergebnis und Vergleichsbasis

Flux Battery läuft mit dem **unveränderten Flux Networks 8.0.0**. Es wird ausschließlich ein Flux-Battery-JAR gebaut. Die Ladezeit-Bridge erhält die virtuelle Point-Funktion und Flux' regulären Verteiler; die Mekanism-Anbindung bleibt erhalten.

Der vorherige getestete Ersatz-JAR-Stand ist als `bridge-baseline-8.0.0` (`e119495`) gesichert. Historischer Bericht und Hashnachweis liegen separat unter `bridge-baseline-test-results.md` und `evidence/bridge-baseline-verification.txt`. Der isolierte Ladezeit-Prototyp wurde vor dem Umbau auf Client und Dedicated Server nachgewiesen und separat committet (`16cea0f`).

Geprüfte Grundkombination: Minecraft 1.21.1, NeoForge 21.1.231, Java 21 (Temurin 21.0.12.1), Kotlin 2.1.21, KFF 5.9.0, Mekanism-Artefakt 1.21.1-10.7.19.85 (Mod-Version 10.7.19), originales Flux Networks 8.0.0, Gradle 9.2.1 und ModDevGradle 2.0.141. Flux-Eingabe-SHA256: `29e511b2d76f85bd247aa213a92bb059ecc037fcc3fed82baffcff97b638d3a6`.

## Ausgeführte Prüfungen

| Prüfung | Ergebnis / Umfang |
|---|---|
| Isolierter Ladezeit-Prototyp | Dedicated Server bis „Done“, Client bis gerenderter Vorschau; frühe Kotlin-ABI und Mixin-Koexistenz bestätigt |
| Build und JUnit | 3 erfolgreiche JUnit-Tests, einschließlich 10.000 deterministischer Energieerhaltungsfälle |
| Loader-Strukturprüfung | 8 Originalklassen akzeptiert; geänderte Körper (8), fehlende Methoden (8), geänderte Methodenflags (8), Feldflags (7) und Oberklassen (8) abgewiesen |
| Bisherige Regression mit Original-Flux | Alle 12 bisherigen GameTests erfolgreich |
| Gebautes Addon-JAR mit Original-Flux | **14 erforderliche GameTests erfolgreich**; lose Entwicklungsklassen ausgeschlossen |
| Neustart „prepare“ und „verify“, getrennte JVMs | Je **15 erforderliche Tests erfolgreich**, beide JVMs sauber beendet; Energie, Name, gültiges/verwaistes Ticket und Entnahme bestätigt |
| Regulärer Dedicated Server, gebautes Addon | Alle **14 Tests auch auf regulärem DedicatedServer erfolgreich**, darunter Minerbetrieb, Abbau/Wiederplatzieren und Tickets; Konsolenstopp und Weltspeicherung |
| Client, gebautes Addon | Erfolgreiche Pflichtpatches einschließlich ClientFluxNetwork, gerenderte GUI-Vorschau und automatisches Beenden |
| Zwei kompatible synthetische Zusatzmods | **14 erforderliche Tests erfolgreich**, Mixin-Erweiterung und Methodentransformer nachgewiesen |
| Pflicht-Bytecode-Konflikt | Konkrete Meldung `changed bytecode in onEndServerTick()V`, obligatorischer Startguard verhindert den Serverstart |
| Flux-Version 8.0.1 (synthetische Negativprobe) | Loader lehnt unpassende Version vor dem Serverstart ab |
| Verändertes JAR mit Versionsnummer 8.0.0 | Runtime-SHA256-Prüfung verhindert den Serverstart |
| JAR-Inhalt und Laufzeitabhängigkeiten | Ein Addon-JAR, keine `sonar/`-Einträge oder Testfixture-/Toolklassen; Original-Flux-Hash bestätigt |

Die vierzehn GameTests wurden außerdem über die echte Konsole eines regulären DedicatedServer ausgeführt. Ein eigener Testreporter protokolliert jeden Erfolg, weil Minecraft die Testzusammenfassung ansonsten nur an Spieler sendet. Das beseitigt die Beschränkung auf den speziellen GameTestServer.

Die vierzehn GameTests:

1. Gemeinsamer FE-/Long-Speicher, Simulation ohne Komponentenänderung, Werte über Int.MAX_VALUE, Handlerkopien und ItemStack-Persistenz.
2. Sofortige Entnahme vor Verarbeitung der Anmeldequeue: keine Verbindung und kein aktiver Point.
3. Digital Miner, Enrichment Chamber und Energized Smelter über denselben Mekanism-Adapter versorgen und regulär entladen; leere Batterie entnehmen.
4. Surge, Priorität, Transferlimits und knappe Plug-Energie; exakte Gutschriften und negative Point-Verbrauchsstatistik.
5. Unberechtigte private Bindung ablehnen, berechtigte zulassen, Attachment bei Netzwerklöschung entfernen.
6. Eigenes tickendes Chunk-Ticket erzeugen und bei Entnahme oder Serverdeaktivierung entfernen.
7. Batterie zwischen Maschinen und Netzwerken verschieben; Energie und ID erhalten, alten Standort entfernen; Maschinenabbau meldet ab.
8. Netzwerk während ausstehender Anmeldung löschen: Attachment und Queues leer, Netzwerk ungültig, erneute Anmeldung abgewiesen; nachfolgender Tick bleibt zulässig.
9. **Tatsächlicher Digital-Miner-Betrieb:** regulärer Plug → Batterie → regulärer Energieslot → Eisenerzabbau mit Raw-Iron-Ausgabe, ohne Point oder Controller am Miner.
10. Normale Flux Points, Plugs, Controller und Basic Storage: Anmeldung, Verteilung, Statistik und Abmeldung.
11. Server-Konfiguration: falsche Batterie-ID, ungültige Werte, unbekannte/fremde private Netzwerke und fremder Besitzer abgelehnt; gültige Änderung erhält Energie und ignoriert gefälschten Besitzer.
12. Zwei maximale Long-Bedarfe: Summe sättigt, Long.MAX_VALUE wird exakt zugeteilt, volle Batterie fordert nichts mehr und kann voll entnommen werden.
13. **Hosted-Anzeige, Farbwechsel und Statistik:** zwei echte Plugs empfangen je Long.MAX_VALUE, Flux verteilt beide Beträge exakt auf zwei Batterien; Eingangs-/Ausgangsstatistik sättigt. Flux' Phantom-Updatepfad zeigt Batterieitem, Namen, Maschinenposition und Verbrauch. Der tatsächliche Netzwerk-Farbwechselcallback verarbeitet normale Tiles und Hosted Points ohne Tile-Castfehler oder Maschinenersatz.
14. **Survival-Abbau und Wiederplatzieren:** Maschine mit Batterie und 765432 FE abbauen, echtes ItemEntity aufnehmen, Maschinenitem serialisieren und wiederherstellen, über BlockItem.place platzieren. Batterie-ID, Energie, Konfiguration und Maschinenenergie bleiben erhalten; alte Verbindung und altes Ticket verschwinden, nur der neue Standort wird registriert. Auch der erneute Abbau entfernt dessen Ticket.

Der GameTest-Server besitzt keinen GameProfileCache. Für den FakePlayer wird im Abbau-Test deshalb NeoForges Namenscache wie bei einem regulären Login vorbelegt. Die ersten Probeläufe deckten außerdem Testfehler auf: Phantom-Speicherformat statt Updateformat und bereits gefüllte Plug-Puffer statt neu empfangener Energie. Die endgültigen Tests benutzen die tatsächlich passenden Upstream-Pfade; diese Korrekturen änderten keine Energieimplementierung. Der Wiederplatzierungsplatz wird ausdrücklich freigeräumt, damit auch normales DedicatedServer-Gelände keine Platzierung verhindert.

Der Zweiprozess-Neustart speichert Batterie mit 987654 FE, Name, volle Maschine, gültiges Ticket und bewusst verwaistes Ticket. Die neue JVM muss gültige Daten und Ticket wiederherstellen, das verwaiste Ticket verwerfen und das wiederhergestellte Ticket nach Entnahme entfernen. Beide Stufen laden das gebaute Addon-JAR und jeweils zusätzlich die vierzehn normalen Tests.

## Geprüfte Transformer-Kombinationen

Die Loader-Stichprobe besteht aus **eigenen synthetischen Testmods**, jeweils zusätzlich zur Grundkombination:

- `fluxbattery_mixin_fixture` 1.0: Kotlin-Mixin ergänzt `FluxNetwork` in der AFTER-Phase. Der Test bestätigt die tatsächlich vorhandene Zusatzmethode.
- `fluxbattery_compatible_fixture` 1.0: METHOD-Coremod ergänzt einen NOP in `TileFluxDevice.getTransferChange()`, außerhalb der dortigen Pflichtanker. Gemeinsam mit dem Mixin-Fall werden alle GameTests ausgeführt.
- `fluxbattery_conflict_fixture` 1.0: METHOD-Coremod verändert die Pflichtstruktur von `ServerFluxNetwork.onEndServerTick()`. Die anschließende CLASS-Prüfung lehnt sie ab; der Modkonstruktor bricht den Start ab.

Diese drei Mods werden nur für Tests unter `build/fixtures/` erzeugt. Sie sind **keine Freigabe realer Drittmods**. Weitere tatsächlich Flux-verändernde Drittmod-JARs wurden bislang nicht festgelegt oder geprüft. Unveränderte Pflichtstrukturen können mit zusätzlichen Methoden koexistieren; beliebige spätere Mixins, Overwrites oder Modpack-Kombinationen sind nicht garantiert. Addons, die Runtime-Teilnehmer zwingend auf TileFluxDevice casten, bleiben inkompatibel.

## Reproduktion

```bash
python3 scripts/bootstrap-dependencies.py
./gradlew build
./gradlew runGameTestServer -PpackagedAddon
python3 scripts/test-restart.py
python3 scripts/test-loader.py
python3 scripts/test-server.py
./gradlew runClient -PpackagedAddon -PclientSmoke
python3 scripts/verify-distribution.py
```

Lokal verwendet: `JAVA_HOME=/tmp/fluxbattery-gradle/jdks/eclipse_adoptium-21-amd64-linux.2`, `GRADLE_USER_HOME=/tmp/fluxbattery-gradle`. Die Werkzeuge verwenden die nach `build` vorhandenen Abhängigkeiten offline. Der paketierte ModDev-Lauf besitzt einen eigenen Laufzeit-SourceSet, um lose Main-Ressourcen nicht zusätzlich als unvollständige Mod zu entdecken. Es werden keine Addonklassen aus Entwicklungsverzeichnissen zugeladen.

Für den Linux-Client bleibt `earlyWindowControl=false` in `run-client/config/fml.toml` gesetzt. Der Screenshot ist eine synthetische GUI-Vorschau. Er ersetzt keine Bedienung durch zwei echte Clients. Entscheidend für Startabbruchtests sind die tatsächlichen Modlade- und Serverstartmarker; FML kann einen gescheiterten Modstart trotz Gradle-Status `BUILD SUCCESSFUL` melden.

## Weiterhin manuell zu prüfen

- Zwei echte Spieler auf Dedicated Server: Konfigurationsoberfläche vollständig bedienen und Rechteentzug über Flux' normales GUI prüfen.
- Einzelspieler-Bedienung und vollständige visuelle Prüfung der Flux-Verbindungsübersicht.
- Reale ungeticketete Chunk-Entladung, Rückkehr und Batteriebewegung zwischen Dimensionen.
- Längerer Minerbetrieb ohne Spieler über mehrere Abbauchunks mit Mekanism-Anchor-Regeln.
- Mehrstündiger Modpackbetrieb, mehrere Besitzer/Dimensionen und Ticket-Quoten.
- Konkrete weitere Drittmods, die Flux-Klassen verändern; die synthetischen Stichproben ersetzen dies nicht.

## Installation und Grenzen

Installiert werden **Flux Battery + originales Flux Networks 8.0.0 + Mekanism + Kotlin for Forge** auf NeoForge 21.1.231. Ein Ersatz-JAR wird nicht benötigt. Aktuelle Ausgabe: `build/libs/fluxbattery-0.1.0-dev.jar`. Hashes und entscheidende Logzeilen stehen in `evidence/verification.txt`.

Ein aktiver Point pro Maschinenposition; erster passender Batterieslot gewinnt. Keine komplexen Mekanism-IMultiblocks und keine automatische Unterstützung fremder FE-Slots. Chunkloading ist standardmäßig aus und hält ausschließlich den Maschinenchunk. Long-Summenanzeigen sättigen. Creative-/Admin-Kopien kompletter Items liegen außerhalb der Energieerhaltung normaler Survival-Transfers.
