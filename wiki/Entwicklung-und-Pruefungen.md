# Entwicklung und Prüfungen

**Deutsch** · [English](Development-and-Tests)

![Tatsächlicher Schmelzbetrieb ergänzt die automatisierten Nachweise.](https://raw.githubusercontent.com/wiki/TeutonStudio/FluxBatterie/assets/screenshots/wiki-powering-machine.png)

Veröffentlichter Stand: **0.1.3-beta.2**, Commit `1a9e690`. Die Implementierung ist Kotlin mit Kotlin for Forge; der Ladezeit-Patch erweitert ausschließlich die benötigten Original-Flux-Klassen im Speicher. Es wird kein Ersatz-Flux-JAR ausgeliefert.

## Lokal bauen

Mit Java 21 im Projektverzeichnis:

```sh
python3 scripts/bootstrap-dependencies.py
./gradlew build
./gradlew runGameTestServer -PpackagedAddon
python3 scripts/test-restart.py
python3 scripts/test-server.py
python3 scripts/test-loader.py
python3 scripts/test-client.py
python3 scripts/verify-distribution.py
```

Der Wrapper verwendet Gradle 9.2.1, ModDevGradle 2.0.141 und Kotlin 2.1.21. Das Addon liegt in `build/libs/`. `-PpackagedAddon` prüft das gebaute JAR; Entwicklungs- und Testfixture-Klassen ersetzen kein auszulieferndes Flux-JAR. Für IntelliJ die eingecheckte Gradle-Konfiguration **Flux Battery Client** oder `runClient` verwenden.

## Tatsächlich ausgeführte Release-Prüfungen

| Prüfung | Ergebnis |
|---|---|
| JUnit | 3 bestanden |
| Dedicated Server | 24/24 bestanden |
| GameTests mit kompatiblen synthetischen Mixin-/Coremod-Fixtures | 24/24 bestanden |
| Neustart, zwei getrennte JVMs | Je 26/26 bestanden |
| Falsche Version, geändertes Original, Pflichtkonflikt | Erwarteter Startabbruch |
| Client | Deutsch/Englisch, Suche, Seitenwechsel, Schalter, Validierung und echte GUI-Verkleinerung auf 320 × 240 |
| Distribution | Ein Addon, korrekte Metadaten/Lizenzen und gültiges JSON |

Die reale Spielaufnahme oben ergänzt diese Prüfungen: Der Schmelzofen produzierte Barren. Eine Aufnahmefixture richtete die Szene ein; das ist kein manuell durchgespielter Survival-Aufbau. Der Energiefluss im Ladebeispiel wurde zusätzlich anhand des echten Flux-Speichers geprüft.

[Prüfnachweis und Hashes](https://github.com/TeutonStudio/FluxBatterie/blob/v0.1.3-beta.2/docs/evidence/release-0.1.3-beta.2.txt) · [Architektur](https://github.com/TeutonStudio/FluxBatterie/blob/v0.1.3-beta.2/docs/architecture.md) · [Offene Spielabnahme](https://github.com/TeutonStudio/FluxBatterie/blob/v0.1.3-beta.2/docs/release-acceptance.md)

Die Aufnahmen ändern weder Mod-Version noch Release-JAR. Details zur Aufnahmeherkunft stehen unter [Bildnachweise](Bildnachweise).
