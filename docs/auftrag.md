# Codex-Arbeitsauftrag: Flux Battery für Minecraft 1.21.1

## Projektziel

Entwickle eine eigenständige NeoForge-Mod namens **Flux Battery**, mit der ein konfigurierbares Batterieitem im Energieslot einer Mekanism-Maschine als virtueller Flux Point fungieren kann.

Das Item soll sämtliche für diesen Zweck relevanten Eigenschaften eines normalen Flux Points besitzen. Insbesondere:

- Verbindung zu einem ausgewählten Flux-Netzwerk
- Berechtigungs- und Besitzerbindung
- Energieempfang aus dem Netzwerk ohne Flux Controller
- Priorität und Surge-Modus
- Transferlimit und dessen Deaktivierung
- Benutzerdefinierter Name
- Optionales Chunkloading
- Darstellung als Verbindung in Flux Networks
- Persistenz der Konfiguration beim Entfernen und Versetzen

Die Maschine darf nicht dauerhaft verändert werden. Sämtliche Zusatzfunktionen sind an die Anwesenheit der Batterie im Energieslot gebunden.

**Wichtig: Die neue Mod ist vollständig in Kotlin mit Kotlin for Forge zu entwickeln.**

### Verbindliche Versionen

- Minecraft: 1.21.1
- NeoForge: 21.1.x, mit einer Version, die die erforderlichen Mekanism- und KFF-Versionen unterstützt
- Java-Toolchain: 21
- Kotlin: zunächst 2.1.21
- Kotlin for Forge: zunächst 5.9.0
- Mekanism: 10.7.19.85
- Flux Networks: 8.0.0
- Gradle: zur verwendeten NeoForge-ModDevGradle-Version kompatibler Wrapper

KFF 5.9.0 ist der erste zu testende Ausgangspunkt. Die endgültige Kombination ist durch einen erfolgreichen Client- und Dedicated-Server-Start zu bestätigen. Verwende nicht blind die KFF-6.x-Anleitung.

Der bestehende Java-Quellcode von Flux Networks darf für einen notwendigen, kleinen Kompatibilitätspatch Java bleiben. Alle neu entwickelten Klassen der Flux-Battery-Mod müssen in Kotlin geschrieben werden.

---

## Phase 0: Repository- und API-Analyse

Untersuche vor der Implementierung das vorhandene Projekt. Wenn bereits eine Flux-Battery-Mod existiert, erweitere diese, statt ein paralleles Projekt zu erstellen.

Prüfe insbesondere:

**Flux Networks, Branch `1.21`:**

- `ServerFluxNetwork`
- `FluxNetwork`
- `TransferIterator`
- `TransferHandler`
- `NetworkStatistics`
- `TileFluxDevice`
- `TileFluxPoint`
- `FluxPointHandler`
- `FluxConnectorHandler`
- `PhantomFluxDevice`
- `FluxNetworkData`
- `FluxChunkLoading`
- `FluxDeviceConfigComponent`
- `FluxCapabilities`
- `IFNEnergyStorage`

Quellcode: https://github.com/SonarSonic/Flux-Networks

Vergleiche den Quellcodestand mit dem tatsächlich veröffentlichten JAR 8.0.0. Der GitHub-Zweig kann bereits abweichende Entwicklungsänderungen enthalten. Patch und Addon müssen gegen denselben definierten Stand gebaut werden.

**Mekanism, Branch `1.21.x`:**

- `EnergyInventorySlot`
- `BasicInventorySlot`
- `TileEntityMekanism`
- `TileEntityDigitalMiner`
- `EnergyCompatUtils`
- `IStrictEnergyHandler`

Quellcode: https://github.com/mekanism/Mekanism

Ermittle alle relevanten Einstiegspunkte für Item-Capabilities, Energieslot-Zustandsänderungen, Maschinenticks und Persistenz.

Halte die bestätigten Methoden und Klassen in `docs/api-analysis.md` fest.

**Abnahme:** Eine nachvollziehbare Kompatibilitätsmatrix, ein erfolgreicher Grundbuild und eine klare Entscheidung, ob die Flux-Integration mit einem isolierten Patch am unveränderten JAR zuverlässig möglich ist oder ein kleiner Flux-Networks-Fork benötigt wird.

