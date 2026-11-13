# Installation

**Deutsch** · [English](Installation-en)

![Echte Testwelt mit Schmelzofen, Energiewürfel und Flux-Geräten.](https://raw.githubusercontent.com/wiki/TeutonStudio/FluxBatterie/assets/screenshots/wiki-overview.png)

Installiere die folgenden Mods auf dem **Server und allen Clients**. Die Wiki beschreibt die veröffentlichte Vorabversion **0.1.3-beta.2** vom 13. November 2026.

| Bestandteil | Unterstützter Stand |
|---|---|
| Minecraft / Java | 1.21.1 / Java 21 |
| NeoForge | ab 21.1.231, innerhalb 21.1 |
| Flux Battery | 0.1.3-beta.2 |
| Flux Networks | originales 1.21.1-8.0.0, CurseForge-Datei 6089446 |
| Mekanism | 1.21.1-10.7.19.85; interne Mod-Version 10.7.19 |
| Kotlin for Forge | 5.9.0 für NeoForge; Metadatenbereich 5.9 bis kleiner 6 |

## Einrichten

1. Lade das Addon-JAR aus dem [Release](https://github.com/TeutonStudio/FluxBatterie/releases/tag/v0.1.3-beta.2).
2. Installiere die Abhängigkeiten und `fluxbattery-0.1.3-beta.2.jar` im `mods`-Ordner.
3. Entferne historische Flux-Ersatz-/Bridge-JARs. Behalte das originale Flux-Networks-JAR.
4. Starte eine Testwelt und fahre mit [Erste Schritte](Erste-Schritte) fort.

JEI ist optional. Es wird im Entwicklungsclient für die Aufnahmen verwendet und ist keine Pflichtabhängigkeit des Addons. Ein Flux Controller ist für den Batteriebetrieb nicht erforderlich. TeutonLib, Create und Sable werden für diese Mod nicht benötigt.

## Aktualisieren und prüfen

Client und Server gemeinsam aktualisieren: Das Konfigurationsprotokoll ist Version 2. Gespeicherte Batterie- und Weltdaten bleiben kompatibel. Vor einem Update eine Sicherung der Welt anlegen.

Die zum Download gehörende `.sha256`-Datei prüft das Addon. Nur das unveränderte Flux Networks 8.0.0 wird akzeptiert, mit SHA-256 `29e511b2d76f85bd247aa213a92bb059ecc037fcc3fed82baffcff97b638d3a6`. Bei Versions-, Hash- oder Pflicht-Patchkonflikten bricht der Start mit einer konkreten Meldung ab; siehe [Fehlerbehebung](Fehlerbehebung).

Die Abbildung zeigt den echten separaten Aufnahmeclient mit aufgebauter Testszene; die Welteinrichtung erfolgte durch eine temporäre Aufnahmefixture. Sie ist kein Nachweis für beliebige Modpack-Kombinationen.
