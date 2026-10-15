# Ladezeit-Prototyp (16.10.2026)

Referenz: `bridge-baseline-8.0.0` = `e119495`. Bridge-SHA256: `c24c8592e5c596164e6333e0d581d0d023a0e98d89fd488e151ceea21c6c6a64`.

Der erste isolierte Commit ergänzt nur `LoadTimePatched` und eine konstante Methode an der originalen `FluxNetwork`-Klasse. `-PloadTimePrototype` lädt ausschließlich `libs/FluxNetworks-1.21.1-8.0.0.jar`; die alten Ersatzklassen dienen in dieser Stufe ausschließlich noch als Compile-ABI. Energie-/Hostpfade werden im Prototyp nicht ausgeführt.

NeoForge 21.1.231 lädt CoreMods 7.0.3, ModLauncher 11.0.5 und Mixin 0.8.7. Die installierten Sources von `ClassTransformer` zeigen: BEFORE-Plugins → PRE_CLASS → Field-/Method-Transformer → CLASS-Transformer → AFTER-Plugins. `javap` auf `MixinTransformationHandler.handlesClass` bestätigt AFTER_ONLY. Der Kotlin-Mixin `FluxLoadOrderMixin` und die Coremod-Schnittstelle sind nach dem Laden gleichzeitig vorhanden. Innerhalb der CLASS-Phase wählen mehrere YES-Transformer keine vom Addon garantierbare Reihenfolge.

Technische Abweichung: Der frühe Einstieg ist ein JavaScript-Transformationsskript; sämtliche neuen JVM-Modklassen bleiben Kotlin. Das Skript löst nur freigegebene NeoForge-/ASM-Klassen auf und fügt symbolische Verweise auf die Kotlin-ABI ein. Es lädt weder Kotlin-Objects noch KFF-Klassen. Die frühe Transformation wurde vor „Kotlin For Forge Enabled!“ protokolliert; die ABI wird im Modkonstruktor nach KFF erfolgreich benutzt.

Die JavaScript-Sandbox erlaubt beispielsweise `java.lang.AssertionError` nicht. Außerdem fängt `CoreModBaseTransformer.transform` normale Exceptions ab und reicht den Knoten weiter. Deshalb ist ein bloßes `throw` kein zuverlässiger Pflichtschutz: Der eigentliche Umbau muss alle Pflichtmarker im Modkonstruktor prüfen, alle Zielklassen laden und bei fehlendem Marker den Start abbrechen. Marker dürfen erst nach vollständig erfolgreicher Transformation hinzugefügt werden.

Nachweise: `docs/evidence/prototype-server.txt` und `prototype-client.txt`. Dedicated Server bis „Done“, Client bis gerenderter Konfigurationsvorschau und automatischem Beenden. Keine Aussage über Energiepfade in dieser isolierten Stufe.

Quellen: installierte Source-JARs zu CoreMods 7.0.3 / ModLauncher 11.0.5, Mixin-Bytecode 0.8.7; Projektübersicht https://projects.neoforged.net/neoforged/coremods .
