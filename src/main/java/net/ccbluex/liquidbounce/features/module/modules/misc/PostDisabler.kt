/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.AutoPot
import net.ccbluex.liquidbounce.features.module.modules.movement.Sneak
import net.ccbluex.liquidbounce.features.module.modules.world.ChestAura
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.network.play.client.CPacketConfirmTransaction
import net.minecraft.network.play.client.CPacketPlayer

@ModuleInfo(name = "PostDisabler", category = ModuleCategory.MISC, description = "No Post VL in GrimAC")
class PostDisabler : Module() {

    var timer = MSTimer()

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val serverData = mc.currentServerData
        val pingTime: Long
        if (LiquidBounce.moduleManager[ChestAura::class.java].state ||
            LiquidBounce.moduleManager[Sneak::class.java].state ||
            LiquidBounce.moduleManager[AutoPot::class.java].state) {

            if (serverData != null)
                pingTime = serverData.pingToServer
            else
                return


            if (event.packet !is CPacketPlayer) return
            if (!timer.hasTimePassed(pingTime)) return

            mc.connection!!.networkManager.sendPacket(CPacketConfirmTransaction())
            timer.reset()

        }
    }
}