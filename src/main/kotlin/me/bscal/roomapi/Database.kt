package me.bscal.roomapi

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.util.Vector
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Statement
import java.util.*
import java.util.logging.Level

private val Config: HikariConfig = HikariConfig()

internal val DataSource: HikariDataSource by lazy {
	Class.forName("org.h2.Driver")
	Config.jdbcUrl = "jdbc:h2:${RoomApiPlugin.INSTANCE.dataFolder.absolutePath}\\roomapi"
	Config.maximumPoolSize = 10
	Config.connectionTimeout = 60000
	Config.leakDetectionThreshold = 60000
	HikariDataSource(Config)
}

fun CreateTables()
{    // Create rooms
	val sql = """
		CREATE TABLE IF NOT EXISTS rooms (
		room_id              integer   AUTO_INCREMENT,
		world                varchar(64)   NOT NULL,
		owner                uuid(32)   NOT NULL,
		CONSTRAINT pk_rooms_room_id PRIMARY KEY ( room_id ));
	""".trimIndent()
	var conn: Connection? = null
	var stmt: Statement? = null
	try
	{
		conn = DataSource.connection
		stmt = conn.createStatement()
		stmt.execute(sql)
		stmt.close()
		conn.close()
	}
	catch (e: SQLException)
	{
		System.err.println(e.stackTrace)
	}
	stmt?.close()
	conn?.close()

	// Create blocks
	val sql2 = """
		SET FOREIGN_KEY_CHECKS = 0;
		CREATE TABLE IF NOT EXISTS blocks ( 
			id                   bigint   AUTO_INCREMENT,
			room_id              integer   NOT NULL,
			world                varchar(64)   NOT NULL,
			x                    integer   NOT NULL,
			y                    integer   NOT NULL,
			z                    integer   NOT NULL,
			CONSTRAINT pk_blocks_id PRIMARY KEY ( id ));
		SET FOREIGN_KEY_CHECKS = 1;
	""".trimIndent()
	var conn2: Connection? = null
	var stmt2: Statement? = null
	try
	{
		conn2 = DataSource.connection
		stmt2 = conn2.createStatement()
		stmt2.execute(sql2)
		stmt2.close()
		conn2.close()
	}
	catch (e: SQLException)
	{
		System.err.println(e.stackTrace)
	}
	stmt2?.close()
	conn2?.close()
}

fun InsertRoom(world: World, owner: UUID): Int
{
	var id = -1
	val sql = "INSERT INTO rooms (world, owner) values (?,?)"
	var conn: Connection? = null
	var stmt: PreparedStatement? = null
	try
	{
		conn = DataSource.connection
		stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
		stmt.setString(1, world.name)
		stmt.setString(2, owner.toString())
		stmt.execute()
		val rs = stmt.generatedKeys
		if (rs.next()) id = rs.getInt(1)
	}
	catch (e: SQLException)
	{
		System.err.println(e.stackTrace)
	}
	stmt?.close()
	conn?.close()
	RoomApiPlugin.LogDebug(Level.INFO, "Inserting room $id")
	return id
}

fun FetchRoom(roomId: Int): Room?
{
	var room: Room? = null
	val sql = "SELECT * FROM rooms WHERE room_id=?"
	var conn: Connection? = null
	var stmt: PreparedStatement? = null
	try
	{
		conn = DataSource.connection
		stmt = conn.prepareStatement(sql)
		stmt.setInt(1, roomId)
		val rs = stmt.executeQuery()
		if (rs.next())
		{
			val worldStr = rs.getString("world")
			val uuidStr = rs.getString("owner")
			if (worldStr.isNullOrBlank() || uuidStr.isNullOrBlank()) return null
			if (!RoomApiPlugin.INSTANCE.isEnabled)
			{
				System.err.println("Plugin is not enabled")
				return null
			}
			room = Room(roomId, worldStr, UUID.fromString(uuidStr), ObjectArrayList<Vector>())
			stmt.close()

			val sqlForBlocks = "SELECT x, y, z FROM blocks WHERE room_id=?"
			stmt = conn.prepareStatement(sqlForBlocks)
			stmt.setInt(1, roomId)
			val rsForBlocks = stmt.executeQuery()
			while (rsForBlocks.next())
			{
				room.BlockLocations.add(Vector(rs.getInt(1), rs.getInt(2), rs.getInt(3)))
			}
		}
	}
	catch (e: Exception)
	{
		System.err.println(e.stackTrace)
	}
	finally
	{
		stmt?.close()
		conn?.close()
	}
	RoomApiPlugin.LogDebug(Level.INFO, "Inserting room $room")
	return room
}

