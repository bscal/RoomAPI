package me.bscal.roomapi

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent

class BlockListeners : Listener
{

	@EventHandler(priority = EventPriority.MONITOR)
	fun OnBlockBreak(event: BlockBreakEvent)
	{

	}

	@EventHandler(priority = EventPriority.MONITOR)
	fun OnBlockPlace(event: BlockPlaceEvent)
	{

	}

}