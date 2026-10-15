# Architektur

Die neue Mod und ihre Mixins sind vollständig Kotlin. Java bleibt auf die gezielten Änderungen vorhandener Flux-Klassen und die notwendige Bridge-Schnittstelle beschränkt.

## Zustand und Energie

`BatteryComponents` registriert persistente und synchronisierte Identitäts-, Konfigurations- und Energiekomponenten. `BatteryStateService` ist der einzige Schreiber dieser Komponenten. `BatteryEnergyHandler` implementiert FE und die native Flux-Long-API auf demselben ItemStack; Handlerkopien halten keinen eigenen Energiebestand.

Die Batterie besitzt Energie und Konfiguration. `FluxPointAttachment` enthält ausschließlich die nicht serialisierte temporäre Point-Referenz. `HostKey` enthält Serverinstanz, Dimension, unveränderliche Position und Slotindex. Die Laufzeitbindung hält eine Batterie-ID und die registrierte Konfiguration, aber keinen dauerhaft gültigen ItemStack. Vor jedem Bedarf/Transfer wird der aktuelle Slotinhalt erneut geprüft.

## Host-Lebenszyklus

`MekanismTickMixin` ruft den HostManager im gemeinsamen `TileEntityMekanism.tickServer` auf. Ein zweiter Hook meldet beim Entfernen der Tile ab. `EnergySlotMixin` markiert nur Ergebnisse der `fill`- und `fillOrConvert`-Factories. `BasicSlotMixin` erlaubt eine konfigurierte Batterie gezielt in diesen Slots und beobachtet Inhaltsänderungen; `fillInsertCheck` und die Entnahmeregeln bleiben unverändert.

Der HostManager wählt deterministisch den ersten passenden Batterieslot. Identität oder Konfiguration geändert, Rechte ungültig, Item entfernt oder Tile entfernt: aktiven Point stoppen, Ticket entfernen, reguläre Flux-Abmeldung einreihen und Attachment lösen. Dieselben Operationen sind wiederholbar. Ein gleichwertiger kopierter Stack im selben Slot bleibt über die aktuelle Slotabfrage erreichbar.

Beim regulären Serverstopp werden Runtime-Teilnehmer abgemeldet, gültige persistente Tickets aber erhalten. Die Wiederherstellung validiert Maschine, Slot, Batterie, Optionen, Rechte und Quoten erneut. Der neue Server erzeugt einen neuen HostKey.

## Netzwerk und Sicherheit

`HostedFluxPoint` implementiert `IFluxRuntimeDevice` und `IFluxPoint`. Keine neue Welt-BlockEntity, kein Maschinen-BlockState, keine Nachbarschaftssuche. Die bestehende Flux-Queue, Sortierung, Verteilung und Statistik bleiben zuständig.

`HostedPointTransferHandler` meldet freie Batteriekapazität unter Beachtung des verbleibenden Zykluslimits. Zuteilungen werden innerhalb derselben synchronen Serveroperation direkt in das Item geschrieben und als negativer Point-Verbrauch verbucht. Mekanism entlädt das Item anschließend regulär. Es gibt keinen zweiten Pfad in den Maschinenpuffer.

Die Oberfläche wird aus einem serverseitigen Snapshot zugänglicher Netzwerke aufgebaut. Updates nennen nur Hand, Batterie-ID und Konfiguration. Der Server identifiziert das aktuell gehaltene Item erneut, prüft Besitzer, Netzwerkrechte und Wertebereiche und setzt den Besitzer aus dem authentifizierten Spieler. Energie und Maschinenpositionen sind keine Updatefelder. Private Netzwerke verwenden Besitzer-/Mitgliedschaftsbindung, keine Passwortfreigabe.

Clientklassen befinden sich im separaten Client-Paket. Ein ausschließlich clientseitiger KFF-Subscriber installiert den Snapshot-Empfänger. Gemeinsamer Code referenziert keine Minecraft-Screen-Klasse.