fun FetchBlocks(roomId: Int): ObjectArrayList<Vector>
{
	val blocks = ObjectArrayList<Vector>()
	val sql = "SELECT x, y, z FROM blocks WHERE room_id=?"
	var conn: Connection? = null
	var stmt: PreparedStatement? = null
	try
	{
		conn = DataSource.connection
		stmt = conn.prepareStatement(sql)
		stmt.setInt(1, roomId)
		val rs = stmt.executeQuery()
		while (rs.next()) blocks.add(Vector(rs.getInt(1), rs.getInt(2), rs.getInt(3)))
	}
	catch (e: Exception)
	{
		System.err.println(e.stackTrace)
	}
	finally
	{
		stmt?.close()
		conn?.close()
	}
	RoomApiPlugin.LogDebug(Level.INFO, "FetchBlocks $roomId")
	return blocks
}

fun InsertBlock(roomId: Int, worldName: String, vector: Vector)
{
	val sql = "INSERT INTO blocks (world, x, y, z, room_id) values (?, ?, ?, ?, ?)"
	var conn: Connection? = null
	var stmt: PreparedStatement? = null
	try
	{
		conn = DataSource.connection
		stmt = conn.prepareStatement(sql)
		stmt.setString(1, worldName)
		stmt.setInt(2, vector.blockX)
		stmt.setInt(3, vector.blockY)
		stmt.setInt(4, vector.blockZ)
		stmt.setInt(5, roomId)
		stmt.executeUpdate()
	}
	catch (e: SQLException)
	{
		System.err.println(e.stackTrace)
	}
	stmt?.close()
	conn?.close()
	RoomApiPlugin.LogDebug(Level.INFO, "InsertBlock $roomId, $vector")
}

fun InsertBlockBatch(roomId: Int, worldName: String, locations: ObjectArrayList<Vector>)
{
	val sql = "INSERT INTO blocks (world, x, y, z, room_id) values (?, ?, ?, ?, ?)"
	var conn: Connection? = null
	var stmt: PreparedStatement? = null
	try
	{
		conn = DataSource.connection
		stmt = conn.prepareStatement(sql)
		for (vec: Vector in locations)
		{
			stmt.setString(1, worldName)
			stmt.setInt(2, vec.blockX)
			stmt.setInt(3, vec.blockY)
			stmt.setInt(4, vec.blockZ)
			stmt.setInt(5, roomId)
			stmt.addBatch()
		}
		stmt.executeBatch()
	}
	catch (e: SQLException)
	{
		System.err.println(e.stackTrace)
	}
	stmt?.close()
	conn?.close()
	RoomApiPlugin.LogDebug(Level.INFO, "Inserting block batch for $roomId")
}

fun DoesLocationExist(locations: Location): Boolean
{
	var exists = false
	val sql = "SELECT room_id FROM blocks WHERE world = ? AND x = ? AND y = ? AND z = ?;"

	var conn: Connection? = null
	var stmt: PreparedStatement? = null
	try
	{
		conn = DataSource.connection
		stmt = conn.prepareStatement(sql)
		stmt.setString(1, locations.world.name)
		stmt.setInt(2, locations.blockX)
		stmt.setInt(3, locations.blockY)
		stmt.setInt(4, locations.blockZ)
		val rs = stmt.executeQuery()
		if (rs.next()) exists = true
	}
	catch (e: SQLException)
	{
		System.err.println(e.stackTrace)
	}
	stmt?.close()
	conn?.close()

	return exists
}

