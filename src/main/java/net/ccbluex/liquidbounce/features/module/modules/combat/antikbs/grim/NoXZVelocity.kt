package net.ccbluex.liquidbounce.features.module.modules.combat.antikbs.grim

import net.ccbluex.liquidbounce.Pride
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.antikbs.AntiKBMode
import net.ccbluex.liquidbounce.utils.RaycastUtils
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.network.play.client.*
import net.minecraft.network.play.server.SPacketEntityVelocity
import net.minecraft.network.play.server.SPacketExplosion
import kotlin.math.sqrt

class NoXZVelocity : AntiKBMode("NoXZ") {
    /**
     * VALUES
     */
    private var velocityTimer = MSTimer()
    private var velocityInput = false
    private var jump = false
    var sendsprint = false
    private var sprint = false
    private var sendc02 = false
    private var packetx = 0
    private var packetz = 0
    private var safemotion = 0
    private var reallyyaw: Float = 0F


    override fun onEnable() {
        reallyyaw = mc.player.rotationYaw
    }

    override fun onDisable() {
        packetx = 0
        packetz = 0
        safemotion = 0
        sendc02 = false
        sprint = false
        sendsprint = false
        jump = false
        mc.player?.speedInAir = 0.02F
    }

    override fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.player ?: return
        if (thePlayer.isInWater || thePlayer.isInLava || thePlayer.isInWeb)
            return

        sprint = mc.player!!.moveForward >= 0.8 && mc.gameSettings.keyBindForward.isKeyDown
        val sprinting = mc.player?.isSprinting ?: false
        if (!sprinting) return

        if (sendc02 && (mc.player!!.isSprinting || sendsprint)) {
            val motion = if (safemotion >= 2) 0.03f * safemotion else 0f
            mc.player.motionX *= motion
            mc.player.motionZ *= motion
            sendc02 = false
        }
        if (sendsprint && !sprint) {
            mc.connection!!.networkManager.sendPacket(
                CPacketEntityAction(
                    mc.player,
                    CPacketEntityAction.Action.STOP_SPRINTING
                )
            )
            sendsprint = false
        }
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        val killAura = Pride.moduleManager[KillAura::class.java] as KillAura
        if (packet is CPacketEntityAction && sendsprint) {
            if (packet.action == CPacketEntityAction.Action.START_SPRINTING && sprint) {
                event.cancelEvent()
                sendsprint = false
            }
        }
        if (packet is SPacketEntityVelocity) {


            if ((mc.world?.getEntityByID(packet.entityID) ?: return) != mc.player)
                return
            velocityTimer.reset()

            val sprinting = mc.player?.isSprinting ?: false
            if (!sprinting) return

            if (!sendsprint && !sprint) {
                mc.connection?.sendPacket(
                    CPacketEntityAction(
                        mc.player,
                        CPacketEntityAction.Action.START_SPRINTING
                    )
                )
                sendsprint = true
            }
            packetx = packet.motionX
            packetz = packet.motionZ

            safemotion =
                sqrt(packetx.toFloat() * packetx.toFloat() + packetz.toFloat() * packetz.toFloat()).toInt() / 4000 + if (killAura.target!!.isSprinting) 1 else 0 + if (!killAura.target!!.onGround) 1 else 0

            repeat(5) {
                mc.connection?.sendPacket(
                    CPacketUseEntity(killAura.target!!)
                )
                mc.connection?.sendPacket(CPacketAnimation())
                sendc02 = true
            }


        }else if (packet is SPacketExplosion) {
            // TODO: Support velocity for explosions
            event.cancelEvent()
        }
    }

    override fun onAttack(event:AttackEvent) {
        val killAura = Pride.moduleManager[KillAura::class.java] as KillAura
        val sprinting = mc.player?.isSprinting ?: false
        if (!sprinting) return

        if (killAura.state && killAura.target != null && sendsprint) {
            velocityInput = true
            repeat(6) {
                mc.connection?.sendPacket(
                    CPacketUseEntity(killAura.target!!)
                )
                mc.connection?.sendPacket(CPacketAnimation())
                sendc02 = true
            }
        }
    }


    override fun onJump(event: JumpEvent) {
        val thePlayer = mc.player
        if (thePlayer == null || thePlayer.isInWater || thePlayer.isInLava || thePlayer.isInWeb)
            return

    }
    fun jump(){
        if (mc.player.hurtTime == 7) mc.gameSettings.keyBindJump.pressed = true
        if (mc.player.hurtTime == 8) mc.gameSettings.keyBindJump.pressed = mc.gameSettings.keyBindJump.isKeyDown
        val yaw =mc.player!!.rotationYaw * 0.017453292F
    }
}
