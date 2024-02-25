package net.ccbluex.liquidbounce.features.module.modules.misc


import me.utils.PacketUtils
import net.ccbluex.liquidbounce.Pride
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.client.gui.GuiDownloadTerrain
import net.minecraft.item.ItemBow
import net.minecraft.item.ItemBucketMilk
import net.minecraft.item.ItemFood
import net.minecraft.item.ItemPotion
import net.minecraft.item.ItemSword
import net.minecraft.network.INetHandler
import net.minecraft.network.Packet
import net.minecraft.network.login.server.SPacketEncryptionRequest
import net.minecraft.network.play.INetHandlerPlayServer
import net.minecraft.network.play.client.*
import net.minecraft.network.play.server.*
import net.minecraft.network.status.server.SPacketPong
import net.minecraft.network.status.server.SPacketServerInfo
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.sqrt


@ModuleInfo(name = "Disabler", description = "Disabler", category = ModuleCategory.MISC)
class Disabler : Module() {
    val post =BoolValue("Post",true)
    private val AutoBlockFix = BoolValue("RotaionPlace",true)
    //BadPacketA Dis 可以都开
    val badpacketA= BoolValue("BadPacketA1",true)//第一种DisBadA的方式
    private val badpacketa = BoolValue("BadPacketA2",true)//第二种DisBadA的方式
    var lastSprinting = false
    private val badpacketf = BoolValue("BadPacketF", false)
    private val badpackett = BoolValue("BadPacketT",true)
    private val fastBreak = BoolValue("FastBreak",true)
    val modeValue = ListValue("Mode", arrayOf("GrimAC"), "GrimAC")
    private var lastSlot: Int = -1
    private val hasLegacyExpansion: Boolean = true
    private val maxXZ = 0.3001 + (if (hasLegacyExpansion) 0.1 else 0.0)
    private val minY = -0.0001 - (if (hasLegacyExpansion) 0.1 else 0.0)
    private val maxY = 1.8001 + (if (hasLegacyExpansion) 0.1 else 0.0)
    private var blockPos: BlockPos? = null
    private var enumFacing: EnumFacing? = null


    @EventTarget
    fun onPacket(event: PacketEvent){
        val packet = event.packet
        val our = mc.player.heldItemMainhand.item is ItemFood || mc.player.heldItemMainhand.item is ItemPotion || mc.player.heldItemMainhand.item is ItemBucketMilk || mc.player.heldItemMainhand.item is ItemBow
        val killAura = Pride.moduleManager.getModule(KillAura::class.java) as KillAura
        if (packet is CPacketPlayerTryUseItemOnBlock && ( killAura.currentTarget != null || our) && AutoBlockFix.get()) {
            event.cancelEvent()
        }
        if(badpacketA.get() && packet is CPacketHeldItemChange){
            val slot: Int = packet.slotId
            if (slot == this.lastSlot && slot != -1) {
                event.cancelEvent()
            }
            this.lastSlot = packet.slotId

        }
        if(badpacketa.get() && mc.world != null) {
            if(packet is CPacketHeldItemChange) {
                if(packet.slotId == lastSlot) {
                    event.cancelEvent()
                }
                lastSlot = packet.slotId
            }
        }
        if (badpacketf.get()) {
            //BadPacketF
            if (packet is CPacketEntityAction) {
                if (packet.action === CPacketEntityAction.Action.START_SPRINTING) {
                    if (lastSprinting) event.cancelEvent()
                    lastSprinting = true
                } else if (packet.action === CPacketEntityAction.Action.STOP_SPRINTING) {
                    if (!lastSprinting) event.cancelEvent()
                    lastSprinting = false
                }
            }
        }
        if(badpackett.get() && mc.world != null) {
            if(packet is CPacketUseEntity) {
                if(packet.action == CPacketUseEntity.Action.INTERACT) {
                    if(mc.player.heldItemMainhand.item is ItemSword) {
                        val target = killAura.target!!
                        if(target.posX > minY && target.posY < maxY && Math.abs(target.posX) < maxXZ && Math.abs(target.posZ) < maxXZ) {
                            event.cancelEvent()
                        }
                    }
                }
            }
        }
    }
    @EventTarget
    fun onWorld(event: WorldEvent) {
        lastSlot = -1
    }
    companion object {

        // Static
        @JvmStatic
        var storedPackets: MutableList<Packet<INetHandler>> = CopyOnWriteArrayList()
        @JvmStatic
        var pingPackets: ConcurrentLinkedDeque<Int> = ConcurrentLinkedDeque()
        @JvmStatic
        private var lastResult = false
    }
    fun getGrimPost(): Boolean {
        val disabler = Pride.moduleManager.getModule(Disabler::class.java) as Disabler
        val result = disabler.state && modeValue.get()=="GrimAC" && post.get()
                && mc.player != null
                && mc.player.isEntityAlive
                && mc.player.ticksExisted >= 10
                && mc.currentScreen !is GuiDownloadTerrain

        if (lastResult && !result) {
            lastResult = false
            mc.addScheduledTask { processPackets() }
        }
        return result.also { lastResult = it }
    }

