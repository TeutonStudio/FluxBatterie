# Chunkloading und Serverkonfiguration

**Deutsch** · [English](Chunk-Loading)

![Tatsächliche Serverkonfiguration der separaten Aufnahmewelt.](https://raw.githubusercontent.com/wiki/TeutonStudio/FluxBatterie/assets/screenshots/wiki-server-config.png)

Chunkloading ist **standardmäßig deaktiviert**. Eine gesetzte Itemoption allein reicht nicht. Die Serverkonfiguration heißt `fluxbattery-server.toml`; NeoForge bestimmt den weltabhängigen Speicherort, typischerweise unter dem `serverconfig`-Ordner der Welt.

```toml
allowChunkLoading = false
maxTicketsPerOwner = 4
batteryCapacityFE = 1000000000
```

| Option | Wirkung |
|---|---|
| `allowChunkLoading` | Erlaubt eigene tickende Batterietickets. |
| `maxTicketsPerOwner` | Maximale Ticketzahl eines Batterie-Besitzers über alle Dimensionen; 0 bis 1024. |
| `batteryCapacityFE` | Standardkapazität für neue Energiezustände; 1 bis Long-Max FE. Bereits gespeicherte Item-Kapazitäten werden nicht pauschal migriert. |

Die Abbildung zeigt die tatsächliche Konfigurationsoberfläche der neuen Aufnahmewelt. Diese wurde für die Aufnahme bewusst mit erlaubtem Chunkloading vorbereitet. Das ist kein Nachweis einer tatsächlichen Chunk-Entladung mit Rückkehr.

Die aktuelle Beta zeigt in dieser NeoForge-Seite teilweise unübersetzte Konfigurationsschlüssel. Ordne sie anhand der Optionsnamen in der Tabelle zu.

## Voraussetzungen

- `allowChunkLoading` ist auf dem Server aktiv.
- Flux Networks erlaubt Chunkloading global.
- Die Batterie hat **Chunk laden** aktiviert und ihre Konfiguration gespeichert.
- Sie ist in einer geeigneten Maschine aktiv, ihr Netzwerk ist gültig und ihr Besitzer hat dauerhaften Zugriff.
- Das gemeinsame Besitzerkontingent ist noch nicht ausgeschöpft.

Eine aktive Batterie hält genau **den Maschinenchunk** tickend geladen. Das gilt unter den Voraussetzungen für Point- und Plug-Hosts. Der Digital Miner kann zusätzlich ein Mekanism-Anchor-Upgrade für seine Abbauchunks benötigen. Die Batterie ersetzt diese Regeln nicht.

Entnahme, Maschinenabbau, Rechteverlust und das Abschalten der Serverfreigabe entfernen das Batterieticket. Gültige Tickets werden beim Neustart erneut validiert; ungültige oder verwaiste Tickets werden verworfen. Die automatischen Neustarttests prüfen diesen Lebenszyklus, ersetzen aber keine vollständige Langzeitabnahme im Modpack.
