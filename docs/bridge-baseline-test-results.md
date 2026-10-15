# Build- und Testbericht — 15.10.2026

## Ergebnis

**Implementiert, gebaut und in der Entwicklungsumgebung auf Client und Dedicated Server gestartet.** Der Digital Miner hat in einem automatisierten GameTest tatsächlich Eisenerz abgebaut, gespeist durch einen regulären Flux Plug und die Batterie. Die vollständige manuelle Multiplayer-/Modpack-Abnahme ist noch offen; eine pauschale Freigabe aller Drittmodkombinationen wird nicht behauptet.

Getestete Kombination: Minecraft 1.21.1, NeoForge 21.1.231, Java 21 (Temurin 21.0.12.1), Kotlin 2.1.21, KFF 5.9.0, Mekanism-Artefakt 1.21.1-10.7.19.85, Flux Networks 8.0.0 mit der enthaltenen Bridge, Gradle 9.2.1, ModDevGradle 2.0.141.

## Ausgeführte Prüfungen

| Prüfung | Ergebnis / Umfang |
|---|---|
| Ursprünglicher MDK-Grundbuild | Erfolgreich, `BUILD SUCCESSFUL` |
| Kotlin-Mod und Bridge, `build` | Erfolgreich |
| JUnit | **3 Tests erfolgreich**, darunter 10.000 deterministische Erhaltungsfälle |
| Dedicated Server, `runServer` | Bis **Done** gestartet; anschließend bewusst mit SIGINT beendet (Gradle meldet deshalb Exit 130) |
| `runGameTestServer` | **12 erforderliche GameTests erfolgreich** |
| Neustart, separate JVM „prepare“ | 11 Tests des damaligen Stands erfolgreich, gültiges und verwaistes Ticket gespeichert |
| Neustart, separate JVM „verify“ | 11 Tests des damaligen Stands erfolgreich, Persistenz/Wiederherstellung/Entnahme geprüft |
| Client und Oberfläche | Modinitialisierung, Texturatlanten und gerenderte Konfigurationsvorschau erfolgreich; automatisches Beenden |
| JAR-Inhalt | Nicht ersetzte Upstream-Klassen und Ressourcen byteweise unverändert; keine doppelten ZIP-Einträge |

Die zwölf GameTests:

1. Gemeinsamer FE-/Long-Speicher, Simulation ohne Komponentenänderung, Werte oberhalb Int.MAX_VALUE, Handlerkopien, ItemStack-Speicherung und Wiederherstellung.
2. Batterie vor Verarbeitung der ersten Netzwerkanmeldung wieder entfernen: keine Verbindung und kein aktiver Point zurückgelassen.
3. Digital Miner, Enrichment Chamber und Energized Smelter über denselben Adapter versorgen und über reguläre Mekanism-Slots entladen; leere Batterie entnehmen.
4. Surge, Priorität, Transferlimits und knappe Plug-Energie; tatsächliche Gutschriften und negative Point-Verbrauchsstatistik.
5. Unberechtigte private Bindung ablehnen, berechtigte zulassen, Netzwerklöschung löst Attachment.
6. Eigenes tickendes Chunk-Ticket erzeugen, bei Entnahme entfernen und bei Serverdeaktivierung nicht weiterführen.
7. Batterie zwischen Maschinen verschieben; Energie/ID erhalten, Standort und Netzwerk wechseln; Maschinenabbau hinterlässt keinen aktiven Point.
8. Netzwerk löschen, solange der Point nur in der Anmeldequeue steht.
9. **Arbeitender Digital Miner:** regulärer Plug → Batterie → regulärer Mekanism-Energieslot → Eisenerzabbau und Raw-Iron-Ausgabe, ohne Point/Controller am Miner.
10. Normale Flux Points, Plugs, Controller und Basic Storage: Anmeldung, Verteilung, Statistik und Abmeldung.
11. Server-Konfigurationsdienst: falsche Batterie-ID, ungültige Werte, unbekanntes/fremdes privates Netzwerk und fremder Batterie-Besitzer abgelehnt; gültige Änderung verändert Energie nicht, gefälschter Besitzer wird ignoriert.
12. Zwei maximale Long-Bedarfe: Summenbedarf sättigt, Long.MAX_VALUE wird exakt zugeteilt, volle Batterie fordert nichts mehr an und lässt sich voll entnehmen.

Der Zweiprozess-Neustarttest ergänzt diese Tests: Batterie mit 987654 FE und voller Maschine speichern, Server vollständig beenden, neue JVM starten, Energie/Name erhalten, gültiges Ticket wiederherstellen, verwaistes Ticket verwerfen und wiederhergestelltes Ticket nach Entnahme löschen.

## Reproduktion

```bash
python3 scripts/bootstrap-dependencies.py
./gradlew build
./gradlew runGameTestServer
python3 scripts/test-restart.py
./gradlew runClient -PclientSmoke
python3 scripts/verify-bridge.py
```