## Phase 1: Kotlin-Projekt einrichten

Verwende Kotlin DSL (`build.gradle.kts`) und das für Minecraft 1.21.1 geeignete NeoForge-ModDevGradle.

Richte Kotlin for Forge entsprechend der 5.x-Dokumentation ein:

```toml
modLoader="kotlinforforge"
loaderVersion="[5.9,6)"
```

Die KFF-Abhängigkeit lautet für die gewählte Ausgangsversion:

```kotlin
implementation(
    "thedarkcolour:kotlinforforge-neoforge:5.9.0"
)
```

Mekanism und Flux Networks werden als feste Mod-Abhängigkeiten behandelt. Für die Entwicklungsumgebung müssen die tatsächlichen Laufzeit-JARs und ihre erforderlichen Abhängigkeiten verfügbar sein.

Erstelle die Kotlin-Mod-Hauptklasse, Itemregistrierung, Data-Component-Registrierung, Attachment-Registrierung, Clientregistrierung und Konfiguration.

Verhindere, dass clientseitige Klassen auf einem Dedicated Server geladen werden.

**Abnahme:** `runClient`, `runServer` und `build` funktionieren mit der gewählten Abhängigkeitskombination.

## Phase 2: Das Flux-Battery-Item

Erstelle `FluxBatteryItem.kt`.

Das Item ist nicht stapelbar und besitzt einen persistenten Energiepuffer.

Registriere mindestens:

- `Capabilities.EnergyStorage.ITEM` für Standard-FE
- `FluxCapabilities.ITEM` für die native Long-Energie-API von Flux Networks

Beide Fähigkeiten greifen auf **denselben** Energiespeicher zu.

Verwende keine separaten FE- und Flux-Energiebestände und keine unkontrollierten Konvertierungsfaktoren. Der Itemspeicher wird intern als nicht negativer `Long`-Wert in FE geführt.

### Persistente Itemdaten

Definiere Data Components für:

`BatteryIdentity`
- Eindeutige Iteminstanz-ID zur Lebenszyklusverfolgung

`BatteryConfig`
- networkId
- ownerUuid
- customName
- priority
- transferLimit
- disableTransferLimit
- surgeMode
- chunkLoadingEnabled

`BatteryEnergy`
- storedEnergy
- capacity

Die Namen sind Arbeitsbezeichnungen. Definiere bei Bedarf eine sinnvolle gemeinsame Konfigurationskomponente und getrennte Energiekomponente.

Nutze einen persistenten Codec und geeignete Stream-Codecs für tatsächlich clientseitig benötigte Werte.

Alle Änderungen am Item müssen über einen zentralen Zustandsdienst erfolgen. Verhindere negative Werte, Long-Überläufe und Energieerzeugung durch Simulationen.

### Energieverhalten

- `receiveEnergy` erhöht den Itempuffer, soweit Kapazität frei ist.
- `extractEnergy` verringert den Itempuffer, soweit Energie vorhanden ist.
- Simulation verändert niemals den Zustand.
- Die Energieeinheit wird gegenüber Mekanism ausschließlich über dessen vorhandene Energieadapter umgesetzt.
- Bei einer Zustandsänderung innerhalb eines Mekanism-Slots wird dessen Änderungsmechanismus korrekt ausgelöst.

### Mekanism-Slot-Kompatibilität

Eine vorgeladene Batterie muss von `EnergyInventorySlot.fillInsertCheck` erkannt und von `fillContainerOrConvert` regulär entladen werden.

Untersuche anschließend, wie auch eine vollständig leere, bereits konfigurierte Batterie eingesetzt werden kann.

**Wichtig:** Eine pauschale Änderung von `fillInsertCheck` kann zugleich die Extraktionsbedingungen beeinflussen. Implementiere deshalb keinen scheinbar entnehmbaren Energiewert und keine globale Umgehung der Slotvalidierung.

Falls ein spezieller Einfüge-Hook nötig ist, beschränke ihn strikt auf das Flux-Battery-Item und prüfe die Entnahme bei leerem, teilweise geladenem und vollem Puffer.

**Abnahme:** Die Batterie kann geladen, gespeichert, entladen, aus dem Slot entfernt und erneut eingesetzt werden. Kein Energieverlust oder Energiegewinn durch Kopieren von Handlern, Speichern oder Simulation.

