package me.bscal.roomapi

import net.axay.kspigot.commands.command
import net.axay.kspigot.commands.runs
import net.axay.kspigot.extensions.pluginManager
import net.axay.kspigot.main.KSpigot
import java.util.logging.Level

class RoomApiPlugin : KSpigot()
{
	companion object
	{
		lateinit var INSTANCE: RoomApiPlugin; private set
		var DEBUG: Boolean = false; private set

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

		CreateTables()

		pluginManager.registerEvents(RoomListeners(), this)

		command("createroom") {
			runs {
				RoomApi.CreateRoom(player)
			}
		}
	}

	override fun shutdown()
	{
		DataSource.close()
	}

}