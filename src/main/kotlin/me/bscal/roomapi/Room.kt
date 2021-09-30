package me.bscal.roomapi

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.util.Vector
import java.util.*

class Room(val RoomId: Int, val World: World, val Owner: UUID)
{
	lateinit var BlockPos: ObjectArrayList<Vector>

	fun AsLocation(vector: Vector): Location = Location(World, vector.x, vector.y, vector.z)

	fun GetBlock(vector: Vector): Block = World.getBlockAt(vector.blockX, vector.blockY, vector.blockZ)
}