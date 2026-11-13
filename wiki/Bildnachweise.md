# Bildnachweise und Grenzen

**Deutsch** · [English](Media-Provenance)

![Echte Flux-Verbindungsübersicht des Einspeisenetzes mit virtuellem Batterie-Plug.](https://raw.githubusercontent.com/wiki/TeutonStudio/FluxBatterie/assets/screenshots/wiki-flux-connections.png)

Diese Wiki verwendet echte, unveränderte Minecraft-Aufnahmen. Die nachstehenden Bildtypen haben unterschiedliche Aussagen; eine Konfigurationsabbildung allein beweist keine lang andauernde Spielmechanik.

## Neue Spielaufnahmen vom 13. November 2026

Aufnahmeclient: veröffentlichtes Addon **0.1.3-beta.2**, Minecraft 1.21.1, NeoForge 21.1.231, Java 21, Mekanism 10.7.19.85, KFF 5.9.0, originales Flux Networks 8.0.0 und JEI 19.39.0.372. Fenster 1280 × 800, GUI-Skalierung 2, deutsche Spielsprache.

Eine temporäre separate Aufnahme-Mod richtete in einer **neuen flachen Testwelt** Netzwerke, Maschinen, Inventare und Anfangsenergie ein und öffnete native Oberflächen. Die Aufnahmefixture gehört nicht zum Produkt und wird nicht im Release-JAR ausgeliefert. Es wurden keine persönlichen Welten geöffnet.

- Übersicht: tatsächliche Maschinen-/Flux-Testszene.
- Werkbank: tatsächliche Rezeptberechnung mit Batterie im Ergebnisslot.
- Konfiguration/Netzwerkauswahl: echte serverseitige Batterie-Daten und reale private Netzwerke des Aufnahme-Spielers.
- Schmelzofen: echtes Roheisen wurde zu Eisenbarren verarbeitet; der Aufnahmelauf prüfte den Output.
- Energiewürfel: vollgeladene Batterie im richtigen Item-Ladeslot; der Aufnahmelauf prüfte einen positiven Energiezugang im Flux-Speicher.
- Flux-Verbindungsübersicht: tatsächlicher registrierter virtueller Plug. Bereits gefüllte Speicher können zum Aufnahmezeitpunkt null Durchsatz zeigen.
- Serverkonfiguration: tatsächlich geöffnete NeoForge-Konfigurationsseite der Testwelt, bewusst mit freigegebenem Chunkloading.

## Bereits vorhandene UI-Prüfbilder

Die Dateien `battery-switches.png` und `battery-validation.png` stammen aus dem Release-Client-Prüflauf. Sie zeigen synthetische Netzwerke und Anfangswerte. Der Prüflauf bedient tatsächliche Widgets, validiert Entwurfserhalt und führt keine vollständige Spielverbindung aus. Sie werden als Darstellung von Optionen beziehungsweise Validierung verwendet.

## Was damit nicht nachgewiesen wird

Kein Zwei-Spieler-Berechtigungstest, keine tatsächliche Chunk-Entladung mit Rückkehr, kein mehrstündiger Modpackbetrieb und keine autonome Energieerzeugungsanlage. Automatische FakePlayer-/Servertests und Aufnahmen werden nicht zu einer bestandenen manuellen Gesamtfreigabe zusammengefasst. Der Release bleibt eine Beta.

Das [Medieninventar](https://github.com/TeutonStudio/FluxBatterie/blob/master/docs/wiki-media-coverage.json) hält Zuordnung, Herkunft und Aussagegrenzen fest.
