package de.voxelteuton.fluxbattery.bridge

import net.neoforged.fml.ModList
import net.neoforged.fml.loading.FMLEnvironment
import java.security.MessageDigest
import java.nio.file.Files

object RuntimePatchGuard {
    fun verify() {
        val expected = "29e511b2d76f85bd247aa213a92bb059ecc037fcc3fed82baffcff97b638d3a6"
        val file = ModList.get().getModFileById("fluxnetworks")
        check(file.versionString() == "8.0.0") { "Flux Battery supports only original Flux Networks 8.0.0; found ${file.versionString()}" }
        val digest = Files.newInputStream(file.file.filePath).use { input ->
            val sha = MessageDigest.getInstance("SHA-256")
            val bytes = ByteArray(65536)
            while (true) { val size = input.read(bytes); if (size < 0) break; sha.update(bytes, 0, size) }
            sha.digest().joinToString("") { "%02x".format(it) }
        }
        check(digest == expected) { "Flux Battery requires the unchanged Flux Networks 8.0.0 JAR (SHA-256 $expected); found $digest. Remove replacement/modified Flux JARs." }
        val targets = mutableListOf("common.connection.FluxNetwork", "common.connection.ServerFluxNetwork", "common.connection.TransferIterator", "common.connection.NetworkStatistics", "common.connection.PhantomFluxDevice", "common.device.TileFluxDevice", "register.Messages")
        if (FMLEnvironment.dist.isClient) targets.add("common.connection.ClientFluxNetwork")
        for (target in targets) {
            val name = "sonar.fluxnetworks.$target"
            try {
                val type = Class.forName(name, true, javaClass.classLoader)
                val failure = runCatching { type.getDeclaredMethod("fluxBatteryPatchFailure").invoke(null) }.getOrNull()
                check(failure == null) { "$failure" }
                check(type.getDeclaredMethod("fluxBatteryPatchVersion").invoke(null) == 1) { "wrong patch marker" }
            } catch (error: Throwable) {
                throw IllegalStateException("Flux Battery mandatory runtime patch failed for $name. Expected original Flux Networks 8.0.0 with compatible transformers. ${error.message}", error)
            }
        }
        check(FluxRuntimeDevice::class.java.isAssignableFrom(sonar.fluxnetworks.common.device.TileFluxDevice::class.java))
        check(FluxRuntimeNetwork::class.java.isAssignableFrom(sonar.fluxnetworks.common.connection.FluxNetwork::class.java))
        org.slf4j.LoggerFactory.getLogger("FluxBattery").info("Flux Battery: all mandatory original-8.0.0 runtime patches verified")
    }
}
