# Maschinen versorgen: Flux Point

**Deutsch** · [English](Powering-Machines)

![Echter Energized Smelter: Batterie im Entladeslot und tatsächlich erzeugte Eisenbarren.](https://raw.githubusercontent.com/wiki/TeutonStudio/FluxBatterie/assets/screenshots/wiki-powering-machine.png)

Im Energie-Entladeslot einer unterstützten Mekanism-Maschine wird die Batterie als **virtueller Flux Point** registriert. Der normale Flux-Verteiler liefert Energie in ihren Puffer; Mekanisms Item-Energieadapter entlädt sie in die Maschine.

## Aufbau

1. Versorge dein Netzwerk über einen regulären Flux Plug.
2. Konfiguriere und speichere die Batterie für dieses Netzwerk.
3. Setze sie in den Slot, aus dem die Maschine Energieitems entlädt. Verwechsle ihn nicht mit einem Item-Ladeslot.
4. Lege gegebenenfalls Material ein und kontrolliere Energie und Verarbeitung.

Ein Controller oder zusätzlicher Flux Point direkt an der Maschine ist nicht nötig. Die Batterie muss nicht vorgeladen sein. Die Aufnahme zeigt einen echten Energized Smelter mit eingesetzter Batterie; im Aufnahmelauf wurden tatsächlich Eisenbarren aus Roheisen erzeugt.

## Geeignete Maschinen

Der Adapter unterstützt Mekanism-Entladeslots mit `fill`-/`fillOrConvert`-Logik. Getestete Beispiele sind **Digital Miner**, **Enrichment Chamber** und **Energized Smelter**. Komplexe Mekanism-`IMultiblock`-Maschinen sind ausgeschlossen; fremde FE-Maschinen erhalten nicht automatisch eine Netzwerkverbindung.

Bei mehreren passenden Batterieslots wird nur die erste passende Batterie als Netzwerkgerät aktiv. Weitere Batterien bleiben gewöhnliche Energieitems. Welche Rolle gewinnt, hängt vom ersten passenden Slot ab.

## Entnehmen und versetzen

Entnehmen entfernt die aktive Verbindung und ihr eventuelles Chunk-Ticket. Identität, Einstellungen und Energie bleiben im Item. Setze dieselbe Batterie in eine andere geeignete Maschine ein, um dort weiterzuarbeiten. Rechte und Konfiguration werden während des Betriebs erneut geprüft.

Für den Digital Miner gelten weiterhin dessen Such-, Filter- und Anchor-Regeln. Ein Batterieticket lädt nur den Maschinenchunk, nicht automatisch die gesamte Abbaufläche. Siehe [Chunkloading](Chunkloading).
