# Einstellungen und Energieanzeige

**Deutsch** · [English](Settings)

![Synthetische Client-Prüfung: aktive Schalter und maximaler Limitwert.](https://raw.githubusercontent.com/wiki/TeutonStudio/FluxBatterie/assets/screenshots/battery-switches.png)

Die Einstellungen entsprechen den Eigenschaften des virtuellen Flux-Geräts. Sie gelten sowohl im Point- als auch im Plug-Betrieb und werden erst beim Speichern übernommen.

| Feld | Bedeutung |
|---|---|
| Name | Bis zu 24 Zeichen; erscheint als Gerätename in Flux Networks. |
| Priorität | Ganzzahl von −9999 bis 9999. Höhere Werte erhalten in Flux Networks Vorrang. |
| Limit | Nichtnegative FE pro Netzwerkzyklus; maximal 9.223.372.036.854.775.807. Neue Batterien: 100.000.000. |
| Surge-Modus | Verwendet Flux Networks' Surge-Priorität bei der Zuteilung. Erzeugt keine zusätzliche Energie. |
| Limit deaktivieren | Ignoriert das eingetragene Transferlimit. Der Wert bleibt erhalten und ist vorübergehend nicht editierbar. |
| Chunk laden | Beantragt im eingesetzten Zustand Chunkloading unter den [Serverregeln](Chunkloading). |

Bereits konfigurierte Batterien behalten ihr älteres Limit. Ein Update hebt ein vorhandenes Limit nicht automatisch an. Für hohen Durchsatz müssen auch Energiequelle, regulärer Flux Plug und Maschine ausreichend liefern beziehungsweise aufnehmen können. „Unbegrenzt“ ersetzt keine Energiequelle.

## Puffer und Validierung

Die kompakte Anzeige zeigt Füllstand in Prozent sowie gespeicherte Energie und Kapazität. Der Tooltip enthält die exakten FE-Werte. Das sind Werte **beim Öffnen** des Dialogs, keine Live-Messung oder Netzwerk-Gesamtenergie.

Ungültige Priorität und überlaufende/fehlende Limitwerte verhindern Speichern. Korrigiere die Eingabe; der Entwurf bleibt erhalten. Der Server prüft Identität, Besitzer und Rechte unabhängig von der Oberfläche erneut.

Die Abbildung stammt aus dem synthetischen Client-Prüflauf: Sie zeigt eingeschaltete Optionen und das Long-Max-Limit. Sie belegt Darstellung und Widget-Bedienung; kein tatsächlich geladen gehaltener Maschinenchunk wird damit behauptet.
