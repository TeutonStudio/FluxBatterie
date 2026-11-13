# Build- und Testbericht

## Veröffentlichungskandidat 0.1.3-beta.2 — 13.11.2026

Die neue Konfigurationsoberfläche orientiert sich an den klassischen Flux-Networks-Geräten: dunkles Panel, farbiger Rahmen, Reiter für Einstellungen und Netzwerkauswahl, Schiebeschalter und kompakte Pufferanzeige. Die Darstellung wird eigenständig gezeichnet und verwendet keine kopierten Flux-GUI-Texturen. Netzwerkfarben kommen im Snapshot vom Server; das Konfigurationsprotokoll ist jetzt Version 2. Gespeicherte Item-/Weltdaten bleiben unverändert, Client und Server müssen gemeinsam aktualisiert werden.

`configurationSnapshotRoundTrip` prüft im GameTest den vollständigen Snapshot einschließlich mehrerer Netzwerkfarben, Unicode-Namen, Offhand, aller Optionen und Long-Grenzwerten. Der Client-Prüflauf bedient die tatsächlichen Widgets mit synthetischen Daten: Suche nach Namen/ID, Seitenwechsel, Auswahl, Entwurfserhalt, alle Schalter, ungültige Priorität, fehlende Netzwerke, leere Suchergebnisse und Trennen. Die tatsächliche Fenstergröße wechselt von 854 × 480 auf 640 × 480 (GUI-Skalierung 2, entsprechend 320 × 240 GUI-Pixeln); Entwürfe bleiben erhalten. Beide Sprachen wurden ausgeführt und die wesentlichen Layouts visuell geprüft. Dies ist keine vollständige Spielverbindung oder manuelle Bedienungsabnahme.

| Prüfung | Ergebnis |
|---|---|
| Build, JUnit und Loader-Strukturen | Bestanden; 3 Unit-Tests |
| Paketierte GameTests mit kompatiblen Mixin-/Coremod-Fixtures | 24/24 bestanden |
| Neustart in zwei getrennten JVMs | Je 26/26 bestanden |
| Regulärer Dedicated Server | 24/24 bestanden, sauber beendet und gespeichert |
| Pflichtkonflikt, falsche Flux-Version und verändertes Original-JAR | Erwarteter Startabbruch bestätigt |
| Paketierter Client mit JEI, Deutsch und Englisch | Beide Prüfläufe bestanden, Screenshots bei beiden Fenstergrößen |
| Distribution und Formatprüfung | Ein Addon-JAR; Version, Lizenzen und JSON geprüft; `git diff --check` bestanden |

Der JAR-Hash blieb bei den abschließenden Prüfungen unverändert. Artefakt- und Log-Prüfsummen: [Release-Nachweis](evidence/release-0.1.3-beta.2.txt). Reproduzierbarer Clientlauf: `python3 scripts/test-client.py`; die bisherigen Client-Optionen werden anschließend wiederhergestellt. Vorschau: [Einstellungen](evidence/battery-screen.png), [Netzwerkauswahl](evidence/battery-networks.png), [kleine GUI](evidence/battery-small-layout.png), [Schalter und Long-Limit](evidence/battery-switches.png), [Validierung](evidence/battery-validation.png), [Englisch](evidence/battery-screen-en.png).

Die noch offenen echten Multiplayer-, Chunk-Unload- und Langzeittests bleiben im [Abnahmeprotokoll](release-acceptance.md) dokumentiert; dieser Stand ist weiterhin als Beta gekennzeichnet.

## Veröffentlichungskandidat 0.1.3-beta.1 — 12.11.2026

MIT-Lizenz und Flux-Networks-Lizenzhinweis werden im JAR mitgeliefert. Die Mod-Beschreibung nennt sowohl Point- als auch Plug-Betrieb. README und Changelog beschreiben den aktuellen Stand.

Der neue GameTest `crossDimensionMoveAndPerOwnerQuota` verwendet echte Maschinen in Overworld und Nether. Er prüft ein gemeinsames Kontingent pro Besitzer über Dimensionsgrenzen, ein unabhängiges Kontingent für einen zweiten Besitzer sowie Umzug derselben Batterie mit erhaltener Identität, Konfiguration und Energie. Das alte Ticket und die alte Verbindung müssen verschwinden, am Ziel wird das frei gewordene Kontingent nutzbar. Ein separater Testbesitzer isoliert diesen Fall von persistenten Neustart-Fixtures.

Der Neustarttest legt jetzt pro Durchlauf eine frische Welt unter `build/restart-world-*` an und verwendet sie für beide JVMs. Das verhindert Einflüsse alter Fixtures, zurückgelassener Netzwerke und bereits verbrauchter Kontingente; frühere Testwelten bleiben erhalten. Loader-Prozessprüfungen laden ebenfalls das paketierte Addon.

