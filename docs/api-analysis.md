# API-Analyse — 15.10.2026

## Festgelegte Quellen und Artefakte

- Flux Networks Release 8.0.0: Commit `45cf2480474782ecad66fb3c1215eb935fdb0d88` (Commitnachricht „Release 8.0.0“).
- Zweig `1.21`: `ea8b6e0cf708098ab2f02bf5c3d5c1f0ed2a1f5f`, bereits `8.1.0-SNAPSHOT`. Nicht als Releasebasis verwenden.
- Veröffentlichtes JAR: CurseForge Projekt 248020, Datei 6089446, `FluxNetworks-1.21.1-8.0.0.jar`.
  SHA-256: `29e511b2d76f85bd247aa213a92bb059ecc037fcc3fed82baffcff97b638d3a6`.
- Mekanism: Maven `mekanism:Mekanism:1.21.1-10.7.19.85`, einschließlich Sources-JAR.
- KFF: `thedarkcolour:kotlinforforge-neoforge:5.9.0` aus https://thedarkcolour.github.io/KotlinForForge/ . Das veröffentlichte POM bestätigt Kotlin **2.1.21**, Coroutines 1.10.2 und Serialization 1.8.1.

Quellen: [Flux Release](https://github.com/SonarSonic/Flux-Networks/commit/45cf2480474782ecad66fb3c1215eb935fdb0d88), [JAR](https://www.curseforge.com/minecraft/mc-mods/flux-networks/files/6089446), [Mekanism](https://github.com/mekanism/Mekanism/tree/1.21.x).

## Bestätigte Flux-Einstiegspunkte

Mit `javap` am veröffentlichten JAR bestätigt: `ServerFluxNetwork.getLogicalDevices(int)` liefert `ArrayList<TileFluxDevice>`; `enqueueConnectionAddition(TileFluxDevice)` und `enqueueConnectionRemoval(TileFluxDevice, boolean)` verlangen echte Tiles. `FluxCapabilities.ITEM` ist `ItemCapability<IFNEnergyStorage, Void>`.

Releasequellen:

| Klasse | Bedeutung für die Integration |
|---|---|
| `FluxNetwork` | Gemeinsame Teilnehmer-API; Verbindungskarte nach `GlobalPos`, aktive Listen konkret typisiert. |
| `ServerFluxNetwork` | Warteschlangen, Sortierung nach Transferhandler-Priorität, `onEndServerTick()` mit Start/Verteilung/Ende; kein Controller für Point-Versorgung erforderlich. |
| `TransferIterator` | `Iterator<TileFluxDevice>`; Point-Bedarf aus `getRequest()`, Plug-Verfügbarkeit aus `getBuffer()`. |
| `TransferHandler` | Geschützte Zyklus- und Pufferoperationen; Priorität -9999…9999, Surge 100000; deaktiviertes Limit `Long.MAX_VALUE`. |
| `NetworkStatistics` | Verarbeitet konkrete Tile-Listen; Point-Verbrauch als negatives `getTransferChange()`. |
| `TileFluxDevice` | Liefert Handler, Konfiguration, `markEnergyChanged()`, `disconnect()` und Blockupdates. |
| `TileFluxPoint` / `FluxPointHandler` | Typ POINT, Nachbarschaftstransfers; Hosted Point darf diese Nachbarschaftssuche nicht übernehmen. |
| `FluxConnectorHandler` | Verwalter der Seitentransfers normaler Blockgeräte; für die Batterie kein geeigneter Empfänger. |
| `PhantomFluxDevice` | Darstellungs-/Speicherabbild; `makeUnloaded` derzeit Tile-Parameter; Item-ID im bestehenden Tagformat möglich. |
| `FluxNetworkData` | Serverseitige Netzwerkdaten und Tickverwaltung; reguläre Netzwerke bleiben Energieverteiler. |
| `FluxChunkLoading` | Ticket-Wiederherstellung prüft `instanceof TileFluxDevice`; Batterie benötigt eigenen Controller. |
| `FluxDeviceConfigComponent` | Konfiguration normaler Flux-Items; Batterie braucht eigene Besitzer-/Identitätsdaten. |
| `FluxCapabilities` / `IFNEnergyStorage` | Native Long-FE-Itemfähigkeit; `receiveEnergyL`, `extractEnergyL`, gespeicherte und maximale Long-Energie. |

### Warteschlangenfehler

`enqueueConnectionRemoval` prüft zuerst die aktive ANY-Liste. Ein lediglich in `mToAdd` stehendes Gerät wird deshalb nicht entfernt. Der Bridge-Patch muss ausstehende Anmeldungen unabhängig von der aktiven Liste abbrechen und die positionsbasierte Karte nur bei passender Instanz entfernen.

## Architekturentscheidung

**Minimaler Flux-Fork auf Release 8.0.0 erforderlich.** Das unveränderte JAR bietet keinen zuverlässigen Registrierungseinstieg für ein Nicht-Tile. Die Alternative würde mehrere interne Listen, Iteratoren, Methodensignaturen und Statistikpfade durch Mixins ersetzen. Die Bridge soll `IFluxRuntimeDevice extends IFluxDevice` für Handler, Energieänderung und Abmeldung ergänzen. Bestehende Tiles implementieren sie; aktive Listen/Queues werden darauf umgestellt. `Messages` enthält zusätzlich einen `TileFluxDevice::sendBlockUpdate`-Aufrufer, der getrennt berücksichtigt werden muss. Position, Speicherformate und normale Gerätereihenfolge bleiben erhalten.

## Kompatibilitätsmatrix

| Komponente | Ausgangspunkt | Stand |
|---|---|---|
| Minecraft | 1.21.1 | MDK-Ziel |
| NeoForge | 21.1.231 | MDK-Version, noch kein Laufzeitnachweis |
| ModDevGradle | 2.0.141 | MDK-Version |
| Java | 21 | Toolchain-Download erforderlich; Host hat Java 25 |
| Kotlin / KFF | 2.1.21 / 5.9.0 | POM verifiziert; Starts noch offen |
| Mekanism | 10.7.19.85 | Laufzeit- und Sources-JAR heruntergeladen |
| Flux Networks | 8.0.0 + Bridge | Original-JAR heruntergeladen, Fork noch offen |

Die Matrix ist keine Laufzeitfreigabe. Build, Client und Dedicated Server werden in `test-results.md` getrennt erfasst.

## Bestätigte Mekanism-Einstiegspunkte (10.7.19.85 Sources-JAR)

- `TileEntityMekanism.tickServer(Level, BlockPos, BlockState, TileEntityMekanism)` ist der gemeinsame statische Serverpfad; er ruft `onUpdateServer()` auf. Ein gezielter Hook dort erreicht Maschinen unabhängig von geöffneten Menüs.
- `getInventorySlots(null)` liefert die zugänglichen Inventarslots; `BasicInventorySlot.getStack()` liest den aktuellen Stack. `onContentsChanged()` benachrichtigt den `IContentsListener`; `setStack` und normale Transfers rufen ihn ebenfalls auf.
- `EnergyInventorySlot.fillInsertCheck(ItemStack)` simuliert `IStrictEnergyHandler.extractEnergy(Long.MAX_VALUE, Action.SIMULATE) > 0`. Leere Batterien fallen regulär durch.
- `fill` und `fillOrConvert` verwenden die Negation derselben Prüfung für automatisierte Entnahme. Daher keine pauschale Änderung von `fillInsertCheck`.
- `fillContainerOrConvert()` / `fillContainer()` führen über `fillContainerFromItem()` erst Simulation, dann tatsächliche Entnahme und `onContentsChanged()` aus. `drainContainer()` ist die entgegengesetzte Richtung; nicht als Batteriehost zulassen.
- `TileEntityDigitalMiner` liegt unter `mekanism.common.tile.machine`; sein Energieslot wird mit `fillOrConvert` erstellt.
- `EnergyCompatUtils.getStrictEnergyHandler(ItemStack)` adaptiert vorhandene Item-Energiecapabilities; die FE-Konversion bleibt vollständig bei Mekanism.
- `IStrictEnergyHandler` führt Long-Energie mit `Action` und Restmengen bei Insert; dies ist nicht dieselbe Rückgabesemantik wie FE-Receive.

## Bestätigte Build-/Laufzeitkombination

Der unveränderte MDK-Grundbuild war mit Java 21 erfolgreich. Auch das Kotlin-Projekt kompiliert mit Kotlin 2.1.21, KFF 5.9.0, ModDevGradle 2.0.141 und Gradle 9.2.1. NeoForge 21.1.231 lädt Mekanism 10.7.19.85 und die Flux-Bridge auf dem Dedicated Server bis zur Done-Meldung. Der KFF-Clientsubscriber eines Kotlin-`object` benötigt Instanzmethoden, kein `@JvmStatic`. Statische Kotlin-Mixin-Injektionen funktionieren mit **privatem Companion**; durch Maschinen-GameTests praktisch bestätigt.

Mekanisms JAR-Dateiname enthält `.85`, seine Mod-Metadaten melden jedoch `10.7.19`. Die TOML-Abhängigkeit prüft diesen tatsächlichen Wert; die Build-Abhängigkeit bleibt exakt auf Artefakt 10.7.19.85 festgelegt.

Die Java-Quellbäume des aktuellen Flux-Zweigs und des Release-Commits wurden vollständig verglichen: keine Java-Differenzen. Der Branch unterscheidet sich in Buildskripten/Versionsdeklarationen. Der Abgleich mit dem veröffentlichten JAR umfasst die öffentlichen Signaturen der Bridge-Einstiegspunkte und die byteweise Erhaltung sämtlicher nicht ersetzter JAR-Einträge; es wird keine vollständige Bytecodeidentität von neu kompilierten Quellen behauptet.
