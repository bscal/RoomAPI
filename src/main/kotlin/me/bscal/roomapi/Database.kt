package me.bscal.roomapi

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.Location
import org.bukkit.World
import java.sql.PreparedStatement
import java.sql.Statement
import java.util.*

private val Config: HikariConfig = HikariConfig()

val DataSource: HikariDataSource by lazy {
	Class.forName("org.h2.Driver")
	Config.jdbcUrl = "jdbc:h2:${RoomAPI.INSTANCE.dataFolder.absolutePath}\\roomapi"
	HikariDataSource(Config)
}

fun CreateTables()
{
	// Create rooms
	val sql = """
		CREATE TABLE IF NOT EXISTS rooms (
		room_id              integer   NOT NULL AUTO_INCREMENT,
		world                uuid(32)   NOT NULL,
		owner                uuid(32)   NOT NULL,
		CONSTRAINT pk_rooms_room_id PRIMARY KEY ( room_id ));
	""".trimIndent()
	DataSource.connection.createStatement().execute(sql)

	// Create blocks
	val sql2 = """
		CREATE TABLE IF NOT EXISTS blocks ( 
			id                   bigint   NOT NULL,
			room_id              integer   NOT NULL,
			world                uuid(32)   NOT NULL,
			x                    integer   NOT NULL,
			y                    integer   NOT NULL,
			z                    integer   NOT NULL,
			CONSTRAINT pk_blocks_id PRIMARY KEY ( id )
		 );

		ALTER TABLE "PUBLIC".blocks ADD CONSTRAINT fk_blocks_rooms FOREIGN KEY ( room_id ) REFERENCES "PUBLIC".rooms( room_id ) ON DELETE NO ACTION ON UPDATE NO ACTION;
	""".trimIndent()
	DataSource.connection.createStatement().execute(sql2)
}

fun CreateRoom(world: World, owner: UUID) : Int
{
	var id = -1
	val sql = "INSERT INTO rooms(world, owner) values(?,?)"
	val stmt: PreparedStatement = DataSource.connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
	stmt.setString(1, world.name)
	stmt.setString(2, owner.toString())
	stmt.executeUpdate()
	val rs = stmt.generatedKeys
	if (rs.next()) id = rs.getInt(1)
	stmt.close()
	return id
}

fun LoadRoom(room: Room)
{
	val sql = "SELECT * FROM rooms WHERE id=?"
	val stmt: PreparedStatement = DataSource.connection.prepareStatement(sql)
	stmt.setInt(1, room.RoomId)
	val rs = stmt.executeQuery()
	if (rs.next())
	{
	}
	stmt.close()
}

fun CreateBlock(roomId: Int, location: Location)
{
	val sql = "INSERT INTO blocks(world, x, y, z, roomId) values(?,?,?,?,?)"
	val stmt: PreparedStatement = DataSource.connection.prepareStatement(sql)
	stmt.setString(1, location.world.name)
	stmt.setInt(2, location.blockX)
	stmt.setInt(3, location.blockY)
	stmt.setInt(4, location.blockZ)
	stmt.setInt(5, roomId)
	stmt.execute()
	stmt.close()
}

fun LoadBlock(location: Location)
{
	val sql = "SELECT roomId FROM blocks WHERE world=?,x=?,y=?,z=?"
	val stmt: PreparedStatement = DataSource.connection.prepareStatement(sql)
	stmt.setString(1, location.world.name)
	stmt.setInt(2, location.blockX)
	stmt.setInt(3, location.blockY)
	stmt.setInt(4, location.blockZ)
	val rs = stmt.executeQuery()
	if (rs.next())
	{
	}
	stmt.close()
}

fun RemoveRoom(roomId: Int)
{
	val sql = "DELETE FROM blocks WHERE roomId=?; DELETE FROM rooms WHERE roomId=?"
	val stmt: PreparedStatement = DataSource.connection.prepareStatement(sql)
	stmt.setInt(1, roomId)
	stmt.setInt(2, roomId)
	stmt.execute()
	stmt.close()
}