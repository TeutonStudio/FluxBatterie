# Rechte, Besitzer und gespeicherte Daten

**Deutsch** · [English](Permissions-and-Security)

![Für den tatsächlichen Aufnahme-Spieler zugängliche Netzwerke.](https://raw.githubusercontent.com/wiki/TeutonStudio/FluxBatterie/assets/screenshots/wiki-network-selection.png)

Die Batterie ist an einen Besitzer gebunden. Beim ersten erfolgreichen Speichern wird der Spieler als Besitzer festgehalten. Nur dieser Besitzer darf ihre Konfiguration bearbeiten; bei fremden Batterien wird das Öffnen abgewiesen.

## Netzwerkrechte

Die Netzwerkliste enthält nur zugängliche Netzwerke. Private oder verschlüsselte Netzwerke verlangen **dauerhafte Besitzer- oder Mitgliedsrechte**. Ein Passwort wird nicht in der Batterie gespeichert. Eine einmalige Passwortkenntnis ersetzt daher keine Mitgliedschaft.

Der Server prüft beim Speichern noch einmal das gehaltene Item, seine Identität, Besitzer und Konfigurationswerte sowie das Zielnetzwerk. Ein alter geöffneter Dialog umgeht keinen zwischenzeitlichen Rechteentzug. Während des Maschinenbetriebs werden Rechte erneut geprüft; bei Verlust enden unberechtigte Transfers und Tickets.

Die Abbildung zeigt reale zugängliche Netzwerke des angemeldeten Aufnahme-Spielers. Sie stellt keinen Versuch mit zwei unabhängigen Spielern dar. Die FakePlayer-Regression prüft Rechtefälle serverseitig; die vollständige Multiplayer-GUI-Abnahme bleibt offen.

## Umzug, Speichern und Neustart

Die Batterie speichert Identität, Besitzer, Netzwerk-ID, Name, Priorität, Limit, Optionen, lokalen Energiepuffer und ausstehende Netzwerkübergaben im Item. Bei Entnahme oder Umzug werden alte Laufzeitbindungen gelöst. Dieselbe Batterie kann danach in einem anderen geeigneten Host, auch in einer anderen Dimension, weiterarbeiten.

Normale Survival-Transfers werden auf Energieerhaltung geprüft. Kreativ-/Admin-Kopien kompletter Items kopieren jedoch auch deren Daten; sie sind kein unterstützter Energieerhaltungsfall. Fremde Mod-Eingriffe können zusätzliche Einschränkungen verursachen. Nutze vor Änderungen an Modpacks oder Weltdaten Sicherungen.

Die [Prüfungen](Entwicklung-und-Pruefungen) unterscheiden automatisierte Persistenz-/Rechtetests von den noch offenen echten Spielerfällen.
