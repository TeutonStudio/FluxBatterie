# Energieinvarianten

- Intern: ein nichtnegativer Long-FE-Bestand je ItemStack, `0 <= stored <= capacity`.
- Empfang: `min(max(request, 0), capacity - stored)`. Erst subtrahieren, dann addieren; kein Überlauf bei `Long.MAX_VALUE`.
- Entnahme: `min(max(request, 0), stored)`.
- Simulation liest ausschließlich. Keine Komponenten, Identität, Netzwerkstatistik oder Slotbenachrichtigung werden erzeugt.
- FE-Int-Abfragen sättigen bei `Integer.MAX_VALUE`. Long-FE und FE verwenden denselben Bestand. Umrechnung in Mekanism-Energie erfolgt nur durch dessen bestehende Adapter.
- Netzwerkbedarf: `min(capacity - stored, logicalLimit - creditedThisCycle)`. Surge und Priorität werden vom normalen TransferHandler übernommen.
- Jede tatsächlich abgezogene Flux-Zuteilung wird unmittelbar genau einmal in der Batterie gespeichert. Es gibt keinen flüchtigen Zwischenpuffer. Slotbenachrichtigung erfolgt nach der Gutschrift.
- Der Server verarbeitet Bedarf, Plug-Entnahme und Gutschrift synchron. Abmeldung setzt den Teilnehmer vor neuen Transfers inaktiv; bereits gutgeschriebene Energie liegt im Item.
- Statistik meldet `-creditedThisCycle` als Point-Verbrauch. Der Transferpuffer des Hosted Points ist null; die Batterieenergie wird nicht erneut als Netzwerkspeicher gezählt.
- Summenbedarf und nichtnegative statistische Summen sättigen bei `Long.MAX_VALUE`, statt umzuschlagen. Summen jenseits des Long-Bereichs sind damit begrenzte Anzeigen; einzelne Energiebewegungen bleiben exakt.
- Creative-Kopien eines ganzen ItemStacks sind Minecraft-Kopien inklusive Energie und Identität. Das Addon verspricht keinen Schutz gegen Creative-/Admin-Klonen; normale Handlerkopien und Speichern/Lesen erzeugen keine Energie.

Automatisierte Nachweise: Grenzwerte, 10.000 deterministische Zufallsfälle, gemeinsame Fähigkeiten, Simulation, Komponentenpersistenz, Netzwerkverteilung, knappe Versorgung, volle Batterie und tatsächlicher Minerbetrieb. Siehe `test-results.md`.
