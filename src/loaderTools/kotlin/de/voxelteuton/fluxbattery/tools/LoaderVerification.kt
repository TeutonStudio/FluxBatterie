package de.voxelteuton.fluxbattery.tools

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile
import javax.script.Invocable

/** Uses the very same JS shape function and transformer as production, without loading Minecraft. */
object LoaderVerification {
    private val root = Path.of("").toAbsolutePath()
    private val names = listOf("common/connection/FluxNetwork", "common/connection/ServerFluxNetwork", "common/connection/ClientFluxNetwork", "common/connection/TransferIterator", "common/connection/NetworkStatistics", "common/connection/PhantomFluxDevice", "common/device/TileFluxDevice", "register/Messages").map { "sonar/fluxnetworks/$it" }
    @JvmStatic fun main(args: Array<String>) {
        val engine = NashornScriptEngineFactory().getScriptEngine("--language=es6")
        // ASMAPI uses NeoForge's per-coremod tracker; substitute logging and data loading only.
        val source = Files.readString(root.resolve("src/main/resources/coremods/fluxbattery.js"))
            .replace("var ASM = Java.type('net.neoforged.coremod.api.ASMAPI');", "var ASM = { log: function(level,message) { print(level + ': ' + message); } };")
        engine.eval(source)
        val inv = engine as Invocable
        val jar = JarFile(root.resolve("libs/FluxNetworks-1.21.1-8.0.0.jar").toFile())
        fun original(name: String) = ClassNode().also { ClassReader(jar.getInputStream(jar.getJarEntry("$name.class"))).accept(it, ClassReader.EXPAND_FRAMES) }
        fun quote(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\""
        val manifest = root.resolve("src/main/resources/coremods/fluxbattery-8.0.0.json")
        if (args.firstOrNull() == "generate") {
            val data = names.joinToString(",\n", "{\n", "\n}\n") { name ->
                val node = original(name)
                // Validate every rewritten method; Tile's unchanged methods remain open to other mods.
                val methods = if (name.endsWith("TileFluxDevice")) node.methods.filter { it.name in listOf("getTransferHandler", "markEnergyChanged", "disconnect", "sendBlockUpdate") }
                    else if (name.endsWith("Messages")) node.methods.filter { m -> m.instructions.toArray().any { it is InvokeDynamicInsnNode && it.bsmArgs.any { a -> a is org.objectweb.asm.Handle && a.name == "sendBlockUpdate" } } }
                    else node.methods
                quote(name) + ": {\"superName\":" + quote(node.superName) + ",\"fields\":" + node.fields.joinToString(",", "[", "]") { quote(it.name + ":" + it.desc + ":" + it.access) } + ",\"methods\":" + methods.joinToString(",", "{", "}") { quote(it.name + it.desc) + ":" + quote(inv.invokeFunction("shape", it).toString()) } + "}"
            }
            Files.writeString(manifest, data)
            println("Generated original 8.0.0 executable shapes: $manifest")
            return
        }
        engine.eval("expected = " + Files.readString(manifest))
        for (name in names) {
            val node = original(name)
            inv.invokeFunction("transform", node)
            check(node.methods.any { it.name == "fluxBatteryPatchVersion" }) { "Failed transformation: $name" }
            // All output classes must remain serializable. Runtime tests verify frames and JVM linkage.
            node.accept(ClassWriter(ClassWriter.COMPUTE_MAXS))
            val changed = original(name)
            val specMethod = changed.methods.first { it.instructions.size() > 0 && (name.endsWith("TileFluxDevice").not() || it.name == "disconnect") && (name.endsWith("Messages").not() || it.name.startsWith("lambda$") && it.instructions.toArray().any { n -> n is InvokeDynamicInsnNode && n.bsmArgs.any { a -> a is org.objectweb.asm.Handle && a.name == "sendBlockUpdate" } }) }
            specMethod.instructions.insert(InsnNode(Opcodes.NOP))
            inv.invokeFunction("transform", changed)
            check(changed.methods.none { it.name == "fluxBatteryPatchVersion" }) { "Changed bytecode accepted: $name" }
            check(changed.methods.any { it.name == "fluxBatteryPatchFailure" }) { "Missing incompatibility diagnostic: $name" }
            val access = original(name)
            access.methods.first { it.name == specMethod.name && it.desc == specMethod.desc }.access = specMethod.access xor Opcodes.ACC_STATIC
            inv.invokeFunction("transform", access)
            check(access.methods.none { it.name == "fluxBatteryPatchVersion" }) { "Changed method access accepted: $name" }
            val fieldAccess = original(name)
            if (fieldAccess.fields.isNotEmpty()) {
                fieldAccess.fields.first().access = fieldAccess.fields.first().access xor Opcodes.ACC_STATIC
                inv.invokeFunction("transform", fieldAccess)
                check(fieldAccess.methods.none { it.name == "fluxBatteryPatchVersion" }) { "Changed field access accepted: $name" }
            }
            val superclass = original(name)
            superclass.superName = "java/lang/UnsupportedOperationException"
            inv.invokeFunction("transform", superclass)
            check(superclass.methods.none { it.name == "fluxBatteryPatchVersion" }) { "Changed superclass accepted: $name" }
            val missing = original(name)
            missing.methods.removeIf { it.name == specMethod.name && it.desc == specMethod.desc }
            inv.invokeFunction("transform", missing)
            check(missing.methods.none { it.name == "fluxBatteryPatchVersion" }) { "Missing method accepted: $name" }
        }
        val fieldTargets = names.count { original(it).fields.isNotEmpty() }
        println("Loader verification passed: 8 originals, 8 changed bodies, 8 missing methods, 8 changed method flags, $fieldTargets changed field flags, 8 changed superclasses")
    }
}
