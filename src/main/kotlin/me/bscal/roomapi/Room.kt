package me.bscal.roomapi

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.util.Vector
import java.util.*

class Room(val RoomId: Int, val World: World, val Owner: UUID)
{
	constructor(roomId: Int, world: World, owner: UUID, blockLocations: ObjectArrayList<Location>) : this(roomId, world, owner)
	{
		this.BlockLocations = blockLocations
	}

	lateinit var BlockLocations: ObjectArrayList<Location>

	fun AsLocation(vector: Vector): Location = Location(World, vector.x, vector.y, vector.z)

	fun GetBlock(vector: Vector): Block = World.getBlockAt(vector.blockX, vector.blockY, vector.blockZ)
}