## Phase 3: Temporäre Verbindung zur Mekanism-Maschine

Implementiere ein `FluxPointAttachment`, das an eine fremde BlockEntity gebunden werden kann.

Verwende NeoForge Data Attachments für die Maschinenanbindung. Laufzeitreferenzen werden nicht persistent serialisiert.

Die persistenten Einstellungen verbleiben im ItemStack.

### Vorgeschlagene Klassen

- `FluxBatteryHostManager.kt`
- `FluxPointAttachment.kt`
- `MekanismHostAdapter.kt`
- `HostKey.kt`
- `BatteryHostLifecycle.kt`

Ein `HostKey` identifiziert den Host anhand von:

- Serverinstanz
- Dimension
- Blockposition
- Energieslot-Index

Der registrierte Laufzeitzustand enthält zusätzlich die Batterieinstanz-ID und einen Zugriff auf den aktuellen ItemStack.

**Speichere keine dauerhaft gültige Referenz auf einen ItemStack, der inzwischen aus dem Slot entfernt oder durch eine Kopie ersetzt worden sein könnte.**

### Registrierung

Beim erstmaligen Vorfinden einer Batterie im unterstützten Energieslot:

1. Prüfe, ob die Maschine aktiv als BlockEntity in der Welt existiert.
2. Lies die Batterie und ihre Konfiguration.
3. Erzeuge das Attachment beziehungsweise reaktiviere den vorhandenen Laufzeitzustand.
4. Prüfe die Netzwerkberechtigung.
5. Registriere einen virtuellen Flux Point im passenden Flux-Netzwerk.
6. Aktiviere optional Chunkloading.

### Abmeldung

Wird die Batterie entfernt, der Slot ersetzt, die Maschine abgebaut oder die Konfiguration geändert:

1. Stoppe neue Transfers.
2. Schließe bereits begonnene Transfers konsistent ab.
3. Schreibe alle zustandsrelevanten Energiedaten in den ItemStack.
4. Entferne den Teilnehmer aus dem bisherigen Netzwerk.
5. Entferne bei tatsächlicher Entfernung das Chunk-Ticket.
6. Löse die Laufzeitbindung an die Maschine.

Der Vorgang muss mehrfach aufrufbar sein, ohne Fehler oder doppelte Energiebewegungen zu verursachen.

### Integration in Mekanism

Verwende nach Möglichkeit den gemeinsamen Server-Tickpfad von `TileEntityMekanism`.

Die Energieslots können über `getInventorySlots(null)` ermittelt und nach `EnergyInventorySlot` gefiltert werden.

Bevorzuge einen gezielten Hook im gemeinsamen Maschinentick und ergänzende Lebenszyklusereignisse gegenüber individuellen Änderungen an jeder Maschine.

Implementiere zunächst den Digital Miner vollständig. Stelle sicher, dass die Architektur danach weitere Mekanism-Maschinen über denselben Adapter unterstützen kann.

Ein Maschinenwechsel darf keine Registrierung am alten Standort zurücklassen.

**Abnahme:** Die Batterie wird beim Einsetzen genau einmal angemeldet und beim Herausnehmen genau einmal wirksam abgemeldet. Alle Vorgänge funktionieren auch ohne geöffnetes GUI.

## Phase 4: Native Flux-Networks-Integration

Das ist die wichtigste Architekturphase.

Die bestehende Netzwerkimplementierung verarbeitet aktive Teilnehmer überwiegend als `TileFluxDevice`.

Für die gewünschte Funktion muss das Netzwerk einen virtuellen Teilnehmer akzeptieren können, ohne dass dieser eine eigene Welt-BlockEntity besitzt.

### Zielarchitektur

Führe im Flux-Networks-Bridge-Bereich eine Schnittstelle wie `IFluxRuntimeDevice` ein.

Sie soll `IFluxDevice` erweitern und zusätzlich die für die Energieverteilung nötigen Methoden enthalten:

```java
TransferHandler getTransferHandler();
void markEnergyChanged();
void disconnect();
```

Ergänze nur diejenigen Funktionen, die nach der tatsächlichen API-Analyse zusätzlich erforderlich sind.

Passe im kleinsten möglichen Umfang an:

