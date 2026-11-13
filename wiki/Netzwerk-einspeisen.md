# Netzwerk einspeisen: Flux Plug

**Deutsch** · [English](Feeding-the-Network)

![Echter Energiewürfel mit vollgeladener Batterie im Item-Ladeslot.](https://raw.githubusercontent.com/wiki/TeutonStudio/FluxBatterie/assets/screenshots/wiki-charging-slot.png)

Im **Item-Ladeslot** eines unterstützten Mekanism-Blocks wird dieselbe konfigurierte Batterie zum **virtuellen Flux Plug**. Verwende beispielsweise den Plus-/Ladeslot eines Energiewürfels.

## Energiefluss

1. Wähle das Zielnetzwerk und speichere die Batterie-Konfiguration.
2. Sorge für Energie im Ladeblock und Bedarf im Flux-Netzwerk, etwa einen Point oder Flux-Speicher.
3. Setze die Batterie in den Ladeslot ein.
4. Der Block füllt zuerst den lokalen Batterie-Puffer. Erst danach wird überschüssige Energie für das Netzwerk angenommen.
5. Flux Networks verteilt diese Energie über seine normale Verteilung; Priorität, Rechte und Limit gelten weiter.

Die Abbildung zeigt die echte Aufnahme mit einer bereits voll geladenen Batterie im Ladeslot eines Ultimate Energy Cube. Der Aufnahmelauf prüfte zusätzlich, dass der separate Flux-Speicher tatsächlich Energie erhielt. Für das Beispiel wurde Anfangsenergie in der Testwelt bereitgestellt; es ist kein Nachweis einer autonomen Solarproduktion.

## Kein Bedarf und ausstehende Übergaben

Wenn das Netzwerk nichts aufnehmen kann, wird weitere Überschussenergie abgewiesen und bleibt im Ladeblock. Bereits angenommene, noch nicht verteilte Energie bleibt separat in der Batterie gespeichert. Der Tooltip nennt sie als „FE zur Netzwerkübergabe (Ladeslot)“. Entnehmen oder Neustart löscht sie nicht; beim erneuten Einsetzen in einen geeigneten Ladeslot wird die Verteilung fortgesetzt.

Ein gewöhnlicher FE-Item-Lader kann den lokalen Puffer laden, erzeugt aber keine automatische Netzwerk-Einspeisung. Dafür ist der Mekanism-Hostadapter erforderlich. Komplexe Multiblocks und der Betrieb mehrerer virtueller Geräte aus einer Maschine werden nicht unterstützt.

## In Flux Networks ansehen

Öffne ein reguläres Gerät im Zielnetzwerk und wechsle zur Verbindungsübersicht. Die Batterie erscheint am Maschinenstandort mit Namen, Item und passender Plug-Rolle. Die Aufnahme auf [Bildnachweise](Bildnachweise) zeigt diese reale Registrierung. Momentaner Durchsatz kann nach dem Füllen der Empfänger wieder null sein.
