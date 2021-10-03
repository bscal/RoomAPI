package me.bscal.roomapi

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.util.logging.Level

private const val Distance = 16 * 16

internal val NORTH = Vector(1, 0, 0)
internal val SOUTH = Vector(-1, 0, 0)
internal val UP = Vector(0, 1, 0)
internal val DOWN = Vector(0, -1, 0)
internal val EAST = Vector(0, 0, 1)
internal val WEST = Vector(0, 0, -1)

@JvmRecord data class FloodFillReturnData(val isRoom: Boolean, val blocksProcessed: Int, val blocks: ObjectArrayList<Location>)

@Deprecated("Use CreateRoom() instead.")
internal fun TestWallConnects(player: Player)
{
	val scope = CoroutineScope(Dispatchers.Default)
	val job = scope.launch {
		val scopedLocation = player.location.clone()
		if (DoesLocationExist(scopedLocation))
		{
			RoomApiPlugin.Log(Level.WARNING, "This room seems to already exist!")
			return@launch
		}
		val started = System.nanoTime()
		val fillReturnData: FloodFillReturnData = FloodFill(scopedLocation)
		if (fillReturnData.isRoom)
		{
			val roomId = InsertRoom(scopedLocation.world, player.uniqueId)
			InsertBlockBatch(roomId, fillReturnData.blocks)
			RoomApiPlugin.Log(Level.INFO, "RoomId=$roomId")
		}

		val diff = System.nanoTime() - started
		RoomApiPlugin.Log(Level.INFO,
			"FloodFill took: ${diff}ns (${diff / 1000000}ms), Processed ${fillReturnData.blocksProcessed} blocks. Is Room: ${fillReturnData.isRoom}")
	}
}

private fun AddVecsToNew(src: Vector, add: Vector): Vector = Vector(src.x + add.x, src.y + add.y, src.z + add.z)

fun FloodFill(location: Location): FloodFillReturnData
{
	val origin: Vector = location.toVector()
	val visited: ObjectOpenHashSet<Vector> = ObjectOpenHashSet()
	val stack: ObjectArrayList<Vector> = ObjectArrayList()
	val roomBlocks: ObjectArrayList<Location> = ObjectArrayList()
	var isRoom = true
	var counter = 0

	if (!visited.contains(origin)) stack.push(origin)
	while (!stack.isEmpty)
	{
		val vec = stack.pop()
		location.set(vec.x, vec.y, vec.z)
		visited.add(vec)
		counter++
		if (!location.isWorldLoaded || !location.isChunkLoaded)
		{
			System.err.println("Location's world: ${location.world} or chunk: ${location.chunk} is not loaded. Canceling FloodFill")
			return FloodFillReturnData(false, counter, ObjectArrayList<Location>(1))
		}
		else if (origin.distanceSquared(vec) > Distance)
		{
			isRoom = false
			continue
		}
		else if (location.block.type == Material.AIR) // Do logic here
			roomBlocks.add(location.clone())
		else continue

		val v0 = AddVecsToNew(vec, NORTH)
		if (!visited.contains(v0)) stack.push(v0)

		val v1 = AddVecsToNew(vec, SOUTH)
		if (!visited.contains(v1)) stack.push(v1)

		val v2 = AddVecsToNew(vec, EAST)
		if (!visited.contains(v2)) stack.push(v2)

		val v3 = AddVecsToNew(vec, WEST)
		if (!visited.contains(v3)) stack.push(v3)

		val v4 = AddVecsToNew(vec, UP)
		if (!visited.contains(v4)) stack.push(v4)

		val v5 = AddVecsToNew(vec, DOWN)
		if (!visited.contains(v5)) stack.push(v5)
	}
	return FloodFillReturnData(isRoom, counter, roomBlocks)
}

// My first implementation of floodfill I tried to optimize with the newer version.
// I did some basic tests and found that it was consistently faster by ~10-30% faster
//private fun Fill(location: Location)
//{
//	val origin = location.clone()
//	var isRoom = true
//
//	if (ShouldProcess(location)) Stack.push(location)
//	while (!Stack.isEmpty)
//	{
//		val stackLoc = Stack.pop()
//		val x = stackLoc.x
//		val y = stackLoc.y
//		val z = stackLoc.z
//		Visited.add(stackLoc)
//		Counter++
//
//		if (origin.distanceSquared(stackLoc) > Distance)
//		{
//			isRoom = false
//			continue
//		}
//		else if (stackLoc.block.type == Material.AIR) // Do logic here
//			RoomBlocks.add(stackLoc)
//		else continue
//
//		// Checks surrounding blocks and clones when needed
//		// I use this method to avoid a lot of unnecessary cloning
//		var loc: Location = stackLoc.set(x + 1, y, z)
//		if (ShouldProcess(loc)) Stack.push(loc.clone())
//
//		loc = stackLoc.set(x - 1, y, z)
//		if (ShouldProcess(loc)) Stack.push(loc.clone())
//
//		loc = stackLoc.set(x, y + 1, z)
//		if (ShouldProcess(loc)) Stack.push(loc.clone())
//
//		loc = stackLoc.set(x, y - 1, z)
//		if (ShouldProcess(loc)) Stack.push(loc.clone())
//
//		loc = stackLoc.set(x, y, z + 1)
//		if (ShouldProcess(loc)) Stack.push(loc.clone())
//
//		loc = stackLoc.set(x, y, z - 1)
//		if (ShouldProcess(loc)) Stack.push(loc.clone())
//
//		// returns stackLoc to original cords
//		stackLoc.set(x, y, z)
//	}
//
//	if (isRoom)
//	{
//		Bukkit.getLogger().info("This is a room!")
//	}
//
//	Visited.clear()
//	Visited.trim()
//	Stack.clear()
//	Stack.trim()
//	RoomBlocks.clear()
//	RoomBlocks.trim()
//}