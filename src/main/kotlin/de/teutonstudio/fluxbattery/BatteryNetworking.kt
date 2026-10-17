package de.teutonstudio.fluxbattery

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.network.chat.Component
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import sonar.fluxnetworks.common.connection.FluxNetworkData
import java.util.UUID

private fun id(path: String) = ResourceLocation.fromNamespaceAndPath(FluxBatteryMod.ID, path)

data class NetworkChoice(val id: Int, val name: String)

data class BatterySnapshot(val identity: UUID, val hand: InteractionHand, val config: BatteryConfig, val energy: BatteryEnergy, val networks: List<NetworkChoice>) : CustomPacketPayload {
    override fun type() = TYPE
    companion object {
        val TYPE = CustomPacketPayload.Type<BatterySnapshot>(id("snapshot"))
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, BatterySnapshot> = StreamCodec.of({ buf, value ->
            buf.writeUUID(value.identity)
            buf.writeEnum(value.hand)
            buf.writeJsonWithCodec(BatteryConfig.CODEC, value.config)
            buf.writeLong(value.energy.storedEnergy); buf.writeLong(value.energy.capacity)
            buf.writeVarInt(value.networks.size)
            value.networks.forEach { buf.writeInt(it.id); buf.writeUtf(it.name, 128) }
        }, { buf ->
            val identity = buf.readUUID(); val hand = buf.readEnum(InteractionHand::class.java)
            val config = buf.readJsonWithCodec(BatteryConfig.CODEC)
            val energy = BatteryEnergy(buf.readLong(), buf.readLong())
            val count = buf.readVarInt(); require(count in 0..4096)
            BatterySnapshot(identity, hand, config, energy, List(count) { NetworkChoice(buf.readInt(), buf.readUtf(128)) })
        })
    }
}

data class BatteryUpdate(val identity: UUID, val hand: InteractionHand, val config: BatteryConfig) : CustomPacketPayload {
    override fun type() = TYPE
    companion object {
        val TYPE = CustomPacketPayload.Type<BatteryUpdate>(id("update"))
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, BatteryUpdate> = StreamCodec.of({ buf, value ->
            buf.writeUUID(value.identity); buf.writeEnum(value.hand)
            buf.writeJsonWithCodec(BatteryConfig.CODEC, value.config)
        }, { buf -> BatteryUpdate(buf.readUUID(), buf.readEnum(InteractionHand::class.java), buf.readJsonWithCodec(BatteryConfig.CODEC)) })
    }
}

object BatteryNetworking {
    // The client-only subscriber installs this. Common/server classes never reference a Screen.
    var clientReceiver: (BatterySnapshot) -> Unit = {}
    fun register(event: RegisterPayloadHandlersEvent) {
        val registrar = event.registrar("1")
        registrar.playToClient(BatterySnapshot.TYPE, BatterySnapshot.CODEC) { payload, _ -> clientReceiver(payload) }
        registrar.playToServer(BatteryUpdate.TYPE, BatteryUpdate.CODEC) { payload, context ->
            val player = context.player() as ServerPlayer
            val accepted = applyUpdate(player, payload)
            player.displayClientMessage(Component.literal(if (accepted) "Flux Battery: configuration saved" else "Flux Battery: configuration rejected (item or permission changed)"), accepted)

        }
    }
    fun applyUpdate(player: ServerPlayer, payload: BatteryUpdate): Boolean {
        val stack = player.getItemInHand(payload.hand)
        val old = BatteryStateService.config(stack)
        val config = payload.config.copy(ownerUuid = player.uuid)
        val validStack = stack.item is FluxBatteryItem && stack.get(BatteryComponents.IDENTITY.get()) == payload.identity && (old.ownerUuid == UNOWNED || old.ownerUuid == player.uuid)
        val validConfig = config.customName.length <= 24 && config.priority in -9999..9999 && config.transferLimit >= 0 && config.networkId >= 0 &&
            (config.networkId == 0 || FluxBatteryHostManager.permitted(FluxNetworkData.getNetwork(config.networkId), config))
        if (!validStack || !validConfig) return false
        BatteryStateService.setConfig(stack, config)
        player.inventory.setChanged()
        player.containerMenu.broadcastChanges()
        return true
    }
    fun open(player: ServerPlayer, hand: InteractionHand) {
        val stack = player.getItemInHand(hand)
        val config = BatteryStateService.config(stack)
        if (config.ownerUuid != UNOWNED && config.ownerUuid != player.uuid) {
            player.displayClientMessage(Component.literal("Only the battery owner may configure this item."), false)
            return
        }
        val identity = BatteryStateService.identity(stack)
        val networks = FluxNetworkData.getAllNetworks().filter { FluxBatteryHostManager.permitted(it, config.copy(ownerUuid = player.uuid)) }
            .sortedBy { it.networkID }.take(4095).map { NetworkChoice(it.networkID, it.networkName.take(128)) }
        PacketDistributor.sendToPlayer(player, BatterySnapshot(identity, hand, config, BatteryStateService.energy(stack), listOf(NetworkChoice(0, "Disconnected")) + networks))
    }
}
