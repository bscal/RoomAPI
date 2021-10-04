package me.bscal.roomapi

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.util.Vector
import java.util.*

class Room(val RoomId: Int, val WorldName: String, val Owner: UUID)
{
	constructor(roomId: Int, worldName: String, owner: UUID, blockLocations: ObjectArrayList<Vector>) : this(roomId, worldName, owner)
	{
		this.BlockLocations = blockLocations
	}

	lateinit var BlockLocations: ObjectArrayList<Vector>

	fun AsLocation(vector: Vector): Location = Location(Bukkit.getWorld(WorldName), vector.x, vector.y, vector.z)

	fun GetBlock(vector: Vector): Block? = Bukkit.getWorld(WorldName)?.getBlockAt(vector.blockX, vector.blockY, vector.blockZ)
}