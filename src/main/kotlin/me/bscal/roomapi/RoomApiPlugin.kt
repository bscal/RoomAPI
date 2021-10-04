package me.bscal.roomapi

import net.axay.kspigot.commands.command
import net.axay.kspigot.commands.runs
import net.axay.kspigot.extensions.pluginManager
import net.axay.kspigot.main.KSpigot
import java.util.logging.Level
import kotlin.math.sqrt

class RoomApiPlugin : KSpigot()
{
	companion object
	{
		lateinit var INSTANCE: RoomApiPlugin; private set
		var DEBUG: Boolean = false; private set
		var MAX_SEARCH_DISTANCE: Int = -1; private set

		internal fun Log(level: Level, msg: String)
		{
			if (DEBUG) INSTANCE.logger.log(level, msg)
		}
	}

	override fun load()
	{
		INSTANCE = this
	}

	override fun startup()
	{
		saveDefaultConfig()
		DEBUG = config.getBoolean("debug_mode")
		MAX_SEARCH_DISTANCE = config.getInt("max_search_distance")
		MAX_SEARCH_DISTANCE *= MAX_SEARCH_DISTANCE // This is because the search function does not use the sqrt distance function

		CreateTables()

		pluginManager.registerEvents(RoomListeners(), this)

		command("createroom") {
			runs {
				RoomApi.CreateRoom(player)
			}
		}

		Log(Level.INFO, "Starting in DEBUG mode!")
	}

	override fun shutdown()
	{
		DataSource.close()
	}

}