Für den lokalen Linux-Client musste nach einem sporadischen Fehler der NeoForge-Frühfensterübergabe `earlyWindowControl=false` in **run-client/config/fml.toml** gesetzt werden. Das ändert keine Mod-Abhängigkeiten. Der Screenshot ist eine synthetische GUI-Vorschau, kein Nachweis einer echten Netzwerkverbindung. Der Dedicated-Server-Test verifiziert die gemeinsame Modinitialisierung ohne Clientklassen.

Die anfänglichen Fehler (Mekanism-Metadatenversion, öffentliche Kotlin-Mixin-Companions, statischer KFF-Object-Subscriber) wurden behoben. Gradles erfolgreicher Taskstatus wurde nicht als ausreichender Laufzeitnachweis verwendet: ein früher Modladefehler beendete `runServer` trotz `BUILD SUCCESSFUL` ohne funktionierenden Server.

## Noch manuell zu prüfen

| Ablauf | Erwartetes Ergebnis |
|---|---|
| Zwei echte Spieler auf Dedicated Server; Shift-Rechtsklick, alle Felder ändern, speichern und neu öffnen | Server übernimmt nur berechtigte Änderungen; Client zeigt gespeicherte Werte |
| Dieselbe Konfiguration im Einzelspieler | Identisches Verhalten; keine Client-/Server-Abweichung |
| Flux-Konfigurationsübersicht öffnen | Batterie als POINT mit Batterieitem, Maschinenposition, Name, Limit und Priorität sichtbar |
| Privatnetz-Mitgliedschaft eines anderen Batterie-Besitzers im normalen Flux-GUI entziehen | Nächster Maschinentick beendet Registrierung und Ticket; Itemenergie bleibt erhalten |
| Ungeticketeten Maschinenchunk tatsächlich entladen und zurückkehren; Batterie anschließend zwischen Dimensionen bewegen | Pro neuem Standort genau eine Verbindung, keine alte Verbindung und keine verlorene Energie |
| Miner länger ohne Spieler betreiben, einschließlich Abbaubereich über mehrere Chunks | Maschinenchunk bleibt geladen; weiter entfernte Abbauchunks unterliegen Mekanisms Anchor-Regeln |
| Mehrstündiger Modpackbetrieb, mehrere Besitzer/Dimensionen und Ticket-Quoten | Keine verwaisten Teilnehmer/Tickets, Rechte und Kontingente eingehalten |

Diese manuellen Fälle wurden nicht als bestanden markiert. Insbesondere eine vollständige End-to-End-GUI-Bedienung durch zwei echte Clients ist durch den synthetischen Screenshot und die direkt geprüfte Servervalidierung noch nicht ersetzt.

## Einschränkungen

- Original-Flux-JAR allein inkompatibel; die bereitgestellte Bridge ist erforderlich.
- Genau ein aktiver Point pro Maschinenposition; erster passender Batterieslot gewinnt.
- Komplexe Mekanism-`IMultiblock`-Maschinen ausgeschlossen.
- Chunkloading ist standardmäßig aus und hält nur den Maschinenchunk.
- Energie und Kapazität sind persistente Long-FE-Werte. Summenanzeigen jenseits Long.MAX_VALUE sättigen.
- Drittaddons, die aktive Teilnehmer zwingend auf TileFluxDevice casten, wurden nicht getestet.
- Creative-/Admin-Kopien kompletter Items liegen außerhalb der Energieerhaltung normaler Survival-Transfers.

## Entwicklungsdateien und Installation

Neue Modklassen: `src/main/kotlin/de/voxelteuton/fluxbattery/`, Client in `client/`, gezielte Mekanism-Hooks in `mixin/`, Integrationstests in `gametest/`. Bridge: acht ersetzte Java-Klassen plus `IFluxRuntimeDevice`, vollständiger Quellpatch unter `bridge/patches/`. Build: `build.gradle.kts`, `settings.gradle.kts`, Metadaten, Itemmodell/Textur, Übersetzungen und Rezept. Testwerkzeuge unter `scripts/`, JUnit unter `src/test/kotlin/`.

Installation und Bedienung stehen in `README.md`. Benötigte Ausgabe: `build/libs/fluxbattery-0.1.0-dev.jar` **und** `build/bridge/FluxNetworks-1.21.1-8.0.0-fluxbattery.1.jar`, jeweils auf Client und Server neben Mekanism und KFF. Original-Flux-JAR entfernen.

## Gesicherte Nachweise

`docs/evidence/verification.txt` enthält die entscheidenden Laufzeitmeldungen und SHA-256 der beiden JARs. `docs/evidence/battery-screen.png` zeigt die im Minecraft-Client gerenderte Oberfläche bei 854×480 Pixeln. Nach erneutem Ausführen von `patchedFlux --rerun` blieb der Bridge-Hash identisch (`c24c8592e5c596164e6333e0d581d0d023a0e98d89fd488e151ceea21c6c6a64`).
