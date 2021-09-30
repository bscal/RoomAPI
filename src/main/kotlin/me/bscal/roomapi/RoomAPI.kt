package me.bscal.roomapi

import net.axay.kspigot.commands.command
import net.axay.kspigot.commands.runs
import net.axay.kspigot.main.KSpigot
import java.util.logging.Level

class RoomAPI : KSpigot()
{
	companion object
	{
		lateinit var INSTANCE: RoomAPI; private set
		var DEBUG: Boolean = false

		fun Log(level: Level, msg: String)
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

		command("floodfill") {
			runs {
				TestWallConnects(player)
				logger.info("out")
			}
		}
	}

	override fun shutdown()
	{

	}

}