# Flux-Networks-Runtime-Bridge

## Grundlage und Ausgangsreferenz

Freigegeben: originales Flux Networks **8.0.0**, Release-Commit `45cf2480474782ecad66fb3c1215eb935fdb0d88`, CurseForge-Datei **6089446**, SHA-256 `29e511b2d76f85bd247aa213a92bb059ecc037fcc3fed82baffcff97b638d3a6`.

`bridge-baseline-8.0.0` sichert den getesteten Ersatz-JAR-Stand `e119495`. Der alte Patch unter `bridge/patches/fluxnetworks-8.0.0-runtime-device.patch` bleibt die historische Änderungsbeschreibung; die damaligen Ersatzquellen und der damalige Build sind über diesen Git-Tag wiederherstellbar. Vergleichsbericht: `bridge-baseline-test-results.md`, alte Hashes: `evidence/bridge-baseline-verification.txt`.

Der isolierte Ladezeit-Prototyp ist separat in `16cea0f` gesichert. Client, Dedicated Server, frühe Kotlin-ABI-Verfügbarkeit und Mixin-Koexistenz sind in `load-time-prototype.md` nachgewiesen. Erst danach wurde die Runtime-Bridge umgesetzt.

## Auslieferung

Nur `build/libs/fluxbattery-0.1.0-dev.jar` wird ausgeliefert. Es enthält keine Klassen oder Ressourcen unter `sonar/`. Das unveränderte Flux-JAR wird sowohl in der Entwicklungsumgebung als auch in der Installation geladen. `patchedFlux`, der Java-Bridge-SourceSet und vollständige Ersatzklassen entfallen.

NeoForge liest `META-INF/coremods.json` und führt `coremods/fluxbattery.js` vor der Klassendefinition aus. Das Skript löst nur freigegebene NeoForge-/ASM-Klassen auf. Sämtliche neuen JVM-Klassen bleiben Kotlin; das frühe JavaScript-Skript ist die ausdrücklich dokumentierte technische Abweichung.

## Zielklassen und Eingriffe

| Originalklasse | Eingriff |
|---|---|
| `TileFluxDevice` | Implementiert zusätzlich `FluxRuntimeDevice`; vorhandene Methoden erfüllen die ABI. |
| `FluxNetwork` | Implementiert `FluxRuntimeNetwork`, erhält Runtime-Überladungen und bewahrt Tile-Delegationssignaturen. |
| `ServerFluxNetwork` | Runtime-Typen in aktiven Listen, Queues, Casts, Comparator-Lambdas und Transferpfaden; Queue-/Löschguards; Bedarfssumme sättigt. |
| `ClientFluxNetwork` | Passende Runtime-Überladungen; serverseitige logische Operationen bleiben unzulässig. |
| `TransferIterator` | Runtime-Iterator einschließlich Rückgabe, Feld, Casts und Aufrufen; alte Tile-Rückgabesignatur als Kompatibilitätsüberladung. |
| `NetworkStatistics` | Runtime-Lambda-Parameter; fünf nichtnegative Summen-/Differenzpfade sättigen bei Long.MAX_VALUE. |
| `PhantomFluxDevice` | Runtime-Überladung für `makeUnloaded`; alte Tile-Signatur bleibt. |
| `Messages` | Ausschließlich der Blockupdate-Methodenverweis beim Netzwerkfarbwechsel wird auf die Runtime-Schnittstelle umgestellt. |

Die ABI liegt im Addon-Namespace `de.teutonstudio.fluxbattery.bridge`: `FluxRuntimeDevice`, `FluxRuntimeNetwork`, `ServerRuntimeAccess`. Normale Flux-Geräte und Hosted Points benutzen Flux' originale Queues und denselben Verteiler. `RuntimeHooks` besitzt weder eine eigene Netzwerkregistry noch einen Energieverteiler.

Ausstehende Anmeldung wird bei sofortiger Entnahme aus `mToAdd` entfernt. Positionskarten werden nur für die passende Instanz entfernt; eine andere Runtime-Instanz an derselben Position wird abgewiesen. Netzwerklöschung trennt aktive und ausstehende Teilnehmer, sperrt neue Anmeldungen, leert Listen und Queues und lässt keine null-Listen zurück. Das Löschflag wird vor den Disconnect-Callbacks gesetzt.

