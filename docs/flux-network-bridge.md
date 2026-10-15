# Flux-Networks-Bridge

## Grundlage

Release-Commit **`45cf2480474782ecad66fb3c1215eb935fdb0d88`**, veröffentlichtes JAR **8.0.0**, CurseForge-Datei **6089446**. Das Eingabe-JAR wird anhand SHA-256 überprüft. Der spätere Branch-Commit `ea8b6e0…` ändert Buildskripte; ein vollständiger Vergleich der Java-Quellbäume ergab keine Java-Unterschiede zum Release. Maßgeblich bleiben trotzdem Release und geprüftes JAR.

Entscheidung: Ein isolierter Runtime-Mixin-Patch am Original wäre über mehrere konkrete Listen, Methodensignaturen, Iteratoren und Statistikpfade verteilt. Stattdessen wird ein kleiner Fork als reproduzierbare Klassenüberlagerung gebaut. Alle unveränderten Klassen und Ressourcen stammen unverändert aus dem veröffentlichten JAR. `scripts/verify-bridge.py` prüft das byteweise.

## Änderungen

- Neue Schnittstelle `IFluxRuntimeDevice extends IFluxDevice`: TransferHandler, Energieänderung, Disconnect; optionaler Blockupdate-Hook als leere Standardmethode.
- `TileFluxDevice` implementiert diese Schnittstelle mit seinen vorhandenen Methoden.
- `FluxNetwork`, `ServerFluxNetwork`, `ClientFluxNetwork`, `TransferIterator`, `NetworkStatistics`: aktive Typen auf Runtime-Schnittstelle verallgemeinert.
- `FluxNetwork` behält die alten Tile-Überladungen der An-/Abmeldung als Delegationsmethoden bei, damit unveränderte Flux-Klassen weiter verlinken.
- `PhantomFluxDevice.makeUnloaded` akzeptiert Runtime-Teilnehmer; die alte Tile-Überladung bleibt erhalten.
- `Messages`: Blockupdate-Aufruf bei Netzwerkfarbwechsel über den Runtime-Hook. Hosted Points ändern keinen Maschinenblock.
- Warteschlangen: ausstehende Anmeldung lässt sich vor Aktivierung abbrechen. Positionskarten werden bei Entfernung nur bei identischer Instanz geändert. Eine andere aktive Runtime-Instanz an derselben Position wird nicht überschrieben.
- Netzwerklöschung berücksichtigt auch noch nicht aktivierte Teilnehmer; Listen werden geleert und spätere Anmeldungen abgelehnt. Dadurch bleibt ein beim ersten Zyklus gelöschtes Netzwerk lebenszyklussicher.
- Summenbedarf und nichtnegative Energiestatistiken sättigen bei Long.MAX_VALUE. Dies verhindert Überlauf durch mehrere Batterien mit maximaler Kapazität; normale Werte bleiben unverändert.

Der genaue Quellpatch liegt unter `bridge/patches/fluxnetworks-8.0.0-runtime-device.patch`. Die ersetzten Quellen liegen unter `bridge/src/main/java/`; Upstream-Lizenz unter `bridge/LICENSE`.

## Unverändert

Mod-ID `fluxnetworks`, Version in der Mod-Metadatei `8.0.0`, IDs, Netzwerkdatenformate, normale Geräte, Transferreihenfolge, Surge- und Prioritätsberechnung, Energieadapter und Flux-Chunk-Tickets. Das Artefakt trägt zur Unterscheidung den Dateinamenszusatz `fluxbattery.1`.

Das Addon verwendet eigene persistente Komponenten und einen eigenen Chunk-TicketController. Es erzeugt keine zusätzliche BlockEntity und keinen getarnten Flux-Point-Block. Verbindungsvorschauen verwenden das vorhandene Phantom-Tagformat und `fluxbattery:flux_battery` als Anzeigeitem.

## Bauen

```bash
python3 scripts/bootstrap-dependencies.py
./gradlew build
python3 scripts/verify-bridge.py
```

`patchedFlux` kompiliert ausschließlich die ersetzten Java-Klassen gegen das überprüfte Original und die Minecraft-/NeoForge-API. Das Ergebnis wird nach `build/bridge/` geschrieben. Zipreihenfolge und Zeitstempel sind normalisiert. Original und Patch werden niemals gemeinsam auf den Laufzeitklassenpfad gelegt.

Addons, die selbst `getLogicalDevices()` iterieren und jeden Teilnehmer auf `TileFluxDevice` casten, benötigen gegebenenfalls eine Anpassung an die neue Schnittstelle. Diese externe Kompatibilität ist nicht durch die Tests normaler Flux-Geräte abgedeckt.