- `FluxNetwork`
- `ServerFluxNetwork`
- `TransferIterator`
- `NetworkStatistics`
- `PhantomFluxDevice` beziehungsweise dessen Erzeugung
- Alle weiteren direkt von konkreten `TileFluxDevice`-Typen abhängigen Aufrufer

`TileFluxDevice` implementiert die neue Laufzeitschnittstelle, sodass bestehende Flux-Geräte unverändert weiterarbeiten.

Der Batterieadapter implementiert die Laufzeitschnittstelle als Kotlin-Klasse `HostedFluxPoint`.

### Anforderungen an den HostedFluxPoint

- Gerätetyp `POINT`
- Position entspricht der Maschinenposition
- Konfiguration stammt aus dem Batterie-Item
- Transferhandler ist mit der Batterie verbunden
- Kein eigenes BlockState-Management
- Keine Änderung des Maschinenblocks
- Keine Nachbarschaftssuche wie beim normalen Flux Point
- Keine künstliche Registrierung als BlockEntity
- Erscheint in der Netzwerk-Verbindungsübersicht mit dem Batterieitem als Anzeige

### Netzwerkregistrierung

Verwende die regulären Warteschlangen von Flux Networks für Hinzufügen und Entfernen.

Berücksichtige auch diesen Sonderfall:

Eine Batterie wird innerhalb desselben Ticks eingesetzt und wieder entfernt, bevor die Warteschlange verarbeitet wurde.

In diesem Fall darf kein dauerhaft registrierter Teilnehmer übrig bleiben.

Implementiere die Registrierung und Abmeldung serverseitig und idempotent.

### Einschränkung für die erste Version

Erlaube genau einen aktiven virtuellen Flux Point pro Maschinenposition.

Sollte eine Maschine mehrere passende Batterieslots enthalten, definiere eine deterministische Auswahl. Die übrigen Batterien dürfen reguläre Energieitems bleiben, aber nicht dieselbe Netzwerkposition überschreiben.

Dies ist notwendig, weil die bisherige Flux-Verbindungsverwaltung Positionen als Schlüssel verwendet.

### Core-Patch oder Mixin

Prüfe zunächst, ob sich die Erweiterung ohne vollständigen Fork mit einem kleinen, stabilen Patch realisieren lässt.

Wenn stattdessen viele Methoden oder interne Listen durch Mixins ersetzt werden müssten, verwende einen minimalen Fork von Flux Networks.

Bei einem Fork:

- Erhalte die Mod-ID `fluxnetworks`.
- Erhalte vorhandene Speicherformate und IDs.
- Erhalte normales Verhalten aller bisherigen Geräte.
- Verändere keine bestehenden Energie- oder Netzwerkregeln ohne Notwendigkeit.
- Dokumentiere exakt die Unterschiede zum Original.
- Baue das Patch-JAR reproduzierbar.
- Lade niemals Original und Fork gleichzeitig.

Ein Fork darf kein Vorwand für ein allgemeines Refactoring von Flux Networks werden.

**Abnahme:** Die Batterie erscheint als Point in der Flux-Verbindungsübersicht. Sie erhält Energie nach derselben Netzwerkverteilungslogik wie andere Points. Normale Flux Points, Plugs, Controller und Storages bleiben unverändert funktionsfähig.

## Phase 5: Energieübertragung

Implementiere `HostedPointTransferHandler.kt` als spezialisierte Transferlogik für den virtuellen Point.

Sie muss die normalen Phasen des Flux-Netzwerks unterstützen:

1. Bedarfsermittlung zu Beginn des Netzwerkzyklus
2. Energiezuteilung anhand der Netzwerkprioritäten
3. Tatsächlicher Energieempfang
4. Verbuchung und Statistik
5. Abschluss des Transferzyklus

Anders als beim normalen Flux Point wird keine benachbarte BlockEntity mit Energie versorgt.

Der Empfänger ist der Energiepuffer der Batterie im Maschinen-Energieslot.

Mekanism entnimmt anschließend über seine reguläre Energieschnittstelle die benötigte Energie aus der Batterie.

**Es darf keinen zweiten direkten Energiepfad in den Maschinenpuffer geben.**

### Übertragungsinvarianten