Hosted Points liefern Statistik und Verbindungsvorschau über das bestehende Phantom-Tagformat samt Batterieitem und Maschinenposition. Ihr Blockupdate-Hook ist leer; normale Flux-Tiles behalten ihre Rendererupdates. Summen jenseits Long.MAX_VALUE sättigen, ohne die normale Prioritäts-/Surge-Verteilung zu verändern.

## Pflichtprüfung und Fehlerverhalten

`coremods/fluxbattery-8.0.0.json` enthält die eingecheckte erwartete Oberklasse, Feld-/Methodenstrukturen einschließlich Zugriffsflags, Exceptiontabellen und ausführbare Opcodefolgen mit Memberreferenzen, Konstanten, Lambda-Handles und Sprungzielen. Debugzeilen und Stackframes sind keine Vergleichsanker. Die Prüfungen laufen vor der Änderung einer Klasse. Zusätzliche Methoden/Felder sind möglich, solange Pflichtstrukturen passen.

CoreMods 7.0.3 kann Scriptfehler protokollieren und den Knoten weiterreichen. Deshalb genügt ein Script-`throw` nicht. Erfolgsmarker werden zuletzt ergänzt; Fehler erhalten eine konkrete Diagnosemethode. `RuntimePatchGuard` prüft im Modkonstruktor Version und SHA-256 des Flux-JARs und erzwingt das Laden aller Pflichtklassen. Fehlender Marker, falscher Marker, Diagnose oder Ladefehler brechen den Start ab. Die clientseitige Zielklasse wird auf dem Dedicated Server nicht geladen. Ein teilweise wirksamer Patch wird damit nicht zum Spielbetrieb freigegeben.

Loader-Reihenfolge in der geprüften Kombination: BEFORE-Plugins → PRE_CLASS → Feld-/Method-Transformer → CLASS-Coremods → AFTER-Plugins (Mixin). Keine zugesicherte Reihenfolge zwischen beliebigen CLASS-Coremods. Spätere Mixins können Methoden verändern; die Tests begründen keine Garantie gegen beliebige Overwrites oder Modpacks.

## Reproduktion und Grenzen

```bash
./gradlew build
./gradlew runGameTestServer
./gradlew runGameTestServer -PpackagedAddon
python3 scripts/test-restart.py
python3 scripts/test-loader.py
python3 scripts/test-server.py
./gradlew runClient -PpackagedAddon -PclientSmoke
python3 scripts/verify-distribution.py
```

`verifyLoader` gehört zu `check`. Das Kotlin-Werkzeug prüft acht Originalklassen, acht geänderte Methoden, acht fehlende Methoden, acht geänderte Methodenflags, sieben geänderte Feldflags und acht geänderte Oberklassen mit demselben Transformationsskript. `generateFluxShapes` aktualisiert die festgelegte Eingabebeschreibung nur bei bewusster Wartung; es wird nicht automatisch vor Prüfungen ausgeführt.

`test-loader.py` baut getrennte, synthetische Lowcode-Mods: eine Methodentransformation außerhalb der Pflichtanker, einen Kotlin-Mixin auf FluxNetwork und einen absichtlichen Pflichtkonflikt. Zusätzlich müssen falsche Version und umgepacktes JAR vor dem Serverstart scheitern. Testfixtures liegen ausschließlich unter `build/fixtures/` und werden nicht ausgeliefert. Konkrete Ergebnisse und Einschränkungen stehen im Testbericht.

Drittaddons, die `getLogicalDevices()` zwingend als `TileFluxDevice` iterieren, benötigen Anpassungen an Runtime-Teilnehmer. Die alte Iterator-Tile-Überladung kann bei Hosted Points kein Tile liefern. Maschinenunterstützung entsteht ausschließlich durch einen zuverlässigen Host-Adapter, nicht allein durch FE-Kompatibilität.
