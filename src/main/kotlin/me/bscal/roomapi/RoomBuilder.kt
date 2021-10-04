package me.bscal.roomapi

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.util.*
import java.util.logging.Level

internal val NORTH = Vector(1, 0, 0)
internal val SOUTH = Vector(-1, 0, 0)
internal val UP = Vector(0, 1, 0)
internal val DOWN = Vector(0, -1, 0)
internal val EAST = Vector(0, 0, 1)
internal val WEST = Vector(0, 0, -1)

@JvmRecord data class FloodFillReturnData(val isRoom: Boolean, val blocksProcessed: Int, val blocks: ObjectArrayList<Vector>)

@Deprecated("Use CreateRoom() instead.")
internal fun TestWallConnects(player: Player)
{
	val scope = CoroutineScope(Dispatchers.Default)
	val job = scope.launch {
		val scopedLocation = player.location.clone()
		if (DoesLocationExist(scopedLocation))
		{
			RoomApiPlugin.LogDebug(Level.WARNING, "This room seems to already exist!")
			return@launch
		}
		val started = System.nanoTime()
		val fillReturnData: FloodFillReturnData = FloodFill(scopedLocation)
		if (fillReturnData.isRoom)
		{
			val roomId = InsertRoom(scopedLocation.world, player.uniqueId)
			InsertBlockBatch(roomId, scopedLocation.world.name, fillReturnData.blocks)
			RoomApiPlugin.LogDebug(Level.INFO, "RoomId=$roomId")
		}

		val diff = System.nanoTime() - started
		RoomApiPlugin.LogDebug(Level.INFO,
			"FloodFill took: ${diff}ns (${diff / 1000000}ms), Processed ${fillReturnData.blocksProcessed} blocks. Is Room: ${fillReturnData.isRoom}")
	}
}

private fun AddVecsToNew(src: Vector, add: Vector): Vector = Vector(src.x + add.x, src.y + add.y, src.z + add.z)

fun FindBlocks(worldName: String, origin: Vector): ObjectArrayList<Vector>
{
	val world = Bukkit.getWorld(worldName) ?: return ObjectArrayList(0)
	val location = Location(world, origin.x, origin.y, origin.z)
	val blocks = ObjectArrayList<Vector>(0)
	val visited: ObjectOpenHashSet<Vector> = ObjectOpenHashSet()
	val stack: ObjectArrayList<Vector> = ObjectArrayList()
	stack.push(origin)
	while (!stack.isEmpty)
	{
		val vec = stack.pop()
		location.set(vec.x, vec.y, vec.z)
		visited.add(vec)
		if (!location.isWorldLoaded || !location.isChunkLoaded)
		{
			System.err.println("Location's world: ${location.world} or chunk: ${location.chunk} is not loaded. Canceling FloodFill")
			return ObjectArrayList(0)
		}
		if (origin.distanceSquared(vec) > RoomApiPlugin.MAX_SEARCH_DISTANCE) return ObjectArrayList(0)

		blocks.add(vec)

		val v0 = AddVecsToNew(vec, NORTH)
		if (!visited.contains(v0) && RoomBlocks.IsExcludedOrAir(
				location.world.getBlockAt(v0.blockX, v0.blockY, v0.blockZ).type)
		) stack.push(v0)

		val v1 = AddVecsToNew(vec, SOUTH)
		if (!visited.contains(v1) && RoomBlocks.IsExcludedOrAir(
				location.world.getBlockAt(v1.blockX, v1.blockY, v1.blockZ).type)
		) stack.push(v1)

		val v2 = AddVecsToNew(vec, EAST)
		if (!visited.contains(v2) && RoomBlocks.IsExcludedOrAir(
				location.world.getBlockAt(v2.blockX, v2.blockY, v2.blockZ).type)
		) stack.push(v2)

		val v3 = AddVecsToNew(vec, WEST)
		if (!visited.contains(v3) && RoomBlocks.IsExcludedOrAir(
				location.world.getBlockAt(v3.blockX, v3.blockY, v3.blockZ).type)
		) stack.push(v3)

		val v4 = AddVecsToNew(vec, UP)
		if (!visited.contains(v4) && RoomBlocks.IsExcludedOrAir(
				location.world.getBlockAt(v4.blockX, v4.blockY, v4.blockZ).type)
		) stack.push(v4)

		val v5 = AddVecsToNew(vec, DOWN)
		if (!visited.contains(v5) && RoomBlocks.IsExcludedOrAir(
				location.world.getBlockAt(v5.blockX, v5.blockY, v5.blockZ).type)
		) stack.push(v5)
	}
	return blocks
}

