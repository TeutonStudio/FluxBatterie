# Erste Schritte

**Deutsch** · [English](Getting-Started)

![Batterie-Konfiguration mit echten Netzwerken aus der Aufnahmewelt.](https://raw.githubusercontent.com/wiki/TeutonStudio/FluxBatterie/assets/screenshots/wiki-configuration.png)

Eine Flux-Batterie verbindet eine geeignete Mekanism-Maschine mit einem normalen Flux-Netzwerk. Ihre Rolle richtet sich nach dem Slot: **Entladeslot → Flux Point**, **Ladeslot → Flux Plug**.

## Eine Maschine versorgen

1. Lege in Flux Networks ein Netzwerk an, auf das du dauerhaften Zugriff hast. Verbinde eine Energiequelle über einen normalen Flux Plug.
2. [Stelle die Batterie her](Herstellung) und halte sie in der Hand.
3. Rechtsklicke in die Luft. Wenn du einen Block anvisierst, verwende **Shift-Rechtsklick**, um dessen Oberfläche zu umgehen.
4. Öffne **Netzwerkauswahl**, wähle dein Netzwerk und kehre zu **Einstellungen** zurück.
5. Vergib einen Namen und prüfe Priorität und Limit. Neue Batterien erlauben 100 Millionen FE pro Netzwerkzyklus.
6. Klicke **Konfiguration speichern** und setze die Batterie in den Energie-Eingabeslot der Maschine. Eine konfigurierte leere Batterie ist ebenfalls zulässig.
7. Öffne die Maschine und prüfe ihren Energiepuffer sowie die normale Verarbeitung.

Die Abbildung zeigt eine vom echten integrierten Server geöffnete Batterie-Konfiguration. Die gehaltene Batterie ist noch keine aktive Netzwerkverbindung. Die Energieanzeige zeigt den Stand beim Öffnen.

## Energie zurück ins Netzwerk

Setze die konfigurierte Batterie in den **Item-Ladeslot** eines unterstützten Mekanism-Blocks, etwa den Plus-Slot eines Energiewürfels. Dieser füllt zuerst den Batterie-Puffer; danach kann Energie an Netzwerkverbraucher und Flux-Speicher fließen. Anleitung: [Netzwerk einspeisen](Netzwerk-einspeisen).

Entnehmen beendet die Verbindung. Identität, Konfiguration und gespeicherte Energie verbleiben im Item. Die bisherige Maschine erhält ohne diese Batterie keine weitere Batterieverbindung.