Die Loader-Prüfdaten enthielten ursprünglich rohe String-Konkatenations-Steuerzeichen (`U+0001`). Diese sind jetzt als Unicode-Escapes maskiert; ein Vergleich der dekodierten Daten bestätigt unveränderte Prüfstrukturen. Der Generator maskiert sämtliche JSON-Steuerzeichen, `verifyLoader` liest über `JSON.parse` und prüft einen Rundlauf aller 32 Steuerzeichen einschließlich Backslash/Anführungszeichen. Die Distributionsprüfung liest alle JSON-Ressourcen im fertigen JAR strikt ein.

Aktueller Prüflauf erfolgreich mit Java **Temurin 21.0.7+6**, NeoForge 21.1.231 und dem endgültigen Beta-JAR:

| Prüfung | Ergebnis |
|---|---|
| Build, JUnit, Loader-Strukturen und JSON-Rundlauf | Bestanden; 3 Unit-Tests ohne Fehler/übersprungene Tests |
| GameTestServer mit paketiertem Addon | 23/23 bestanden |
| Zwei getrennte Neustart-JVMs, frische gemeinsame Testwelt | Je 25/25 bestanden, vollständige Weltspeicherung |
| Regulärer Dedicated Server mit paketiertem Addon | 23/23 bestanden, Konsolenstopp und vollständige Weltspeicherung |
| Kompatible synthetische Mixin-/Coremod-Fixtures | 23/23 bestanden mit paketiertem Addon |
| Pflichtkonflikt, Flux 8.0.1, verändertes 8.0.0-JAR | Je erwarteter Startabbruch nachgewiesen |
| Client mit JEI | Vorschau/Screenshot und sauberes Beenden erfolgreich; deutsche Oberfläche bei 854 × 480 visuell geprüft |
| Distribution und Formatprüfung | Ein Addon-JAR, MIT/Upstream-Hinweis und strikt gültige JSON-Ressourcen; `git diff --check` erfolgreich |

Der SHA-256 des JARs blieb bei den abschließenden Prüfungen unverändert. [Release-Nachweis](evidence/release-0.1.3-beta.1.txt), [Quellstand-Prüfsummen](evidence/release-0.1.3-beta.1-source.sha256) und [Changelog](../CHANGELOG.md). Die bisherigen datierten Abschnitte sind historische Nachweise; ihre Testzahlen und Hashes gelten ausschließlich für den jeweils damaligen Stand.

Vollständige manuelle Spielabnahme bleibt offen: [Abnahmeprotokoll](release-acceptance.md). Insbesondere belegen serverseitige Dimensions-/Tickettests weder einen tatsächlichen Chunk-Unload mit Wiederkehr noch einen durch echte Clients ausgelösten Dimensionswechsel.

## Laden und Netzwerk-Einspeisung — 27.10.2026

Mekanism-Item-Ladeslots (`drain`, z. B. Plus-Slot des Energiewürfels) akzeptieren jetzt auch volle konfigurierte Batterien. Der Ladehost ist ein nativer Flux-Plug-Teilnehmer. FE- und Flux-Long-Empfang füllen zuerst den lokalen Batterie-Puffer; Überschüsse gelangen unter Beachtung von Rechten, verbleibendem Zykluslimit und offenem Netzwerkbedarf in die persistente Item-Komponente `network_input`. Flux verteilt diese Energie regulär an Points und Speicher. Der Tooltip zeigt noch ausstehende Übergaben. Ohne Netzwerkbedarf bleibt die abgewiesene Energie im Ladeblock. Unkonfigurierte Batterien können lokal geladen werden.

`./gradlew build --offline` erfolgreich: drei Unit-Tests und die bestehende Loader-Strukturprüfung. `./gradlew runGameTestServer -PpackagedAddon --offline` erfolgreich mit **22/22 erforderlichen Tests** (16 bestehende und sechs neue Ladetests):

- Volle und teilweise gefüllte Batterien im leeren Energiewürfel; unkonfigurierte Batterie im gefüllten Ladeblock ausschließlich lokal laden; keine Überschussannahme ohne Netzwerkbedarf.
- Tatsächlicher `drainContainer()`-Pfad: 250 FE freie Batteriekapazität zuerst füllen, weitere 1000 FE ins Netzwerk; Quelle = lokale Gutschrift + ausstehende Übergabe + Speicherzugang.
- Schreibfreie Simulation; mehrere FE-/Flux-Handler teilen ein Zykluslimit; kopierte Stacks können keine fremde Hostbindung nutzen; positive Plug-Statistik ohne Point-Registrierung.
- Entnahme vor Verteilung, Item-NBT-Rundlauf und erneutes Einsetzen erhalten ausstehende Übergabeenergie und setzen deren Verteilung fort; alte Handler bleiben getrennt.
- Mitgliedschaft sperren und Netzwerk löschen: keine weitere Einspeisung und kein Verlust bereits angenommener Energie; nach Freigabe fortsetzen.
- `Long.MAX_VALUE` Eingang und Bedarf an zweitem Maschinen-Point ohne Überlauf oder Veränderung des lokalen Quellpuffers.

