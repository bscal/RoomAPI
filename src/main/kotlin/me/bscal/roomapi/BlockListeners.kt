package me.bscal.roomapi

import com.destroystokyo.paper.event.block.BlockDestroyEvent
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityExplodeEvent
import java.util.logging.Level

internal class RoomListeners : Listener
{
	@EventHandler(priority = EventPriority.MONITOR)
	fun OnBlockPlace(event: BlockPlaceEvent)
	{
		if (!RoomBlocks.IsExcluded(event.blockPlaced.type)) RoomApi.UpdateRoomOnPlace(event.blockPlaced.location)
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
		RoomApiPlugin.LogDebug(Level.INFO, "Checking block $location from $eventName")
		if (!RoomBlocks.IsExcludedOrAir(location.block.type)) RoomApi.UpdateRoomOnBreak(location)
	}
}