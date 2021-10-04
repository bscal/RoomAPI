package me.bscal.roomapi

import com.destroystokyo.paper.event.block.BlockDestroyEvent
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.axay.kspigot.runnables.async
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.util.Vector
import java.util.logging.Level

internal class RoomListeners : Listener
{
	@EventHandler(priority = EventPriority.MONITOR)
	fun OnBlockPlace(event: BlockPlaceEvent)
	{
		if (!RoomBlocks.IsExcluded(event.block.type))
		{
			async {
				val loc = event.block.location
				RemoveBlock(loc.world.name, loc.toVector())

				val locArray = arrayOf(loc.clone().add(UP), loc.clone().add(DOWN), loc.clone().add(NORTH), loc.clone().add(SOUTH),
					loc.clone().add(EAST), loc.clone().add(WEST))

				val valid = ObjectArrayList<Vector>()
				var roomId: Int = -1
				for (location in locArray)
				{
					if (RoomBlocks.IsExcludedOrAir(location.block.type))
					{
						valid.add(location.toVector())
						if (roomId == -1) roomId = FetchRoomId(location)
					}
				}

				// No rooms to check
				RoomApiPlugin.LogDebug(Level.INFO, "No rooms")
				if (valid.isEmpty || roomId == -1) return@async

				// All valid blocks around are connected still
				RoomApiPlugin.LogDebug(Level.INFO, "All blocks connect in same room")
				val blocks = FindBlocks(loc.world.name, valid[0])
				if (blocks.containsAll(valid)) return@async

				// Room restructure
				RoomApiPlugin.LogDebug(Level.INFO, "Remaking rooms")
				val room = FetchRoom(roomId) ?: return@async
				val newRoomId = InsertRoom(loc.world, room.Owner)
				UpdateBlocks(newRoomId, loc.world.name, blocks.toTypedArray())
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	fun OnBlockDestroy(event: BlockDestroyEvent)
	{
		CheckForBrokenRoom(event.eventName, event.block.location)
	}

	@EventHandler(priority = EventPriority.MONITOR)
	fun OnEntityChangeBlock(event: EntityChangeBlockEvent)
	{
		if (event.to.isAir) CheckForBrokenRoom(event.eventName, event.block.location)
	}

	@EventHandler(priority = EventPriority.MONITOR)
	fun OnBlockBreak(event: BlockBreakEvent)
	{
		CheckForBrokenRoom(event.eventName, event.block.location)
	}

	@EventHandler(priority = EventPriority.MONITOR)
	fun OnBlockFade(event: BlockFadeEvent)
	{
		CheckForBrokenRoom(event.eventName, event.block.location)
	}

	@EventHandler(priority = EventPriority.MONITOR)
	fun OnBlockExplode(event: BlockExplodeEvent)
	{
		event.blockList().forEach {
			CheckForBrokenRoom(event.eventName, it.location)
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	fun OnEntityExplode(event: EntityExplodeEvent)
	{
		event.blockList().forEach {
			CheckForBrokenRoom(event.eventName, it.location)
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	fun OnLeafDecay(event: LeavesDecayEvent)
	{
		CheckForBrokenRoom(event.eventName, event.block.location)
	}

	private fun CheckForBrokenRoom(eventName: String, location: Location)
	{
		Bukkit.getScheduler().runTaskAsynchronously(RoomApiPlugin.INSTANCE, Runnable {
			RoomApiPlugin.LogDebug(Level.INFO, "Checking block $location from $eventName")

			if (!location.isWorldLoaded && !location.isChunkLoaded && RoomBlocks.IsExcludedOrAir(location.block.type)) return@Runnable

			val start = System.nanoTime()
			val worldName = location.world.name
			val vec = location.toVector()
			val rooms = IntArrayList()
			val vecArray = arrayOf(vec.clone().add(UP), vec.clone().add(DOWN), vec.clone().add(NORTH), vec.clone().add(SOUTH),
				vec.clone().add(EAST), vec.clone().add(WEST))
			for (v in vecArray)
			{
				val roomId = FetchRoomId(v.toLocation(location.world))
				if (roomId > -1 && rooms.indexOf(roomId) == -1) rooms.add(roomId)
			}
			RoomApiPlugin.LogDebug(Level.INFO, "rooms $rooms")
			if (!rooms.isEmpty)
			{
				if (TestRoom(worldName, vec))
				{
					val mainRoomId = rooms.getInt(0)
					InsertBlock(mainRoomId, worldName, vec)
					for (i in 1 until rooms.size)
					{
						val blocks = FetchBlocks(rooms.getInt(i))
						UpdateBlocks(mainRoomId, worldName, blocks.toTypedArray())
						RemoveRoom(rooms.getInt(i))
					}
				}
				else
				{
					for (roomId in rooms.iterator()) RemoveRoom(roomId)
				}
			}

			val end = System.nanoTime() - start
			RoomApiPlugin.LogDebug(Level.INFO, "Took: ${end}ns (${end / 1000000}ms)")
		})
	}
}