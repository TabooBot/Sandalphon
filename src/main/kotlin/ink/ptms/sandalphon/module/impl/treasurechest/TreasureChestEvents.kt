package ink.ptms.sandalphon.module.impl.treasurechest

import ink.ptms.sandalphon.module.Helper
import ink.ptms.sandalphon.module.api.NMS
import ink.ptms.sandalphon.module.impl.treasurechest.data.ChestInventory
import ink.ptms.sandalphon.module.impl.treasurechest.data.open
import ink.ptms.sandalphon.module.impl.treasurechest.data.openEdit
import ink.ptms.sandalphon.util.Utils
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerEditBookEvent
import org.bukkit.inventory.meta.BookMeta
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.common.reflect.Reflex.Companion.getProperty
import taboolib.common.reflect.Reflex.Companion.invokeMethod
import taboolib.common.util.Vector
import taboolib.common.util.random
import taboolib.module.chat.uncolored
import taboolib.module.nms.MinecraftVersion
import taboolib.module.nms.PacketReceiveEvent
import taboolib.platform.util.giveItem
import taboolib.platform.util.isNotAir

/**
 * @author sky
 * @since 2020-05-29 22:00
 */
object TreasureChestEvents : Helper {

    fun toPosition(any: Any): Vector {
        val x = any.invokeMethod<Number>("getX")!!.toInt()
        val y = any.invokeMethod<Number>("getY")!!.toInt()
        val z = any.invokeMethod<Number>("getZ")!!.toInt()
        return Vector(x, y, z)
    }

    @SubscribeEvent
    fun e(e: PacketReceiveEvent) {
        if (e.packet.name == "PacketPlayInUseItem") {
            val a = e.packet.read<Any>("a")!!
            if (a.javaClass.simpleName == "BlockPosition") {
                val pos = toPosition(a)
                val loc = Location(e.player.world, pos.x, pos.y, pos.z)
                val chest = TreasureChest.getChest(loc.block) ?: return
                if (e.packet.read<Any>("c").toString() == "MAIN_HAND") {
                    submit {
                        if (e.player.isSneaking && e.player.isOp && e.player.inventory.itemInMainHand.type.isAir) {
                            chest.openEdit(e.player)
                        } else {
                            chest.open(e.player)
                        }
                    }
                }
            } else {
                val pos = toPosition(a.getProperty<Any>("c")!!)
                val loc = Location(e.player.world, pos.x, pos.y, pos.z)
                val chest = TreasureChest.getChest(loc.block) ?: return
                if (e.packet.read<Any>("b").toString() == "MAIN_HAND") {
                    submit {
                        if (e.player.isSneaking && e.player.isOp && e.player.inventory.itemInMainHand.type.isAir) {
                            chest.openEdit(e.player)
                        } else {
                            chest.open(e.player)
                        }
                    }
                }
            }
            e.isCancelled = true
        }
    }

    @SubscribeEvent(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun e(e: BlockBreakEvent) {
        if (TreasureChest.getChest(e.block) != null) {
            e.isCancelled = true
            e.player.info("你不能破坏宝藏.")
        }
    }

    @SubscribeEvent
    fun e(e: InventoryCloseEvent) {
        if (e.inventory.holder is ChestInventory) {
            e.inventory.viewers.remove(e.player)
            if (e.inventory.viewers.isEmpty()) {
                val chest = (e.inventory.holder as ChestInventory).chestData
                e.inventory.filter { item -> item.isNotAir() }.forEachIndexed { index, item ->
                    submit(delay = index.toLong()) {
                        (e.player as Player).giveItem(item)
                        (e.player as Player).playSound(e.player.location, Sound.ENTITY_ITEM_PICKUP, 1f, 2f)
                    }
                }
                e.inventory.clear()
                chest.globalInventory = null
                chest.globalTime = System.currentTimeMillis() + chest.update
                chest.tick(e.player as Player, true)
                e.player.world.players.forEach { p -> NMS.INSTANCE.sendBlockAction(p, chest.block.block, 1, 0) }
                e.player.world.playSound(chest.block, Sound.BLOCK_CHEST_CLOSE, 1f, random(0.8, 1.2).toFloat())
            }
        }
    }

    @SubscribeEvent
    fun e(e: PlayerEditBookEvent) {
        if (!e.player.isOp) {
            return
        }
        if (e.previousBookMeta.displayName.contains("编辑条件") && e.previousBookMeta.lore!![0].uncolored() == "TreasureChest") {
            val chestData = TreasureChest.getChest(Utils.toLocation(e.previousBookMeta.lore!![1].uncolored()).block)
            if (chestData == null) {
                e.player.error("该宝藏已失效. (${e.previousBookMeta.lore!![1].uncolored()})")
            } else {
                chestData.condition.clear()
                if (e.newBookMeta.pages[0].uncolored() != "clear") {
                    chestData.condition.addAll(e.newBookMeta.pages.flatMap { it.replace("§0", "").split("\n") })
                }
                if (MinecraftVersion.majorLegacy < 11300 && e.player.itemInHand.itemMeta is BookMeta) {
                    e.player.setItemInHand(null)
                }
            }
            e.isSigning = false
        }
    }
}