Der Zweiprozess-Neustarttest (`python3 scripts/test-restart.py`) besteht in beiden getrennten JVMs mit jeweils **24/24 erforderlichen Tests**. Die neue Ladefixture speichert 987654 FE im vollen lokalen Puffer plus 654321 FE ausstehende Übergabe in einem Energiewürfel. Nach dem Neustart werden privates Netzwerk, Batteriekomponenten und tickendes Chunk-Ticket wiederhergestellt; die 654321 FE fließen über den normalen Verteiler in einen echten Flux-Speicher, der lokale Puffer bleibt unverändert und Entnahme entfernt das Ticket. Die bestehende Point-/Orphan-Ticket-Prüfung besteht ebenfalls.

Auch der reguläre Dedicated Server mit gebautem Addon-JAR besteht alle **22/22 Regressionstests** (`python3 scripts/test-server.py`) und beendet sich anschließend über die Konsole mit vollständiger Weltspeicherung. Die Reporter-/Script-Erwartung übernimmt jetzt die tatsächlich registrierte Testzahl statt eines alten festen Werts. Distributionsprüfung und `git diff --check` sind erfolgreich.

Die erste Neustartprobe deckte einen zeitabhängigen Fehler in der bestehenden Miner-Stressprüfung auf: Bei längerer Suche konnte die erste Quelle bereits vollständig in den anschließend geleerten Akku geladen sein. Die separate Stressphase erhält jetzt nach bestätigtem echtem Abbau einen festen Quellbestand; die erwartete 100-Millionen-Zuteilung bleibt unverändert.

Automatische Netzwerk-Einspeisung bleibt an den Mekanism-Hostadapter gebunden. Andere FE-Ladeblöcke können den lokalen Puffer laden. Komplexe Mekanism-Multiblocks bleiben ausgeschlossen; bei mehreren Batterieslots gewinnt weiterhin die erste Batterie.


## Release 0.1.2 — 18.10.2026

Sauberer Build mit drei Unit-Tests, alle 16 GameTests mit dem paketierten Addon, Client-Smoke-Test mit JEI und Distributionsprüfung erfolgreich. Das neue Mod-Icon ist im JAR enthalten und registriert. Versions- und Hashnachweis: [evidence/release-0.1.2.txt](evidence/release-0.1.2.txt).

## Nachprüfung Ausrüstanimation und JEI — 18.10.2026

Die Batterie unterdrückt die erneute Ausrüstanimation bei synchronisierten Energie- und Konfigurationsänderungen derselben Batterie. Slotwechsel, andere Items und Batterien mit anderer Identität lösen sie weiterhin aus. `batteryReequipAnimation` deckt diese Fälle ab; `./gradlew build prepareClientRun runGameTestServer` war erfolgreich, alle 16 GameTests bestanden.

JEI 19.39.0.372 ist ausschließlich im Client-SourceSet als Laufzeitabhängigkeit eingebunden. `runClient -PclientSmoke --offline` und derselbe Lauf mit `-PpackagedAddon` starteten erfolgreich mit JEI in der Modliste, renderten die GUI-Vorschau und beendeten sich automatisch. Die sichtbare Handanimation im Spiel wurde nicht manuell geprüft. Die Distributionsprüfung war erfolgreich.

## Nachprüfung Durchfluss — 18.10.2026

Neue Batterien haben jetzt ein Standardlimit von 100.000.000 statt 1.000.000 FE pro Flux-Zyklus. Bestehende Item-Konfigurationen behalten ihr gespeichertes Limit; es kann in der Batterieoberfläche erhöht oder deaktiviert werden. Das versorgende Plug-Limit bleibt separat wirksam.

`./gradlew build --offline --no-daemon --max-workers=2` und alle 14 GameTests mit `-PpackagedAddon` erfolgreich. Der erweiterte Digital-Miner-Test prüft nach tatsächlichem Erzabbau eine Zuteilung von 100 Millionen FE aus einer leeren Batterie. Anschließend werden alle unterstützten Upgrades installiert, maximaler Radius, volle Welthöhe und Silk Touch aktiviert und 64 Versorgungszyklen über den echten Item-Energieadapter geprüft. Jeder Zyklus deckt den von Mekanism berechneten Verbrauch; die Energiebilanz bleibt erhalten. Diese Phase simuliert den Energieverbrauch durch Entnahme aus dem Maschinenpuffer, ohne einen vollständigen Abbau des großen Suchbereichs abzuwarten. Protokoll: `build/throughput-gametest.log`. Die Ursache einer Unterversorgung im konkreten Spielstand ist damit nicht nachgewiesen.

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

