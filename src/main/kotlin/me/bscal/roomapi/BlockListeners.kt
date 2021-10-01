package me.bscal.roomapi

import com.destroystokyo.paper.event.block.BlockDestroyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockFadeEvent
import org.bukkit.event.block.LeavesDecayEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityExplodeEvent
import java.util.logging.Level

class RoomListeners : Listener
{
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
		if (RoomAPI.DEBUG) RoomAPI.Log(Level.INFO, "Checking block $location from $eventName")

		CoroutineScope(Dispatchers.Default).launch {
			RoomAPI.Log(Level.INFO, "4 I'm working in thread ${Thread.currentThread().name}")
			val world = location.world
			val vec = location.toVector()
			val vecArray = arrayOf(vec, vec.clone().add(UP), vec.clone().add(DOWN), vec.clone().add(NORTH), vec.clone().add(SOUTH),
				vec.clone().add(EAST), vec.clone().add(WEST))

			val indecencies = DoVectorsExist(world, vecArray)
			if (indecencies.isEmpty)
			{
				RoomAPI.Log(Level.INFO, "No indecencies found")
				return@launch
			}

			for (i: Int in indecencies.elements())
			{
				val roomId = LoadBlock(Location(world, vecArray[i].x, vecArray[i].y, vecArray[i].z));
				if (roomId > -1)
				{
					RemoveRoom(roomId)
					RoomAPI.Log(Level.INFO, "Removing room $roomId")
				}
			}
		}
	}
}