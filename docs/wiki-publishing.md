# Flux Battery Wiki pflegen

Die veröffentlichungsfähigen Quellen liegen unter `wiki/`, das öffentliche Wiki unter
https://github.com/TeutonStudio/FluxBatterie/wiki. Stand: veröffentlichtes Release 0.1.3-beta.2.

## Konventionen

- Deutsch ist Einstieg und Hauptnavigation; jede Anleitung hat eine vollständige englische Partnerseite.
- `README.md` ist ein kurzer englischer Einstieg mit Links auf beide Wiki-Sprachen, Downloads und Issues.
- Nutzeranleitungen behandeln Installation, Herstellung, Erstbetrieb, Konfiguration, Netzwerke, Point-/Plug-Rollen, Chunkloading, Rechte und Fehlerbehebung.
- Jede Anleitung bindet eine passende Abbildung ein. Reale Spielaufnahmen und synthetische UI-Prüfbilder werden ausdrücklich unterschieden.
- `docs/wiki-media-coverage.json` ordnet die 12 Sprachpaare und 10 illustrierten Funktionsthemen zu, dokumentiert Herkunft und Aussagegrenzen und prüft Bild-Hashes.
- Ein Konfigurationsbild ist kein Nachweis für echten Chunk-Unload, zwei Spieler oder Langzeitbetrieb. Offene Release-Abnahmefälle bleiben offen.
- Mod-Code, Mod-Version und Release-Artefakte werden für reine Dokumentationsänderungen nicht verändert.

## Prüfen

```sh
python3 scripts/verify-wiki.py
python3 scripts/verify-wiki.py --require-all-media
git diff --check
```

Das Werkzeug prüft interne Seiten-/Abschnittsziele, Navigation, beidseitige Sprachverweise,
Codeblöcke, tatsächliche PNG-Dateien, Medieninventar und kurze README. Die strenge Variante
verlangt zusätzlich, dass alle eingetragenen Funktionsthemen bebildert sind. Die Statusangabe
`shown` bedeutet eine passende, in der Beschreibung eingegrenzte Abbildung, keine vollständige
Spielabnahme dieser Funktion.

Der Workflow `.github/workflows/docs.yml` führt dieselbe strenge Prüfung für Dokumentationsänderungen auf GitHub aus.

## Veröffentlichen

GitHub-Wikis sind separate Git-Repositories. Änderungen am Hauptrepository allein aktualisieren
das Wiki nicht. Das vorhandene Wiki ist bereits initialisiert.

```sh
git clone https://github.com/TeutonStudio/FluxBatterie.wiki.git /tmp/fluxbattery-wiki
cp wiki/*.md /tmp/fluxbattery-wiki/
cp -R wiki/assets /tmp/fluxbattery-wiki/
git -C /tmp/fluxbattery-wiki diff --check
git -C /tmp/fluxbattery-wiki add -- .
git -C /tmp/fluxbattery-wiki commit -m "docs: update Flux Battery wiki"
git -C /tmp/fluxbattery-wiki push origin master
```

Vorher den aktuellen Remote-Stand abrufen und Änderungen anderer Autoren erhalten.
Hauptrepository und Wiki erhalten getrennte Dokumentationscommits. Anschließend die tatsächliche
GitHub-Darstellung beider Sprachen und die Bild-URLs prüfen; HTTP-Erfolg allein garantiert keine
sichtbare Einbindung. Für Medien werden die Wiki-Raw-Adressen unter
`https://raw.githubusercontent.com/wiki/TeutonStudio/FluxBatterie/assets/screenshots/` verwendet.

## Aufnahmeherkunft

Die frischen Bilder stammen aus `build/wiki-capture-run`, einer neuen flachen Welt und einem
separaten temporären Java-SourceSet mit eigenem Mod-ID-Namensraum. Es wurde über ein temporäres
Gradle-Init-Skript ausschließlich dem Aufnahmelauf hinzugefügt. Die Produktquelle und das
veröffentlichte JAR blieben unverändert. Die Fixture platzierte Maschinen und Anfangsenergie,
öffnete native GUIs und prüfte tatsächlich erzeugte Eisenbarren sowie 2.000.000 FE im Flux-Speicher.

Alle PNGs wurden mit Minecrafts Screenshot-API gespeichert und unverändert kopiert. Mehrere
englische Anleitungen verwenden die gleichen deutschen Ingame-Bilder mit englischer Erklärung.
Die zwei übernommenen UI-Prüfbilder sind als synthetische Vorschauen gekennzeichnet.
