package de.teutonstudio.fluxbattery

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.UUIDUtil
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.Registries
import net.minecraft.network.codec.ByteBufCodecs
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.UUID
import java.util.function.Supplier
import com.mojang.serialization.DataResult

private val NON_NEGATIVE_LONG: Codec<Long> = Codec.LONG.validate { if (it >= 0) DataResult.success(it) else DataResult.error { "Negative energy" } }

val UNOWNED: UUID = UUID(0, 0)

data class BatteryConfig(
    val networkId: Int = 0,
    val ownerUuid: UUID = UNOWNED,
    val customName: String = "",
    val priority: Int = 0,
    val transferLimit: Long = 1_000_000,
    val disableTransferLimit: Boolean = false,
    val surgeMode: Boolean = false,
    val chunkLoadingEnabled: Boolean = false
) {
    companion object {
        val CODEC: Codec<BatteryConfig> = RecordCodecBuilder.create { i ->
            i.group(
                Codec.intRange(0, Int.MAX_VALUE).fieldOf("network_id").forGetter(BatteryConfig::networkId),
                UUIDUtil.CODEC.fieldOf("owner").forGetter(BatteryConfig::ownerUuid),
                Codec.STRING.fieldOf("name").forGetter(BatteryConfig::customName),
                Codec.intRange(-9999, 9999).fieldOf("priority").forGetter(BatteryConfig::priority),
                NON_NEGATIVE_LONG.fieldOf("limit").forGetter(BatteryConfig::transferLimit),
                Codec.BOOL.fieldOf("unlimited").forGetter(BatteryConfig::disableTransferLimit),
                Codec.BOOL.fieldOf("surge").forGetter(BatteryConfig::surgeMode),
                Codec.BOOL.fieldOf("chunk_loading").forGetter(BatteryConfig::chunkLoadingEnabled)
            ).apply(i, ::BatteryConfig)
        }
    }
}

data class BatteryEnergy(val storedEnergy: Long, val capacity: Long) {
    companion object {
        val CODEC: Codec<BatteryEnergy> = RecordCodecBuilder.create { i ->
            i.group(
                NON_NEGATIVE_LONG.fieldOf("stored").forGetter(BatteryEnergy::storedEnergy),
                NON_NEGATIVE_LONG.fieldOf("capacity").forGetter(BatteryEnergy::capacity)
            ).apply(i, ::BatteryEnergy)
        }.validate { if (it.storedEnergy <= it.capacity) DataResult.success(it) else DataResult.error { "Energy exceeds capacity" } }
    }
}

object BatteryComponents {
    val REGISTER = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, FluxBatteryMod.ID)
    val IDENTITY = REGISTER.register("identity", Supplier { DataComponentType.builder<UUID>().persistent(UUIDUtil.CODEC).networkSynchronized(ByteBufCodecs.fromCodec(UUIDUtil.CODEC)).build() })
    val CONFIG = REGISTER.register("configuration", Supplier { DataComponentType.builder<BatteryConfig>().persistent(BatteryConfig.CODEC).networkSynchronized(ByteBufCodecs.fromCodec(BatteryConfig.CODEC)).build() })
    val ENERGY = REGISTER.register("energy", Supplier { DataComponentType.builder<BatteryEnergy>().persistent(BatteryEnergy.CODEC).networkSynchronized(ByteBufCodecs.fromCodec(BatteryEnergy.CODEC)).build() })
}
