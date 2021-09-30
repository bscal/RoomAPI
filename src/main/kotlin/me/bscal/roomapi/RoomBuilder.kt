package me.bscal.roomapi

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.util.logging.Level

private const val Distance = 16 * 16

private val NORTH = Vector(1, 0, 0)
private val SOUTH = Vector(-1, 0, 0)
private val UP = Vector(0, 1, 0)
private val DOWN = Vector(0, -1, 0)
private val EAST = Vector(0, 0, 1)
private val WEST = Vector(0, 0, -1)

// Used for debugging the amount of blocks processed
private var Counter: Int = 0
private val Visited: ObjectOpenHashSet<Vector> = ObjectOpenHashSet()
private val Stack: ObjectArrayList<Vector> = ObjectArrayList()
private val RoomBlocks: ObjectArrayList<Location> = ObjectArrayList()

private fun FillVector(location: Location): Boolean
{
	var isRoom = true

	val origin: Vector = location.toVector()
	if (!Visited.contains(origin)) Stack.push(origin)
	while (!Stack.isEmpty)
	{
		val vec = Stack.pop()
		location.set(vec.x, vec.y, vec.z)
		Visited.add(vec)
		Counter++
		if (origin.distanceSquared(vec) > Distance)
		{
			isRoom = false
			continue
		}
		else if (location.block.type == Material.AIR) // Do logic here
			RoomBlocks.add(location.clone())
		else continue

		val v0 = AddVecsToNew(vec, NORTH)
		if (!Visited.contains(v0)) Stack.push(v0)

		val v1 = AddVecsToNew(vec, SOUTH)
		if (!Visited.contains(v1)) Stack.push(v1)

		val v2 = AddVecsToNew(vec, EAST)
		if (!Visited.contains(v2)) Stack.push(v2)

		val v3 = AddVecsToNew(vec, WEST)
		if (!Visited.contains(v3)) Stack.push(v3)

		val v4 = AddVecsToNew(vec, UP)
		if (!Visited.contains(v4)) Stack.push(v4)

		val v5 = AddVecsToNew(vec, DOWN)
		if (!Visited.contains(v5)) Stack.push(v5)
	}

	if (isRoom)
	{
		Bukkit.getLogger().info("This is a room!")
	}

	Visited.clear()
	Visited.trim()
	Stack.clear()
	Stack.trim()

	return isRoom
}

private fun AddVecsToNew(src: Vector, add: Vector): Vector = Vector(src.x + add.x, src.y + add.y, src.z + add.z)

fun TestWallConnects(player: Player)
{
	val scope = CoroutineScope(Dispatchers.Default)
	val job = scope.launch {
		val scopedLocation = player.location
		val started = System.nanoTime()
		Counter = 1
		val isRoom = FillVector(scopedLocation.clone())
		val diff = System.nanoTime() - started
		Bukkit.getLogger().info("FloodFillVec took: ${diff}ns (${diff / 1000000}ms), Processed $Counter blocks")

		RoomAPI.Log(Level.INFO, "Was room? $isRoom")
		if (isRoom)
		{
			val roomId = CreateRoom(scopedLocation.world, player.uniqueId)
			RoomAPI.Log(Level.INFO, "RoomId=$roomId")
		}
	}
	job.invokeOnCompletion {
		RoomBlocks.clear()
		RoomBlocks.trim()
	}

	Bukkit.getLogger().info("end of fun " + job.isCompleted)
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