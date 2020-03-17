package ink.ptms.sandalphon.module.impl

import com.google.common.collect.Lists
import ink.ptms.sandalphon.Sandalphon
import ink.ptms.sandalphon.module.IModule
import io.izzel.taboolib.TabooLib
import io.izzel.taboolib.Version
import io.izzel.taboolib.module.command.lite.CommandBuilder
import io.izzel.taboolib.module.inject.TInject
import io.izzel.taboolib.util.ArrayUtil
import io.izzel.taboolib.util.lite.Numbers
import org.bukkit.Bukkit
import org.bukkit.Effect
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Directional
import org.bukkit.command.BlockCommandSender
import org.bukkit.util.NumberConversions

/**
 * @Author sky
 * @Since 2020-02-15 17:32
 */
object CommandBlockControl : IModule() {

    val map = HashMap<Location, Long>()
    val mapIndex = HashMap<Location, Data>()

    @TInject
    val cbc = CommandBuilder.create("CommandBlockControl", null)
            .aliases("cbc")
            .permission("*")
            .execute { sender, args ->
                if (args.size < 2) {
                    notify(sender, "/cbc [period] [command]")
                    return@execute
                }
                if (sender !is BlockCommandSender) {
                    notify(sender, "This command only allows the use of BlockCommand.")
                    return@execute
                }
                if (System.currentTimeMillis() < map[sender.block.location] ?: 0L) {
                    return@execute
                }
                when {
                    // 充能
                    args[1].startsWith("powered") -> try {
                        val block = sender.block.getRelative(getCommandBlockFace(sender.block))
                        // 短充能
                        if (args[1].startsWith("powered:")) {
                            block.type = Material.REDSTONE_BLOCK
                            Bukkit.getScheduler().runTaskLater(Sandalphon.getPlugin(), Runnable { block.type = Material.GLASS }, NumberConversions.toLong(args[1].substring("powered:".length)))
                        }
                        // 长充能
                        else {
                            block.type = if (block.type == Material.REDSTONE_BLOCK) Material.GLASS else Material.REDSTONE_BLOCK
                        }
                    } catch (t: Throwable) {
                        t.printStackTrace()
                    }
                    // 选取
                    args[1].startsWith("selected") -> try {
                        val collect = collect(sender.block, getCommandBlockFace(sender.block))
                        if (collect.isEmpty()) {
                            return@execute
                        } else {
                            collect.filter { it.type != Material.GLASS }.forEach { it.type = Material.GLASS }
                        }
                        val index = mapIndex.computeIfAbsent(sender.block.location) { Data(0, false) }
                        val block = collect.getOrNull(index.index)
                        if (block != null) {
                            block.type = Material.REDSTONE_BLOCK
                            Bukkit.getScheduler().runTaskAsynchronously(Sandalphon.getPlugin(), Runnable {
                                block.world.playEffect(block.location, Effect.STEP_SOUND, block.type)
                            })
                            // 短选取
                            if (args[1].startsWith("selected:")) {
                                Bukkit.getScheduler().runTaskLater(Sandalphon.getPlugin(), Runnable { block.type = Material.GLASS }, NumberConversions.toLong(args[1].substring("selected:".length)))
                            }
                        }
                        when (args.getOrElse(2) { "cycle" }) {
                            "cycle", "1" -> {
                                if (index.desc) {
                                    if (index.index > 0) {
                                        index.index--
                                        index.desc = index.index != 0
                                    } else {
                                        index.index = collect.size - 1
                                    }
                                } else {
                                    if (index.index < collect.size - 1) {
                                        index.index++
                                        index.desc = index.index == collect.size - 1
                                    } else {
                                        index.index = 0
                                    }
                                }
                            }
                            "repeat", "2" -> {
                                if (index.index < collect.size - 1) {
                                    index.index++
                                } else {
                                    index.index = 0
                                }
                            }
                            "random", "3" -> {
                                index.index = Numbers.getRandom().nextInt(collect.size)
                            }
                            else -> {
                            }
                        }
                    } catch (t: Throwable) {
                        t.printStackTrace()
                    }
                    // 命令
                    else -> try {
                        Bukkit.dispatchCommand(sender, ArrayUtil.arrayJoin(args, 1))
                    } catch (t: Throwable) {
                        t.printStackTrace()
                    }
                }
                map[sender.block.location] = System.currentTimeMillis() + (NumberConversions.toInt(args[0]) * 50L)
            }

    fun collect(block: Block, face: BlockFace): List<Block> {
        val list = Lists.newArrayList<Block>()
        var pos = block
        while (true) {
            pos = pos.getRelative(face)
            if (pos.type == Material.GLASS || pos.type == Material.REDSTONE_BLOCK) {
                list.add(pos)
            } else {
                break
            }
        }
        return list
    }

    fun getCommandBlockFace(block: Block): BlockFace {
        return if (Version.isAfter(Version.v1_13)) {
            (block.blockData as Directional).facing
        } else {
            getCommandBlockFace(block.data.toInt())
        }
    }

    fun getCommandBlockFace(data: Int): BlockFace {
        return when (data) {
            0 -> BlockFace.DOWN
            1 -> BlockFace.UP
            2 -> BlockFace.NORTH
            3 -> BlockFace.SOUTH
            4 -> BlockFace.WEST
            5 -> BlockFace.EAST
            else -> BlockFace.UP
        }
    }

    class Data(var index: Int, var desc: Boolean)
}