- Der Netzwerkbedarf darf die freie Batteriekapazität und das Transferlimit nicht überschreiten.
- Bereits zugeteilte, noch nicht abgeschlossene Energiemengen müssen berücksichtigt werden.
- Die tatsächlich aus dem Netzwerk entnommene Energie muss genau einer tatsächlichen Gutschrift entsprechen.
- Simulationen dürfen weder Statistik noch Energiezustand verändern.
- Ein Entfernen des Items während eines Transferzyklus darf Energie weder vernichten noch duplizieren.
- Zwischenpuffer dürfen nicht beim Chunk-Unload verschwinden.
- `Long.MAX_VALUE` und andere Grenzwerte dürfen keine Überläufe auslösen.
- Priorität, Surge und deaktivierte Limits müssen denselben Regeln folgen wie normale Flux Points.

Nutze nach Möglichkeit die vorhandene `TransferHandler`-Logik, statt einen unabhängigen Energieverteiler zu implementieren.

Bei Verbindungen mit wenig verfügbarer Energie ist die normale Flux-Reihenfolge zu respektieren. Die Batterie darf nicht heimlich sämtliche verfügbaren Energieeinheiten bevorzugt erhalten.

**Abnahme:** Energiefluss und Statistik stimmen bei normalem Verbrauch, mehreren Verbrauchern, Transferlimits, voller Batterie und knapper Netzwerkenergie überein.

## Phase 6: Eigenständiges Chunkloading

Erstelle `BatteryChunkTicketController.kt`.

Verwende einen eigenen NeoForge `TicketController` mit einer eigenen Namespace-ID.

Versuche nicht, die vorhandenen Flux-Networks-Tickets unverändert für Mekanism-Maschinen zu verwenden.

Der Grund: Die Wiederherstellung der normalen Flux-Tickets erwartet eine `TileFluxDevice` an der Ticketposition.

### Chunkloading-Regeln

Ein Chunk-Ticket darf nur existieren, wenn:

- Die Serverkonfiguration Chunkloading erlaubt.
- Eine unterstützte Maschine an der Position existiert.
- Der richtige Energieslot eine gültige Flux-Batterie enthält.
- Die Chunkloading-Option im Batterieitem aktiviert ist.
- Eine weiterhin zulässige Netzwerkbindung besteht.

Beim Einsetzen darf das Ticket entsprechend diesen Bedingungen erstellt werden.

Beim Herausnehmen, Maschinenabbau oder Deaktivieren muss es unmittelbar entfernt werden.

Beim regulären Chunk-Unload ohne aktives Chunk-Ticket wird nur die Laufzeitregistrierung beendet. Die Itemkonfiguration bleibt bestehen.

### Serverneustart

Der Ticket-Validierungscallback muss prüfen, ob an der Position weiterhin eine passende Maschine mit Batterie existiert.

Er darf Tickets nicht allein deshalb wiederherstellen, weil dort beim vorherigen Spielstand einmal eine Batterie lag.

Implementiere auch die korrekte Behandlung von:

- Dimensionswechsel
- Maschinenabbau
- Entferntem Energieslotinhalt
- Gelöschtem Flux-Netzwerk
- Ungültiger Netzwerkberechtigung
- Deaktiviertem Chunkloading in der Serverkonfiguration

Beachte bestehende Serverregeln zu Ticket-Quoten und Chunkloading. Die Batterie darf keine Umgehung von Serverbeschränkungen ermöglichen.

**Abnahme:** Der Digital Miner arbeitet mit aktivierter Option auch ohne Spieler in der Nähe weiter. Nach Entfernen der Batterie oder Abbau der Maschine bleibt kein Chunk-Ticket zurück. Das Verhalten bleibt nach einem vollständigen Serverneustart korrekt.

## Phase 7: Konfigurationsoberfläche

Implementiere eine eigene Kotlin-Oberfläche für die Batterie.

Das Item kann beispielsweise durch Shift-Rechtsklick geöffnet werden, solange es vom Spieler gehalten wird.

Die GUI soll enthalten:

