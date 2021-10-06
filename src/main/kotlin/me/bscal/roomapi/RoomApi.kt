package me.bscal.roomapi

import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.axay.kspigot.runnables.async
import net.axay.kspigot.runnables.sync
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.util.Consumer
import org.bukkit.util.Vector
import java.util.*
import java.util.logging.Level

object RoomApi
{
	fun CreateRoom(player: Player) = CreateRoom(player, null)

	fun CreateRoom(player: Player, cb: Consumer<Room>?)
	{
		val owner: UUID = player.uniqueId
		val locationCopy = player.location.clone()
		val worldName: String = locationCopy.world.name
		async task@{
			if (DoesLocationExist(locationCopy))
			{
				RoomApiPlugin.LogDebug(Level.WARNING, "This room seems to already exist!")
				return@task
			}
			val started = System.nanoTime()
			val fillReturnData: FloodFillReturnData = FloodFill(locationCopy)
			if (fillReturnData.isRoom)
			{
				val roomId = InsertRoom(worldName, owner)
				InsertBlockBatch(roomId, locationCopy.world.name, fillReturnData.blocks)
				RoomApiPlugin.LogDebug(Level.INFO, "Room($roomId) created with ${fillReturnData.blocks.size} blocks!")
				if (cb != null)
				{
					sync {                // I'm pretty sure this is safe? the blocks list is not modified again
						cb.accept(Room(roomId, locationCopy.world.name, owner, fillReturnData.blocks))
					}
				}
			}
			val diff = System.nanoTime() - started
			RoomApiPlugin.LogDebug(Level.INFO,
				"FloodFill took: ${diff}ns (${diff / 1000000}ms), Processed ${fillReturnData.blocksProcessed} blocks. Is Room: ${fillReturnData.isRoom}")
		}
	}

	fun CreateRoom(player: Player, world: World, blocks: ObjectArrayList<Vector>)
	{
		async {
			val roomId = InsertRoom(world.name, player.uniqueId)
			InsertBlockBatch(roomId, world.name, blocks)
		}

	}

	fun IsRoom(location: Location, cb: Consumer<ObjectArrayList<Vector>>?)
	{
		val locationCopy = location.clone()
		async task@{
			if (DoesLocationExist(locationCopy))
			{
				RoomApiPlugin.LogDebug(Level.WARNING, "This room seems to already exist!")
				return@task
			}
			val fillReturnData: FloodFillReturnData = FloodFill(locationCopy)
			sync {
				cb?.accept(fillReturnData.blocks)
			}
		}
	}

	fun GetRoomsId(location: Location, cb: Consumer<Int>)
	{
		async {
			val roomId = FetchRoomId(location)
			if (roomId != -1)
			{
				sync {
					cb.accept(roomId)
				}
			}

		}
	}

	fun GetRoomFromId(roomId: Int, cb: Consumer<Room>)
	{
		async {

			val room = FetchRoom(roomId)
			if (room != null)
			{
				sync {
					cb.accept(room)
				}
			}
		}
	}

	fun GetRoomFromLocation(location: Location, cb: Consumer<Room>)
	{
		async {
			val roomId = FetchRoomId(location)
			if (roomId != -1)
			{
				val room = FetchRoom(roomId)
				if (room != null)
				{
					sync {
						cb.accept(room)
					}
				}
			}
		}
	}

	fun DeleteLocationIfExists(location: Location, cb: Consumer<Room>?)
	{
		async {
			val roomId = FetchRoomId(location)
			if (roomId != -1)
			{
				val room = FetchRoom(roomId)
				if (room != null)
				{
					sync {
						cb?.accept(room)
					}
					DeleteRoom(roomId)
				}
			}
		}
	}

	fun DeleteRoom(roomId: Int)
	{
		async {
			DeleteRoom(roomId)
		}
	}

	fun UpdateRoomOnBreak(location: Location)
	{
		if (!location.isWorldLoaded || !location.isChunkLoaded)
			return

		async {
			val start = System.nanoTime()
			val worldName = location.world.name
			val vec = location.toVector()
			val rooms = IntArrayList()
			val surroundingBlocks = arrayOf(vec.clone().add(UP), vec.clone().add(DOWN), vec.clone().add(NORTH), vec.clone().add(SOUTH),
				vec.clone().add(EAST), vec.clone().add(WEST))
			for (v in surroundingBlocks)
			{
				val type = GetTypeAsync(location.world, v)
				RoomApiPlugin.Log(Level.WARNING, "$v || $type")
				if (RoomBlocks.IsExcludedOrAir(type))
				{
					val roomId = FetchRoomId(v.toLocation(location.world))
					if (roomId > -1 && rooms.indexOf(roomId) == -1) rooms.add(roomId)
				}
			}

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
		}
	}

	fun UpdateRoomOnPlace(location: Location)
	{
		if (!location.isWorldLoaded || !location.isChunkLoaded)
			return
		async {
			val worldName = location.world.name
			val origin = location.toVector()
			RemoveBlock(worldName, origin)
			val surroundingBlocks = arrayOf(origin.clone().add(UP), origin.clone().add(DOWN), origin.clone().add(NORTH),
				origin.clone().add(SOUTH), origin.clone().add(EAST), origin.clone().add(WEST))

			val valid = ObjectArrayList<Vector>()
			var roomId: Int = -1
			for (v in surroundingBlocks)
			{
				val type = GetTypeAsync(location.world, v)
				if (RoomBlocks.IsExcludedOrAir(type))
				{
					valid.add(v)
					if (roomId == -1) roomId = FetchRoomId(worldName, v)
				}
			}

			// No rooms to check
			RoomApiPlugin.LogDebug(Level.INFO, "No rooms")
			if (valid.isEmpty || roomId == -1) return@async

			// All valid blocks around are connected still
			RoomApiPlugin.LogDebug(Level.INFO, "All blocks connect in same room")
			val blocks = FindBlocks(worldName, valid[0])
			if (blocks.containsAll(valid)) return@async

			// Room restructure
			RoomApiPlugin.LogDebug(Level.INFO, "Remaking rooms")
			val room = FetchRoom(roomId) ?: return@async
			val newRoomId = InsertRoom(worldName, room.Owner)
			UpdateBlocks(newRoomId, worldName, blocks.toTypedArray())
		}
	}
}