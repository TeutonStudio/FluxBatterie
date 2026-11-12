# Release-Abnahme 0.1.3-beta.1

Stand: 12.11.2026. Automatische Ergebnisse stehen im aktuellen Release-Nachweis und im Testbericht. Dieses Protokoll unterscheidet tatsächliche Spielabnahme von serverseitigen Regressionstests mit FakePlayer und synthetischer Client-Vorschau.

## Testumgebung

Minecraft 1.21.1, Java 21, NeoForge 21.1.231, Mekanism 1.21.1-10.7.19.85 (Mod-Version 10.7.19), Kotlin for Forge 5.9.0, originales Flux Networks 8.0.0 mit SHA-256 `29e511b2d76f85bd247aa213a92bb059ecc037fcc3fed82baffcff97b638d3a6`. Nur das fertige Addon-JAR zusätzlich installieren, keine historische Bridge. Separate Testwelt verwenden.

## Automatische Teilnachweise

| Prüfung | Ergebnis | Abgrenzung |
|---|---|---|
| Build/JUnit und Loader-Strukturprüfung | Bestanden: 3 Unit-Tests und alle Struktur-/Negativproben | Zusätzlich striktes JSON und Rundlauf aller Steuerzeichen |
| Paketiertes Addon auf GameTestServer | 23/23 bestanden | Enthält echte Maschinen in Overworld/Nether und besitzerbezogenes Ticketkontingent |
| Regulärer Dedicated Server | 23/23 bestanden, Welten vollständig gespeichert | Reale Serverpfade, FakePlayer statt zwei angemeldeter Clients |
| Neustart in zwei getrennten JVMs | 25/25 je JVM bestanden | Geteilte frische Welt für prepare/verify; Point-/Plug-Energie und Tickets |
| Loader-Prozessprüfungen | Alle vier Szenarien bestanden | Zwei kompatible synthetische Testmods, Pflichtkonflikt, falsche Version und verändertes JAR |
| Client mit JEI | Vorschau gerendert und sauber beendet; deutscher Screenshot bei 854 × 480 visuell geprüft | Keine vollständige Bedienung mit realem Spieler/Server |
| Distribution | MIT, Upstream-Hinweis und alle JSON-Ressourcen im JAR geprüft | Keine mitgelieferten Flux-Ersatzklassen oder Testfixture-/Toolklassen |

## Tatsächliche Spielabnahme — offen

Für jeden Fall Datum, Tester, JAR-Hash, Ergebnis und Log/Screenshot ergänzen. Ein automatischer Teilnachweis zählt nicht als bestandener manueller Fall.

| Fall | Durchführung | Erwartetes Ergebnis | Status |
|---|---|---|---|
| Einzelspieler und Oberfläche | Neue Batterie herstellen, Rechtsklick in Luft, Shift-Rechtsklick auf Maschine; alle Eingaben/Schalter, Netzwerk vor/zurück, Speichern, Abbrechen und erneutes Öffnen prüfen. Deutsch/Englisch und kleine/große GUI-Skalierung testen. | Gespeicherte Werte stimmen; Abbrechen verändert keine Konfiguration; alle Bedienelemente und Tooltips lesbar. | Offen |
| Zwei echte Spieler | Dedicated Server und zwei unabhängig angemeldete Clients. A legt privates Netzwerk an; B ist zunächst kein Mitglied, dann Mitglied. Jeder konfiguriert seine eigene Batterie; fremde Batterie bearbeiten versuchen. | Netzwerk nur mit passenden Rechten verwendbar; fremde Konfiguration nicht änderbar. | Offen |
| Rechteentzug im Flux-GUI | A sperrt B während Bs Batterie als Point läuft; danach im Plug-Betrieb wiederholen. Rechte wieder freigeben. | Keine neuen unberechtigten Transfers/Tickets; gespeicherte Energie bleibt erhalten; nach Freigabe funktioniert die Verbindung wieder. | Offen |
| Flux-Verbindungsübersicht | Point- und Plug-Batterie einsetzen, Name/Priorität ändern und Netzfarbe wechseln. Flux-Terminal öffnen. | Richtiger Gerätetyp, Item, Name, Position und Statistik; kein Absturz. | Offen |
| Tatsächliche Chunk-Entladung | Chunkloading im Item und Server ausschalten, Maschine außerhalb Spawn-/anderer Zwangsladebereiche betreiben. Alle Spieler weit entfernen; tatsächliches Entladen über Serverdiagnose bestätigen, dann zurückkehren. | Alte Bindung verschwindet; nach Rückkehr genau eine neue Bindung; Energie/Identität erhalten. | Offen |
| Dimensionswechsel | Geladene konfigurierte Batterie in Overworld entnehmen und im Nether in geeignete Maschine einsetzen, anschließend zurückbewegen. | Keine Verbindung/Tickets am alten Standort; Identität, Energie und Konfiguration bleiben erhalten; neuer Host arbeitet. | Offen |
| Miner ohne Spieler | Batterie-/Server-/Flux-Chunkloading aktivieren; Miner mit geeignetem Mekanism-Anchor über mehrere Abbauchunks arbeiten lassen, alle Spieler entfernen. Später zurückkehren und Server neu starten. | Abbau läuft entsprechend Mekanisms Anchor-Regeln; keine Energieverdopplung; Entnahme beendet Batterieticket. | Offen |
| Langzeit/Modpack | Mindestens vier Stunden mit mehreren Besitzern, Dimensionen, Lade-/Entladehosts, Netzwerkänderungen und Neustart. Welt/Tickets vor und nach dem Test prüfen. | Keine verwaisten Verbindungen/Tickets, anhaltenden Fehler oder unerklärlichen Energieänderungen. | Offen |
| Reale Drittmods | Tatsächlich vorgesehenes Modpack samt Versionsliste mit vorstehenden Fällen testen, insbesondere Mods mit Flux-Mixins/Coremods. | Keine Patchkonflikte oder inkorrekten Teilnehmerannahmen. Freigabe gilt nur für geprüfte Kombination. | Offen |

## Grenzen der hier ausführbaren Abnahme

Die automatisierte Serverregression prüft reale Minecraft-/Mekanism-/Flux-Pfade, benutzt aber FakePlayer und direkte Serveraufrufe. Der Client-Smoke-Test rendert eine Vorschau ohne vollständige Spielverbindung. Zwei echte angemeldete Spieler sowie mehrstündige Bedienungs- und Modpacktests sind damit nicht nachgewiesen. Ein finaler stabiler Release bleibt bis zu diesen Nachweisen offen; die Ausgabe ist als Beta gekennzeichnet.