fun TestRoom(worldName: String, origin: Vector): Boolean
{
	val world = Bukkit.getWorld(worldName) ?: return false
	val location = Location(world, origin.x, origin.y, origin.z)
	val visited: ObjectOpenHashSet<Vector> = ObjectOpenHashSet()
	val stack: ObjectArrayList<Vector> = ObjectArrayList()
	stack.push(origin)
	while (!stack.isEmpty)
	{
		val vec = stack.pop()
		location.set(vec.x, vec.y, vec.z)
		visited.add(vec)
		if (!location.isWorldLoaded || !location.isChunkLoaded)
		{
			System.err.println("Location's world: ${location.world} or chunk: ${location.chunk} is not loaded. Canceling FloodFill")
			return false
		}
		if (origin.distanceSquared(vec) > RoomApiPlugin.MAX_SEARCH_DISTANCE) return false

		val v0 = AddVecsToNew(vec, NORTH)
		if (!visited.contains(v0) && RoomBlocks.IsExcludedOrAir(
				location.world.getBlockAt(v0.blockX, v0.blockY, v0.blockZ).type)
		) stack.push(v0)

		val v1 = AddVecsToNew(vec, SOUTH)
		if (!visited.contains(v1) && RoomBlocks.IsExcludedOrAir(
				location.world.getBlockAt(v1.blockX, v1.blockY, v1.blockZ).type)
		) stack.push(v1)

		val v2 = AddVecsToNew(vec, EAST)
		if (!visited.contains(v2) && RoomBlocks.IsExcludedOrAir(
				location.world.getBlockAt(v2.blockX, v2.blockY, v2.blockZ).type)
		) stack.push(v2)

		val v3 = AddVecsToNew(vec, WEST)
		if (!visited.contains(v3) && RoomBlocks.IsExcludedOrAir(
				location.world.getBlockAt(v3.blockX, v3.blockY, v3.blockZ).type)
		) stack.push(v3)

		val v4 = AddVecsToNew(vec, UP)
		if (!visited.contains(v4) && RoomBlocks.IsExcludedOrAir(
				location.world.getBlockAt(v4.blockX, v4.blockY, v4.blockZ).type)
		) stack.push(v4)

		val v5 = AddVecsToNew(vec, DOWN)
		if (!visited.contains(v5) && RoomBlocks.IsExcludedOrAir(
				location.world.getBlockAt(v5.blockX, v5.blockY, v5.blockZ).type)
		) stack.push(v5)
	}
	return true
}

fun FloodFill(location: Location): FloodFillReturnData
{
	val origin: Vector = location.toVector()
	val visited: ObjectOpenHashSet<Vector> = ObjectOpenHashSet()
	val stack: ObjectArrayList<Vector> = ObjectArrayList()
	val roomBlocks: ObjectArrayList<Vector> = ObjectArrayList()
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
			return FloodFillReturnData(false, counter, ObjectArrayList<Vector>(1))
		}
		else if (origin.distanceSquared(vec) > RoomApiPlugin.MAX_SEARCH_DISTANCE)
		{
			isRoom = false
			continue
		}
		else if (RoomBlocks.IsExcludedOrAir(location.block.type)) // Do logic here
			roomBlocks.add(location.toVector())
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