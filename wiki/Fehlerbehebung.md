# Fehlerbehebung

**Deutsch** · [English](Troubleshooting)

![Synthetischer Prüffall: ungültige Priorität verhindert Speichern.](https://raw.githubusercontent.com/wiki/TeutonStudio/FluxBatterie/assets/screenshots/battery-validation.png)

Die Abbildung zeigt eine absichtlich ungültige Priorität aus dem synthetischen Client-Prüflauf. Der Dialog bleibt offen und überträgt diese Eingabe nicht.

| Beobachtung | Prüfen und beheben |
|---|---|
| Start bricht mit Flux-Version oder Hash ab | Originales Flux Networks 8.0.0 aus der dokumentierten Datei installieren; Ersatz-/Bridge-JARs entfernen. Die Prüfung nicht durch Umbenennen umgehen. |
| Pflicht-Patchkonflikt | Log nennt die veränderte Pflichtstruktur. Modliste vergleichen und den Konflikt mit dem verursachenden Transformer eingrenzen. |
| Rechtsklick öffnet die Maschine | Batterie halten und Shift-Rechtsklick verwenden oder in die Luft rechtsklicken. |
| Kein Netzwerk sichtbar | Netzwerk in Flux Networks anlegen, dauerhafte Rechte prüfen und Dialog erneut öffnen. |
| Gehaltene Batterie bleibt getrennt | Das ist vorgesehen: Aktiv wird sie erst in einem geeigneten Maschinen-Slot. |
| Leere Batterie wird nicht akzeptiert | Zuerst Netzwerk konfigurieren und speichern; richtigen Mekanism-Slot und Multiblock-Ausschluss prüfen. |
| Maschine erhält keine Energie | Netzwerkquelle/Flux Plug, gespeichertes Netzwerk, Rechte, Slot, Priorität und Limits prüfen. |
| Hochgerüsteter Miner zu langsam | Alte Batterien behalten ihr Limit; Batterie und Quell-Plug anpassen. Quelle und Minerbedarf ebenfalls prüfen. |
| Ladeslot speist nicht ein | Erst wird der Batterie-Puffer gefüllt. Danach braucht das Netzwerk Bedarf; gewöhnliche FE-Lader bieten keine automatische Einspeisung. |
| Noch ausstehende FE im Tooltip | Angenommene Übergabeenergie ist im Item erhalten. Wieder in einen berechtigten Mekanism-Ladeslot einsetzen. |
| Zweite Batterie verbindet nicht | Nur die erste passende Batterie je Maschine ist ein virtuelles Netzwerkgerät. |
| Chunk laden bleibt wirkungslos | Itemoption, beide Server-/Flux-Freigaben, Besitzerkontingent und aktive Hostbindung prüfen. |
| Miner stoppt außerhalb des Maschinenchunks | Mekanisms Anchor-Regeln für weitere Abbauchunks beachten. |
| Speichern wird abgewiesen | Eingaben korrigieren; bei geändertem Item oder Rechten neu öffnen. Client und Server müssen Protokoll 2 verwenden. |

## Einen Fehler melden

Nenne Addon-, Minecraft-, NeoForge-, Mekanism-, KFF- und Flux-Versionen, die Rolle (Point/Plug), Maschine und Slot sowie die Schritte zur Wiederholung. Ergänze relevante Logs und den JAR-Hash, aber keine Zugangsdaten. [Issue anlegen](https://github.com/TeutonStudio/FluxBatterie/issues).

0.1.3-beta.2 bleibt eine Vorabversion. Zwei echte Spieler, tatsächliche Chunk-Entladung mit Wiederkehr und mehrstündiger Modpackbetrieb sind noch nicht vollständig abgenommen.
