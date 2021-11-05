package ink.ptms.sandalphon.module.impl.spawner

import ink.ptms.sandalphon.module.Helper
import ink.ptms.sandalphon.util.Utils
import io.lumine.xikage.mythicmobs.MythicMobs
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityTargetEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.metadata.FixedMetadataValue
import taboolib.common.platform.event.OptionalEvent
import taboolib.common.platform.event.SubscribeEvent
import taboolib.module.chat.uncolored
import taboolib.platform.BukkitPlugin
import taboolib.platform.util.hasLore
import taboolib.platform.util.hasName

/**
 * @author sky
 * @since 2020-05-29 13:08
 */
object SpawnerEvents : Helper {

    @SubscribeEvent
    fun e(e: EntityTargetEvent) {
        if (e.entity.hasMetadata("SPAWNER_BACKING")) {
            e.isCancelled = true
        }
    }

    @SubscribeEvent(bind = "io.lumine.xikage.mythicmobs.api.bukkit.events.MythicReloadedEvent")
    fun onMythicReloaded(e: OptionalEvent) {
        Spawner.spawners.forEach { it.mob = MythicMobs.inst().mobManager.getMythicMob(it.mob.internalName) }
    }

    @SubscribeEvent(bind = "io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent")
    fun onMythicMobDeath(e: OptionalEvent) {
        val event = e.get<MythicMobDeathEvent>()
        Spawner.spawners.forEach { spawnerData ->
            spawnerData.mobs.forEach { (k, v) ->
                if (v.uniqueId == event.entity.uniqueId) {
                    spawnerData.time[k] = System.currentTimeMillis() + (spawnerData.respawn * 1000L)
                    event.entity.setMetadata("RESPAWN", FixedMetadataValue(BukkitPlugin.getInstance(), spawnerData.respawn))
                }
            }
        }
    }

    @SubscribeEvent
    fun e(e: BlockBreakEvent) {
        if (e.player.isOp && e.player.inventory.itemInMainHand.hasName("拷贝魔杖") && e.player.inventory.itemInMainHand.hasLore("Spawner")) {
            e.isCancelled = true
            val location = Utils.toLocation(e.player.inventory.itemInMainHand.itemMeta!!.lore!![1].uncolored())
            val spawnerData = Spawner.getSpawner(location.block)
            if (spawnerData == null) {
                e.player.error("该方块不存在刷怪箱.")
                return
            }
            if (e.block.location !in spawnerData.copy) {
                e.player.error("该方块不被拷贝.")
                return
            }
            e.block.display()
            e.player.info("移除刷怪箱拷贝.")
            spawnerData.block.block.display()
            spawnerData.copy.remove(e.block.location)
            spawnerData.cancel(e.block.location)
            Spawner.export()
        }
    }

    @SubscribeEvent
    fun e(e: PlayerInteractEvent) {
        if (e.hand != EquipmentSlot.HAND) {
            return
        }
        if (e.player.isOp
            && e.action == Action.RIGHT_CLICK_BLOCK
            && e.player.inventory.itemInMainHand.hasName("拷贝魔杖")
            && e.player.inventory.itemInMainHand.hasLore("Spawner")
        ) {
            e.isCancelled = true
            val location = Utils.toLocation(e.player.inventory.itemInMainHand.itemMeta!!.lore!![1].uncolored())
            val spawnerData = Spawner.getSpawner(location.block)
            if (spawnerData == null) {
                e.player.error("该方块不存在刷怪箱.")
                return
            }
            if (e.clickedBlock!!.location in spawnerData.copy) {
                e.player.error("该方块已被拷贝.")
                return
            }
            e.clickedBlock!!.display()
            e.player.info("创建刷怪箱拷贝.")
            spawnerData.block.block.display()
            spawnerData.copy.add(e.clickedBlock!!.location)
            Spawner.export()
        }
    }
}