| Einstellung | Funktion |
|---|---|
| Flux-Netzwerk | Zugängliches Netzwerk auswählen |
| Batterie-Name | Individueller Anzeigename |
| Priorität | Normaler Flux-Prioritätswert |
| Transferlimit | FE pro Netzwerkzyklus gemäß Flux-Regeln |
| Unbegrenztes Limit | Begrenzung deaktivieren |
| Surge | Vorrangmodus aktivieren |
| Chunkloading | Chunkloading ein-/ausschalten |
| Energie | Aktueller Inhalt und Kapazität |
| Verbindungsstatus | Verbunden, getrennt, ungültig oder nicht berechtigt |

Die Werte müssen nach einem Weltneustart und beim Wechsel zwischen Maschinen erhalten bleiben.

### Sicherheit

Die Oberfläche darf ausschließlich Anfragen an den Server stellen.

Der Server muss:

- Den aktuell bearbeiteten ItemStack erneut identifizieren.
- Die Netzwerk-ID prüfen.
- Die Rechte des Spielers prüfen.
- Transfergrenzen validieren.
- Einstellungen erst nach erfolgreicher Prüfung übernehmen.
- Keine frei angegebenen Item- oder Maschinenpositionen ohne Besitz- und Zugriffsprüfung akzeptieren.

Speichere keine Netzwerkpasswörter im Item.

Für private Netzwerke soll die erste Version eine gültige Besitzer- oder Mitgliedschaftsbindung verlangen. Eine reine einmalige Passwortfreigabe ist keine ausreichende dauerhafte Berechtigung.

Beim Ändern des Netzwerks während einer aktiven Maschinenverbindung muss der alte Point abgemeldet und anschließend unter der neuen Berechtigung angemeldet werden.

**Abnahme:** Die Konfiguration funktioniert im Einzelspieler und auf einem Dedicated Server. Manipulierte Clientpakete können weder fremde Netzwerke verwenden noch die Itemenergie erhöhen.

## Phase 8: Allgemeine Mekanism-Unterstützung

Nachdem der Digital Miner funktioniert, erweitere denselben Adapter auf andere Mekanism-Maschinen mit passenden `EnergyInventorySlot`-Instanzen.

Keine individuelle Kopie des Netzwerkcodes für jede Maschinenklasse.

Der HostManager muss anhand des tatsächlichen Slottyps und des enthaltenen Items entscheiden können, ob eine Flux-Battery-Verbindung zulässig ist.

Berücksichtige, dass nicht jeder Energieslot ein Entladeslot ist. Eine Batterie darf nur dort als Energiequelle fungieren, wo die Maschine Energie aus Items entnehmen kann.

Unterstütze zuerst gängige Einzelblockmaschinen. Komplexe Multiblockstrukturen werden nur aufgenommen, wenn die Zuständigkeit für Position, Energieslot und Speicher eindeutig geklärt ist.

**Abnahme:** Mindestens Digital Miner und zwei weitere geeignete Mekanism-Maschinen funktionieren mit derselben Implementierung.

## Phase 9: Automatisierte Tests

Erstelle Tests für die folgenden Gruppen.

### Batterie und Energie

- Leere, teilweise geladene und volle Batterie
- Simulation von Laden und Entladen
- Standard-FE und Flux-Long-Energie
- Werte oberhalb von `Integer.MAX_VALUE`
- Kapazitätsgrenzen und Long-Überläufe
- Persistenz von Energie und Konfiguration
- Keine Energieverdopplung durch Simulation

### Netzwerk

- Verbindung zu gültigem Netzwerk
- Ungültige oder gelöschte Netzwerk-ID
- Wechsel zwischen zwei Netzwerken
- Mehrere Batterien in verschiedenen Maschinen
- Einfügen und Entfernen innerhalb eines Ticks
- Prioritäten, Surge-Modus und Transferlimits
- Funktion ohne Flux Controller
- Korrekte Netzwerkstatistik
- Keine verwaisten Netzwerkteilnehmer

### Maschinen

- Digital Miner bei laufendem Betrieb
- Maschine mit vollem Energiepuffer
- Entnahme der Batterie bei 0 FE
- Entnahme während der Energieübertragung
- Verschieben der Batterie zwischen Maschinen
- Maschinenabbau
- Chunk-Unload und erneutes Laden
- Serverneustart

### Sicherheit und Chunkloading

