package de.voxelteuton.fluxbattery.mixin

import de.voxelteuton.fluxbattery.FluxBatteryHostManager
import mekanism.common.tile.base.TileEntityMekanism
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(value = [TileEntityMekanism::class], remap = false)
abstract class MekanismTickMixin {
    @Inject(method = ["setRemoved"], at = [At("HEAD")], remap = false)
    private fun removeBatteryHost(ci: CallbackInfo) {
        FluxBatteryHostManager.remove(this as Any as TileEntityMekanism)
    }
    private companion object {
        @JvmStatic
        @Inject(method = ["tickServer"], at = [At("HEAD")], remap = false)
        private fun tickBatteryHost(level: Level, pos: BlockPos, state: BlockState, tile: TileEntityMekanism, ci: CallbackInfo) {
            FluxBatteryHostManager.tick(tile)
        }
    }
}