fun DoVectorsExist(world: World, positions: Array<Vector>): IntArrayList
{
	val exists = IntArrayList()
	val sql = "SELECT room_id FROM blocks WHERE world = ? AND x = ? AND y = ? AND z = ?;"
	var conn: Connection? = null
	var stmt: PreparedStatement? = null
	try
	{
		conn = DataSource.connection
		stmt = conn.prepareStatement(sql)
		for (v in positions)
		{
			stmt.setString(1, world.name)
			stmt.setInt(2, v.x.toInt())
			stmt.setInt(3, v.y.toInt())
			stmt.setInt(4, v.z.toInt())
			val rs = stmt.executeQuery()
			if (rs.next())
			{
				val roomId = rs.getInt(1)
				if (roomId > 0 && !exists.contains(roomId)) exists.add(roomId)
			}
		}
	}
	catch (e: SQLException)
	{
		System.err.println(e.stackTrace)
	}
	stmt?.close()
	conn?.close()

	return exists
}

fun FetchRoomId(location: Location): Int
{
	var roomId = -1
	val sql = "SELECT room_id FROM blocks WHERE world = ? AND x = ? AND y = ? AND z = ?;"
	var conn: Connection? = null
	var stmt: PreparedStatement? = null
	try
	{
		conn = DataSource.connection
		stmt = conn.prepareStatement(sql)
		stmt.setString(1, location.world.name)
		stmt.setInt(2, location.blockX)
		stmt.setInt(3, location.blockY)
		stmt.setInt(4, location.blockZ)
		val rs = stmt.executeQuery()
		if (rs.next()) roomId = rs.getInt(1)
	}
	catch (e: SQLException)
	{
		System.err.println(e.stackTrace)
	}
	stmt?.close()
	conn?.close()
	RoomApiPlugin.LogDebug(Level.INFO, "FetchRoomId loc: $location")
	return roomId
}

fun RemoveRoom(roomId: Int)
{
	val sql = "DELETE FROM blocks WHERE room_id=?; DELETE FROM rooms WHERE room_id=?"
	var conn: Connection? = null
	var stmt: PreparedStatement? = null
	try
	{
		conn = DataSource.connection
		stmt = conn.prepareStatement(sql)
		stmt.setInt(1, roomId)
		stmt.setInt(2, roomId)
		stmt.executeUpdate()
	}
	catch (e: SQLException)
	{
		System.err.println(e.stackTrace)
	}
	stmt?.close()
	conn?.close()

	RoomApiPlugin.LogDebug(Level.INFO, "Removing room $roomId")
}

fun RemoveBlock(worldName: String, vector: Vector)
{
	val sql = "DELETE FROM blocks WHERE world = ? AND x = ? AND y = ? AND z = ?"
	var conn: Connection? = null
	var stmt: PreparedStatement? = null
	try
	{
		conn = DataSource.connection
		stmt = conn.prepareStatement(sql)
		stmt.setString(1, worldName)
		stmt.setInt(2, vector.blockX)
		stmt.setInt(3, vector.blockY)
		stmt.setInt(4, vector.blockZ)
		stmt.executeUpdate()
	}
	catch (e: SQLException)
	{
		System.err.println(e.stackTrace)
	}
	stmt?.close()
	conn?.close()
	RoomApiPlugin.LogDebug(Level.INFO, "Removing block $vector")
}

fun UpdateBlocks(newRoomId: Int, worldName: String, positions: Array<Vector>)
{
	val sql = "UPDATE blocks SET room_id = ? WHERE world = ? AND x = ? AND y = ? AND z = ?"
	var conn: Connection? = null
	var stmt: PreparedStatement? = null
	try
	{
		conn = DataSource.connection
		stmt = conn.prepareStatement(sql)
		for (v in positions)
		{
			stmt.setInt(1, newRoomId)
			stmt.setString(2, worldName)
			stmt.setInt(3, v.blockX)
			stmt.setInt(4, v.blockY)
			stmt.setInt(5, v.blockZ)
			stmt.addBatch()
		}
		stmt.executeBatch()
	}
	catch (e: SQLException)
	{
		System.err.println(e.stackTrace)
	}
	stmt?.close()
	conn?.close()
	RoomApiPlugin.LogDebug(Level.INFO, "Updating to room $newRoomId")
}