Konkrete Durchführung, Erwartungen und offener Status pro Fall: [Release-Abnahme](release-acceptance.md).

- Zwei echte Spieler auf Dedicated Server: Konfigurationsoberfläche vollständig bedienen und Rechteentzug über Flux' normales GUI prüfen.
- Einzelspieler-Bedienung und vollständige visuelle Prüfung der Flux-Verbindungsübersicht.
- Reale ungeticketete Chunk-Entladung, Rückkehr und Batteriebewegung zwischen Dimensionen.
- Längerer Minerbetrieb ohne Spieler über mehrere Abbauchunks mit Mekanism-Anchor-Regeln.
- Mehrstündiger Modpackbetrieb, mehrere Besitzer/Dimensionen und Ticket-Quoten.
- Konkrete weitere Drittmods, die Flux-Klassen verändern; die synthetischen Stichproben ersetzen dies nicht.

## Installation und Grenzen

Installiert werden **Flux Battery + originales Flux Networks 8.0.0 + Mekanism + Kotlin for Forge** auf NeoForge 21.1.231. Ein Ersatz-JAR wird nicht benötigt. Aktueller Veröffentlichungskandidat: `build/libs/fluxbattery-0.1.3-beta.1.jar`. Historische Ausgaben und ihre Nachweise bleiben unten unverändert dokumentiert. Das aktuelle Abnahmeprotokoll steht in [release-acceptance.md](release-acceptance.md).

Ein aktiver Point pro Maschinenposition; erster passender Batterieslot gewinnt. Keine komplexen Mekanism-IMultiblocks und keine automatische Unterstützung fremder FE-Slots. Chunkloading ist standardmäßig aus und hält ausschließlich den Maschinenchunk. Long-Summenanzeigen sättigen. Creative-/Admin-Kopien kompletter Items liegen außerhalb der Energieerhaltung normaler Survival-Transfers.


## UI und Kreativtab (18.10.2026)

- Die Batterie erscheint zusätzlich im Kreativtab von Flux Networks (`fluxnetworks:tab`). Tools & Utilities bleibt verfügbar.
- `creativeTabIntegration` baut die tatsächlichen Kreativtab-Inhalte mit der Server-Registry auf und prüft genau einen Batterieeintrag in beiden Tabs sowie die Aufnahme in ihre Suchinhalte.
- `./gradlew build runGameTestServer -PpackagedAddon --offline`: erfolgreich; 3 Unit-Tests und alle 15 GameTests bestanden, einschließlich Kreativtab-Integration.
- `./gradlew build runClient -PpackagedAddon -PclientSmoke --offline`: erfolgreich mit dem gebauten Addon-JAR. Deutsche Konfigurationsvorschau bei 854 × 480 Pixeln und GUI-Skalierung 2 visuell geprüft; Screenshot: `evidence/battery-screen.png`.
- Dunkles Flux-Terminal mit türkisfarbenem Akzent, segmentierter Energieanzeige, Vor-/Zurück-Netzwerkauswahl, gruppierten Eingaben, Schaltern, Tooltips und Abbrechen/Speichern. Das Transferlimit bleibt bei aktiviertem Unlimited erhalten und ist dann nicht editierbar. Zahlenfelder filtern ungültige Zeichen und prüfen die serverseitig zulässigen Werte vor dem Senden.
- Sämtliche neuen Oberflächentexte sind für Deutsch und Englisch lokalisiert. Der Status stellt klar, dass die gehaltene Batterie getrennt ist und erst eingesetzt als Point arbeitet. Energie ist eine Momentaufnahme beim Öffnen.
- Neues transparentes Item-Icon, mit dem eingebauten Imagegen-Werkzeug erzeugt und per Nearest-Neighbor auf 32 × 32 exportiert. Prompt und Herkunft: `evidence/item-icon-prompt.txt`.
- `python3 scripts/verify-distribution.py` und `git diff --check`: erfolgreich.

Der Client-Smoke-Test prüft eine synthetische Vorschau, keinen vollständigen Konfigurationsdialog mit realem Spieler und Server. Eine manuelle Prüfung aller Schalter, Tastaturbedienung und Größen-/Sprachvarianten bleibt ergänzend sinnvoll.


## Release 0.1.1 (18.10.2026)

Das versionierte JAR wurde mit `build runGameTestServer runClient -PpackagedAddon -PclientSmoke --offline` geprüft: drei Unit-Tests, alle 15 GameTests und Client-Smoke-Test erfolgreich. Distributionsprüfung sowie Prüfung der Mod-Version und des Plus-Rezepts direkt im JAR erfolgreich. Nachweis und SHA-256: `evidence/release-0.1.1.txt`.
