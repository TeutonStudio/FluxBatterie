# Architektur

Alle neuen JVM-Modklassen, Schnittstellen und Mixins sind Kotlin. Der frühe NeoForge-Coremod-Einstieg ist als dokumentierte technische Abweichung ein JavaScript-ASM-Skript. Es löst vor KFF ausschließlich NeoForge-/ASM-Klassen auf und fügt symbolische Verweise auf die spätere Kotlin-ABI ein. Vollständige Ersatzklassen im `sonar.fluxnetworks`-Namespace werden nicht gebaut oder ausgeliefert.

## Zustand und Energie

`BatteryComponents` registriert persistente und synchronisierte Identitäts-, Konfigurations- und Energiekomponenten. `BatteryStateService` ist der einzige Schreiber dieser Komponenten. `BatteryEnergyHandler` implementiert FE und die native Flux-Long-API auf demselben ItemStack; Handlerkopien halten keinen eigenen Energiebestand.

Die Batterie besitzt Energie und Konfiguration. `FluxPointAttachment` enthält ausschließlich die nicht serialisierte temporäre Point-Referenz. `HostKey` enthält Serverinstanz, Dimension, unveränderliche Position und Slotindex. Die Laufzeitbindung hält eine Batterie-ID und die registrierte Konfiguration, aber keinen dauerhaft gültigen ItemStack. Vor jedem Bedarf/Transfer wird der aktuelle Slotinhalt erneut geprüft.

## Host-Lebenszyklus

`MekanismTickMixin` ruft den HostManager im gemeinsamen `TileEntityMekanism.tickServer` auf. Ein zweiter Hook meldet beim Entfernen der Tile ab. `EnergySlotMixin` markiert nur Ergebnisse der `fill`- und `fillOrConvert`-Factories. `BasicSlotMixin` erlaubt eine konfigurierte Batterie gezielt in diesen Slots und beobachtet Inhaltsänderungen; `fillInsertCheck` und die Entnahmeregeln bleiben unverändert.

Der HostManager wählt deterministisch den ersten passenden Batterieslot. Identität oder Konfiguration geändert, Rechte ungültig, Item entfernt oder Tile entfernt: aktiven Point stoppen, Ticket entfernen, reguläre Flux-Abmeldung einreihen und Attachment lösen. Dieselben Operationen sind wiederholbar. Ein gleichwertiger kopierter Stack im selben Slot bleibt über die aktuelle Slotabfrage erreichbar.

Beim regulären Serverstopp werden Runtime-Teilnehmer abgemeldet, gültige persistente Tickets aber erhalten. Die Wiederherstellung validiert Maschine, Slot, Batterie, Optionen, Rechte und Quoten erneut. Der neue Server erzeugt einen neuen HostKey.

## Netzwerk und Sicherheit

`HostedFluxPoint` implementiert `de.voxelteuton.fluxbattery.bridge.FluxRuntimeDevice` und `IFluxPoint`. Keine neue Welt-BlockEntity, kein Maschinen-BlockState, keine Nachbarschaftssuche. Die bestehende Flux-Queue, Sortierung, Verteilung und Statistik bleiben zuständig.

`HostedPointTransferHandler` meldet freie Batteriekapazität unter Beachtung des verbleibenden Zykluslimits. Zuteilungen werden innerhalb derselben synchronen Serveroperation direkt in das Item geschrieben und als negativer Point-Verbrauch verbucht. Mekanism entlädt das Item anschließend regulär. Es gibt keinen zweiten Pfad in den Maschinenpuffer.

Die Oberfläche wird aus einem serverseitigen Snapshot zugänglicher Netzwerke aufgebaut. Updates nennen nur Hand, Batterie-ID und Konfiguration. Der Server identifiziert das aktuell gehaltene Item erneut, prüft Besitzer, Netzwerkrechte und Wertebereiche und setzt den Besitzer aus dem authentifizierten Spieler. Energie und Maschinenpositionen sind keine Updatefelder. Private Netzwerke verwenden Besitzer-/Mitgliedschaftsbindung, keine Passwortfreigabe.

Clientklassen befinden sich im separaten Client-Paket. Ein ausschließlich clientseitiger KFF-Subscriber installiert den Snapshot-Empfänger. Gemeinsamer Code referenziert keine Minecraft-Screen-Klasse.

## Ladezeit und Hostgrenze

Die originale `TileFluxDevice` implementiert nach Transformation dieselbe `FluxRuntimeDevice`-Schnittstelle wie `HostedFluxPoint`. `FluxRuntimeNetwork` ergänzt Runtime-Überladungen der regulären An-/Abmeldung; die alten Tile-Signaturen delegieren weiter. `ServerRuntimeAccess` stellt ausschließlich die originalen Warteschlangen, Verbindungskarte und das Löschflag bereit. `RuntimeHooks` schützt deren Lebenszyklus und sättigt Summen; es verwaltet keine zweite Teilnehmerliste und verteilt keine Energie.

Das Paket `bridge` hängt weder von Mekanism noch von dessen Slots oder Tiles ab. Die Maschinenanbindung implementiert den Netzwerkvertrag in `HostedFluxPoint`; `MekanismHostAdapter` erkennt ausschließlich Mekanisms markierte Entladeslots. Slotrichtung, aktueller Inhalt, Inhaltsänderungen, Abbau und Item-Persistenz sind Aufgaben des jeweiligen Host-Adapters. Weitere Mods müssen diesen Vertrag ausdrücklich erfüllen. Eine beliebige FE-Capability begründet keine Maschinenunterstützung.

Coremod-Prüfung: erwartete Oberklasse, Feld-/Methodenflags, Methodendeskriptoren, Exceptiontabellen, ausführbare Instruktionen, Memberreferenzen, Lambda-Handles und Sprungziele gegen die eingecheckten Originalstrukturen prüfen, dann ändern, zuletzt Erfolg markieren. Der Modkonstruktor prüft Version und Original-JAR-Hash und lädt alle auf der Seite vorhandenen Pflichtklassen. Jeder fehlende Marker oder gespeicherte Transformationsfehler verhindert den Start. ClientFluxNetwork wird ausschließlich auf dem Client geladen. Näheres in `flux-network-bridge.md` und `load-time-prototype.md`.