    fun processPackets() {

        if (storedPackets.isNotEmpty()) {
            for (packet in storedPackets) {
                val event = PacketEvent(packet)
                Pride.eventManager.callEvent(event)
                if (event.isCancelled) {
                    continue
                }
                packet.processPacket(mc.connection as INetHandler)
            }
            storedPackets.clear()
        }
    }

    fun grimPostDelay(packet: Packet<*>): Boolean {
        if (mc.player == null) {
            return false
        }
        if (mc.currentScreen is GuiDownloadTerrain) {
            return false
        }
        if (packet is SPacketServerInfo) {
            return false
        }
        if (packet is SPacketEncryptionRequest) {
            return false
        }
        if (packet is SPacketPlayerListItem) {
            return false
        }
        if (packet is SPacketDisconnect) {
            return false
        }
        if (packet is SPacketChunkData) {
            return false
        }
        if (packet is SPacketPong) {
            return false
        }
        if (packet is SPacketWorldBorder) {
            return false
        }
        if (packet is SPacketJoinGame) {
            return false
        }
        if (packet is SPacketEntityHeadLook) {
            return false
        }
        if (packet is SPacketTeams) {
            return false
        }
        if (packet is SPacketChat) {
            return false
        }
        if (packet is SPacketSetSlot) {
            return false
        }
        if (packet is SPacketEntityMetadata) {
            return false
        }
        if (packet is SPacketEntityProperties) {
            return false
        }
        if (packet is SPacketUpdateTileEntity) {
            return false
        }
        if (packet is SPacketTimeUpdate) {
            return false
        }
        if (packet is SPacketPlayerListHeaderFooter) {
            return false
        }
        if (packet is SPacketEntityVelocity) {
            val sPacketEntityVelocity: SPacketEntityVelocity = packet
            return sPacketEntityVelocity.entityID == mc.player.entityId
        }
        return packet is SPacketExplosion
                || packet is SPacketConfirmTransaction
                || packet is SPacketPlayerPosLook
                //  || packet is SPacketEntityTeleport
                //  || packet is SPacketEntityStatus
                || packet is SPacketEntityEquipment
                || packet is SPacketBlockChange
                || packet is SPacketMultiBlockChange
                //   || packet is SPacketDestroyEntities
                || packet is SPacketKeepAlive
                || packet is SPacketUpdateHealth
                || packet is SPacketEntity
                || packet is SPacketSpawnMob
                || packet is SPacketCustomPayload
    }
    fun fixC0F(packet: CPacketConfirmTransaction) {
        val id: Int = packet.uid.toInt()
        if (id >= 0 || pingPackets.isEmpty()) {
            PacketUtils.sendPacketNoEvent(packet)
        } else {
            do {
                val current: Int = pingPackets.first
                PacketUtils.sendPacketNoEvent(CPacketConfirmTransaction(packet.windowId, current.toShort(), true))
                pingPackets.pollFirst()
                if (current == id) {
                    break
                }
            } while (!pingPackets.isEmpty())
        }
    }
    @EventTarget
    fun onClickBlock(event: ClickBlockEvent) {
        blockPos = event.clickedBlock ?: return
        enumFacing = event.WEnumFacing ?: return
        if (fastBreak.get()) {
            mc.connection?.sendPacket(
                CPacketPlayerDigging(
                    CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK,
                    blockPos!!,
                    enumFacing!!
                )
            )
        }
    }
    override val tag: String
        get() = "Grim"
}

