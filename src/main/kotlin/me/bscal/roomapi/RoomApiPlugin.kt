package me.bscal.roomapi

import me.bscal.roomapi.RoomBlocks.LoadExcludedBlocksFromConfig
import net.axay.kspigot.commands.command
import net.axay.kspigot.commands.runs
import net.axay.kspigot.extensions.pluginManager
import net.axay.kspigot.main.KSpigot
import java.util.*
import java.util.logging.Level

class RoomApiPlugin : KSpigot()
{
	companion object
	{
		lateinit var INSTANCE: RoomApiPlugin; private set
		lateinit var DEBUG_MODE: DebugMode private set
		var MAX_SEARCH_DISTANCE: Int = -1; private set

		internal fun LogDebug(level: Level, msg: String)
		{
			if (DEBUG_MODE == DebugMode.DEBUG) INSTANCE.logger.log(level, msg)
		}

		internal fun Log(level: Level, msg: String)
		{
			if (DEBUG_MODE.Value.and(3) == 0) INSTANCE.logger.log(level, msg)
		}
	}

	override fun load()
	{
		INSTANCE = this
	}

	override fun startup()
	{
		saveDefaultConfig()
		DEBUG_MODE = DebugMode.Match(config.getString("debug_mode"))
		MAX_SEARCH_DISTANCE = config.getInt("max_search_distance")
		MAX_SEARCH_DISTANCE *= MAX_SEARCH_DISTANCE // This is because the search function does not use the sqrt distance function
		LoadExcludedBlocksFromConfig(config)

		CreateTables()

		pluginManager.registerEvents(RoomListeners(), this)

		command("createroom") {
			runs {
				RoomApi.CreateRoom(player)
			}
		}

		LogDebug(Level.INFO, "Starting in DEBUG mode!")
	}

	override fun shutdown()
	{
		DataSource.close()
	}

}

enum class DebugMode(val Value: Int)
{

	DEBUG(1),
	RELEASE(2),
	DIST(4);

	companion object
	{
		fun Match(s: String?): DebugMode
		{
			try
			{
				val str = s ?: "DEBUG"
				return valueOf(str.uppercase(Locale.getDefault()))
			}
			catch (e: Exception)
			{
				System.err.println(e.stackTrace)
			}
			return DEBUG
		}
	}
}
