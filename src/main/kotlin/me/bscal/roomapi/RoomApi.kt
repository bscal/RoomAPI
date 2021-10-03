package me.bscal.roomapi

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.axay.kspigot.runnables.async
import net.axay.kspigot.runnables.sync
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.util.Consumer
import java.util.UUID
import java.util.logging.Level

class RoomApi
{
	companion object
	{
		fun CreateRoom(player: Player) = CreateRoom(player, null)

		fun CreateRoom(player: Player, cb: Consumer<Room>?)
		{
			val owner: UUID = player.uniqueId
			val locationCopy = player.location.clone()
			async task@{
				if (DoesLocationExist(locationCopy))
				{
					RoomApiPlugin.Log(Level.WARNING, "This room seems to already exist!")
					return@task
				}
				val started = System.nanoTime()
				val fillReturnData: FloodFillReturnData = FloodFill(locationCopy)
				if (fillReturnData.isRoom)
				{
					val roomId = InsertRoom(locationCopy.world, owner)
					InsertBlockBatch(roomId, fillReturnData.blocks)
					if (RoomApiPlugin.DEBUG) RoomApiPlugin.Log(Level.INFO,
						"Room($roomId) created with ${fillReturnData.blocks.size} blocks!")
					if (cb != null)
					{
						sync {                // I'm pretty sure this is safe? the blocks list is not modified again
							cb.accept(Room(roomId, locationCopy.world, owner, fillReturnData.blocks))
						}
					}
				}
				val diff = System.nanoTime() - started
				if (RoomApiPlugin.DEBUG) RoomApiPlugin.Log(Level.INFO,
					"FloodFill took: ${diff}ns (${diff / 1000000}ms), Processed ${fillReturnData.blocksProcessed} blocks. Is Room: ${fillReturnData.isRoom}")
			}
		}

		fun CreateRoom(player: Player, world: World, blocks: ObjectArrayList<Location>)
		{
			async {
				val roomId = InsertRoom(world, player.uniqueId)
				InsertBlockBatch(roomId, blocks)
			}

		}

		fun IsRoom(location: Location, cb: Consumer<ObjectArrayList<Location>>?)
		{
			val locationCopy = location.clone()
			async task@{
				if (DoesLocationExist(locationCopy))
				{
					RoomApiPlugin.Log(Level.WARNING, "This room seems to already exist!")
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
	}
}