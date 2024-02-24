package net.ccbluex.liquidbounce.features.module.modules.combat.antikbs.grim

import net.ccbluex.liquidbounce.Pride
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.combat.AntiKnockback
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.antikbs.AntiKBMode
import net.ccbluex.liquidbounce.injection.forge.mixins.util.MixinTimer
import net.ccbluex.liquidbounce.injection.implementations.IMixinTimer
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.RaycastUtils
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.client.Minecraft
import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraft.entity.Entity
import net.minecraft.network.Packet
import net.minecraft.network.play.client.*
import net.minecraft.network.play.server.SPacketEntityVelocity
import net.minecraft.network.play.server.SPacketExplosion
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import op.wawa.utils.PacketUtils

class NoXZVelocity : AntiKBMode("NoXZ") {
    private var isvel = false
    private var velx = 0f
    private var velz = 0f
    private var vely = 0f
    private var attacked = false;
    private var velocityInput = false
    private var press = false
    private var grimReduce = 0
    private var grimNoAntiKB = 0
    private var cancel = false

    override fun onDisable() {
        attacked = false
        velocityInput = false
        cancel = false
        grimNoAntiKB = 0
        mc.player.speedInAir = 0.02F
        grimReduce = 0
        (mc.timer as IMixinTimer).timerSpeed = 1f
        press = false
    }
    override fun onUpdate(event: UpdateEvent) {
        if (mc.player.isInWater || mc.player.isInLava || mc.player.isInWeb)
            return

        if (isvel) {
            if (attacked) {
                if (attacked) {
                    if (MathHelper.sqrt(velx * velx + velz * velz) <= 2) {
                        if (mc.player.onGround) {
                            mc.player.motionX = velx * 0.0001
                            mc.player.motionZ = velz * 0.0001
                        } else {
                            mc.player.motionX = velx * 0.02
                            mc.player.motionZ = velz * 0.02
                        }
                    } else {
                        mc.player.motionX = velx * 0.15
                        mc.player.motionZ = velz * 0.15

                    }
                }
                attacked = false
            } else if (mc.player.hurtTime == 6 && mc.player.onGround && !mc.gameSettings.keyBindJump.isKeyDown) {
                mc.player.movementInput.jump = true
            }
            if(mc.player.hurtTime == 0){
                isvel = false
            }
        }

    }
    override fun onTick(event: TickEvent) {
        if (velocityInput) {
            val reach = 3.0
            val currentRotation = RotationUtils.serverRotation!!

            val raycastedEntity = RaycastUtils.raycastEntity(
                reach,
                currentRotation.yaw,
                currentRotation.pitch,
                object : RaycastUtils.EntityFilter {
                    override fun canRaycast(entity: Entity?): Boolean {
                        return true
                    }

                })

            if (raycastedEntity != null && raycastedEntity != mc.player) {
                repeat(4) {
                    Pride.eventManager.callEvent(AttackEvent(raycastedEntity))
                    mc.connection?.sendPacket(
                        CPacketUseEntity(
                            raycastedEntity,
                            EnumHand.MAIN_HAND
                        )
                    )
                    mc.player!!.swingArm(EnumHand.MAIN_HAND)
                }
            }
        }
    }

    override fun onPacket(event: PacketEvent) {

        val packet = event.packet

        if (packet is SPacketEntityVelocity) {
            if (mc.player == null || (mc.world?.getEntityByID(packet.entityID) ?: return) != mc.player)
                return

            if (packet.entityID != mc.player.entityId) return
            val targets = (Pride.moduleManager[KillAura::class.java] as KillAura).currentTarget!!
            if (mc.player.getDistance(targets) < 3.01) {

                isvel = true

                for (i in 0..15) {
                    if (mc.player.serverSprintState && MovementUtils.isMoving) {
                        PacketUtils.sendPacketNoEvent(CPacketConfirmTransaction())
                        PacketUtils.sendPacketNoEvent(CPacketAnimation(EnumHand.MAIN_HAND));
                        PacketUtils.sendPacketNoEvent(CPacketUseEntity(targets));
                    } else {
                        PacketUtils.sendPacketNoEvent(CPacketConfirmTransaction())
                        PacketUtils.sendPacketNoEvent(
                            CPacketEntityAction(
                                mc.player,
                                CPacketEntityAction.Action.START_SPRINTING
                            )
                        );
                        mc.player.isSprinting = false
                        PacketUtils.sendPacketNoEvent(CPacketAnimation(EnumHand.MAIN_HAND));
                        PacketUtils.sendPacketNoEvent(CPacketUseEntity(targets));
                        PacketUtils.sendPacketNoEvent(
                            CPacketEntityAction(
                                mc.player,
                                CPacketEntityAction.Action.STOP_SPRINTING
                            )
                        );
                    }
                }
                velx = packet.getMotionX() / 8000f
                velz = packet.getMotionZ() / 8000f
                vely = packet.getMotionY() / 8000f
                attacked = true
            }

        }

        if (packet is SPacketExplosion) {
            event.cancelEvent()
        }
    }
}
