package net.ccbluex.liquidbounce.features.module.modules.combat.antikbs.grim

import net.ccbluex.liquidbounce.Pride
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.antikbs.AntiKBMode
import net.ccbluex.liquidbounce.features.module.modules.movement.StrafeFix
import net.ccbluex.liquidbounce.features.module.modules.world.Scaffold
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.minecraft.block.material.Material
import net.minecraft.network.play.client.*
import net.minecraft.network.play.server.SPacketEntityVelocity

class NoXZVelocity : AntiKBMode("NoXZ") {
    /**
     * VALUES
     */
    private var attack = false
    private var velocityInput = false
    private var velocityed = false
    private var jump = false
    var sendsprint = false
    private var sprint = false
    private var sendc02 = false
    private var packetx = 0
    private var packetz = 0
    private var safemotion = 0
    private var jumped = 0
    private var reallyyaw: Float = 0F


    override fun onEnable() {
        reallyyaw = mc.player.rotationYaw
    }

    override fun onDisable() {
        attack = false
        packetx = 0
        packetz = 0
        safemotion = 0
        sendc02 = false
        sprint = false
        sendsprint = false
        jump = false
        velocityed = false
        mc.player?.speedInAir = 0.02F
    }

    override fun onUpdate(event: UpdateEvent) {
        val sca = Pride.moduleManager[Scaffold::class.java] as Scaffold
        val thePlayer = mc.player ?: return
        if (thePlayer.isInWater || thePlayer.isInLava || thePlayer.isInWeb || !thePlayer.isInsideOfMaterial(Material.AIR))
            return

        if (velocityInput) {
            if (attack) {
                velocityInput = false
                attack = false
            } else {
                //Bypass all ac
                if (mc.player.hurtTime > 0){
                    mc.player.motionX += -1.0E-7
                    mc.player.motionY += -1.0E-7
                    mc.player.motionZ += -1.0E-7
                    mc.player.isAirBorne = true
                }
                velocityInput = false
                attack = false
            }
        }
        if (velocityed){
            if (mc.player!!.onGround && mc.player!!.hurtTime == 9) {
                if (jumped > 2) {
                    jumped = 0
                } else {
                    ++jumped
                    if (mc.player.ticksExisted % 5 != 0) mc.gameSettings.keyBindJump.pressed = true
                }
            } else if (mc.player!!.hurtTime == 8) {
                mc.gameSettings.keyBindJump.pressed = mc.gameSettings.keyBindJump.isKeyDown
                velocityInput = false
            }
        }

        if (mc.player.hurtTime == 0){
            if(!sca.state) mc.gameSettings.keyBindJump.pressed = mc.gameSettings.keyBindJump.isKeyDown
        }
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        val killAura = Pride.moduleManager[KillAura::class.java] as KillAura
        val strafeFix = Pride.moduleManager[StrafeFix::class.java] as StrafeFix
        if (packet is SPacketEntityVelocity) {
            velocityInput = true
            if (mc.player.onGround) mc.gameSettings.keyBindJump.pressed = true
            if (killAura.state && killAura.currentTarget != null && mc.player.getDistanceToEntityBox(killAura.currentTarget!!) <= 3.01 && !strafeFix.state) {

                // is sprinting
                if (mc.player.movementInput.moveForward > 0.9f && mc.player.isSprinting && mc.player.serverSprintState) {
                    repeat(5) {
                        mc.connection!!.sendPacket(CPacketConfirmTransaction(100, 100, true))
                        mc.connection!!.sendPacket(CPacketUseEntity(killAura.currentTarget!!))
                        mc.connection!!.sendPacket(CPacketAnimation())
                    }

                    if (mc.player.collidedHorizontally && !mc.player.isOnLadder && !mc.player.isInWater && !mc.player.isInLava) return
                    attack = true
                } else {
                    if (mc.player.movementInput.moveForward > 0.9f) {
                        repeat(5) {
                            mc.connection!!.sendPacket(CPacketConfirmTransaction(100, 100, true))
                            mc.connection!!.networkManager.sendPacket(CPacketEntityAction(mc.player, CPacketEntityAction.Action.START_SPRINTING))
                            mc.connection!!.sendPacket(CPacketUseEntity(killAura.currentTarget!!))
                            mc.connection!!.sendPacket(CPacketAnimation())
                        }

                        if (mc.player.collidedHorizontally && !mc.player.isOnLadder && !mc.player.isInWater && !mc.player.isInLava) return
                        attack = true
                    }
                }
            }
        }
    }
}
