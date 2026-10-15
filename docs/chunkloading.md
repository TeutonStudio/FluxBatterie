# Chunkloading

Eigener NeoForge-TicketController: **`fluxbattery:battery`**. Ticketbesitzer ist die Maschinenposition; genau deren Chunk wird tickend gehalten. Die normalen Flux-Tickets werden nicht benutzt.

Voraussetzungen: Serverfreigabe, globale Flux-Chunkloading-Freigabe, Itemoption, geeigneter Entladeslot in existierender Mekanism-Tile, gültiges Netzwerk und dauerhafte Rechte des Batterie-Besitzers. Die Kontingentprüfung zählt Besitzer dimensionenübergreifend. Keine Maschine aus `IMultiblock` wird unterstützt.

Entnahme, Maschinenabbau, Konfigurationswechsel oder Rechteverlust entfernen das Ticket. Deaktivierung der Serveroption wird am nächsten Maschinentick wirksam. Bei regulärem Chunk-Unload ohne eigenes Ticket bleibt nur der Itemzustand persistent.

Beim Serverstopp bleiben gültige Tickets gespeichert; ausschließlich die Laufzeitbindungen werden gelöst. Der Validierungscallback lädt die Ticketposition und prüft alle Voraussetzungen erneut. Er verwirft außerdem fremde Chunkpositionen, mehrere Chunks und nicht tickende Ticketformen. Bereits wiederhergestellte Tickets werden der neuen Laufzeitbindung zugeordnet, auch wenn `forceChunk(add=true)` wegen des vorhandenen Tickets keine Änderung meldet.

Ein Neustarttest mit zwei getrennten Server-JVMs speichert eine geladene Batterie und ein absichtlich verwaistes Ticket. Der zweite Prozess prüft Energie und Konfiguration, Wiederherstellung des gültigen Tickets, Verwerfen des verwaisten Tickets und sofortiges Entfernen nach Entnahme.

Der Controller lädt keine beliebig große Miner-Abbaufläche. Dafür gelten Mekanisms eigene Regeln und gegebenenfalls sein Anchor-Upgrade. Globale Server-/Modpack-Regeln können den Betrieb zusätzlich einschränken.