- Nicht berechtigter Spieler
- Netzwerkberechtigung entzogen
- Manipuliertes Konfigurationspaket
- Chunkloading serverseitig deaktiviert
- Batterie herausgenommen bei aktivem Ticket
- Maschine abgebaut bei aktivem Ticket
- Gültiges Ticket nach Serverneustart wiederhergestellt

### Kompatibilität

Teste auf einem Dedicated Server mit Mekanism, Flux Networks, Kotlin for Forge und deren notwendigen Abhängigkeiten.

Überprüfe besonders, dass normale Flux-Geräte unverändert funktionieren.

Automatisierte GameTests sollen nach Möglichkeit die Energieerhaltung und den Lebenszyklus abdecken. Wo die Integration nicht zuverlässig automatisiert geprüft werden kann, dokumentiere die manuellen Testschritte mit erwartetem Ergebnis.

## Phase 10: Abschluss und Dokumentation

Erstelle:

- `README.md` mit Installation und Bedienung
- `docs/api-analysis.md`
- `docs/architecture.md`
- `docs/flux-network-bridge.md`
- `docs/energy-invariants.md`
- `docs/chunkloading.md`
- `docs/test-results.md`

Dokumentiere, ob die Mod mit dem unveränderten Flux-Networks-JAR funktioniert oder ein Patch-JAR voraussetzt.

Verwende getrennte, nachvollziehbare Commits für Projektgrundlage, Batterie, Hostverwaltung, Netzwerk-Bridge, Chunkloading, Oberfläche und Tests.

Am Ende ist ein überprüfbarer Entwicklungsbericht erforderlich:

1. Implementierte Komponenten
2. Geänderte Quelldateien
3. Erforderliche Mod-Versionen
4. Build- und Testresultate
5. Noch offene Einschränkungen
6. Installationsanleitung
7. Genauer Stand der Flux-Networks-Kompatibilität

Eine erfolgreich kompilierte Mod ist nicht automatisch eine erfolgreich getestete Mod. Stelle beide Zustände getrennt dar.

## Verbindliche Architekturregeln

1. Die neue Mod wird in Kotlin entwickelt.
2. Kotlin for Forge ist eine verpflichtende Laufzeitabhängigkeit.
3. Minecraft 1.21.1 und NeoForge sind das Ziel.
4. Kein vorgetäuschter Flux-Point-Block und keine zusätzliche Welt-BlockEntity.
5. Die vorhandene Mekanism-BlockEntity bleibt Eigentümerin ihres Energieslots.
6. Die Batterie ist Eigentümerin ihrer Konfiguration und ihrer Energie.
7. Das Attachment verwaltet nur die temporäre Bindung an den Maschinenstandort.
8. Flux Networks verwaltet weiterhin die eigentliche Energieverteilung.
9. Chunkloading wird über reguläre NeoForge-Tickets implementiert.
10. Entfernen und Wiederherstellen sind idempotent und energieerhaltend.
11. Keine eigenständige parallele Flux-Netzwerksimulation.
12. Keine manuellen NBT-Manipulationen an Fremdmods, wenn reguläre APIs vorhanden sind.
13. Keine umfangreichen, nicht begründeten Refactorings an Mekanism oder Flux Networks.
14. Kein Erfolgsmeldung ohne Build- beziehungsweise Laufzeittest.

## Definition of Done

Das Projekt ist abgeschlossen, wenn eine konfigurierte Flux-Batterie im Energieslot eines Digital Miners steckt, sich als Point mit einem autorisierten Flux-Netzwerk verbindet und den Miner ohne zusätzlichen Flux Point oder Flux Controller mit Energie versorgt.

Das optionale Chunkloading hält den Maschinenchunk geladen.

Wird die Batterie herausgenommen, verschwinden die aktive Netzwerkverbindung und das Chunk-Ticket. Nach dem Einsetzen in eine andere geeignete Mekanism-Maschine funktionieren dieselben Eigenschaften mit der gespeicherten Konfiguration erneut.

Alle relevanten Energie- und Lebenszyklustests sind erfolgreich. Das System funktioniert auf einem Dedicated Server.

Beginne mit Phase 0, gehe anschließend in der festgelegten Reihenfolge vor und dokumentiere technische Abweichungen unmittelbar. Setze nicht stillschweigend auf eine vereinfachte Controller-Lösung zurück, da das Ziel ausdrücklich ein virtueller Flux Point ist.