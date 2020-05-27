package ink.ptms.sandalphon.module.impl.hologram

import ink.ptms.sandalphon.module.impl.hologram.data.HologramData
import ink.ptms.sandalphon.util.Utils
import io.izzel.taboolib.module.db.local.LocalFile
import io.izzel.taboolib.module.inject.TFunction
import io.izzel.taboolib.module.inject.TSchedule
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration

/**
 * @Author sky
 * @Since 2020-05-27 11:17
 */
object Hologram {

    @LocalFile("module/hologram.yml")
    lateinit var data: FileConfiguration
        private set

    val holograms = ArrayList<HologramData>()

    @TSchedule
    fun import() {
        if (Bukkit.getPluginManager().getPlugin("Cronus") == null) {
            return
        }
        holograms.clear()
        data.getKeys(false).forEach {
            holograms.add(HologramData(it, Utils.toLocation(data.getString("$it.location")!!), data.getStringList("$it.content"), data.getStringList("$it.condition")))
        }
    }

    @TFunction.Cancel
    fun export() {
        holograms.forEach { holo ->
            data.set("${holo.id}.location", Utils.fromLocation(holo.location))
            data.set("${holo.id}.content", holo.holoContent)
            data.set("${holo.id}.condition", holo.holoCondition)
        }
    }

    fun delete(id: String) {
        data.set(id, null)
    }

    @TFunction.Cancel
    fun cancel() {
        holograms.forEach { it.cancel() }
    }

    @TSchedule(period = 20)
    fun e() {
        Bukkit.getOnlinePlayers().forEach { player ->
            holograms.filter { it.location.world?.name == player.world.name }.forEach {
                it.refresh(player)
            }
        }
    }

    fun getHologram(id: String): HologramData? {
        return holograms.firstOrNull { it.id == id }
    }
}