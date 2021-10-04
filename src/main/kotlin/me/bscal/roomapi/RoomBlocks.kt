package me.bscal.roomapi

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration

object RoomBlocks
{

	val ExcludedBlock = ObjectOpenHashSet<Material>();

	fun IsExcluded(material: Material) : Boolean = ExcludedBlock.contains(material)

	fun IsExcludedOrAir(material: Material) : Boolean = material.isAir || ExcludedBlock.contains(material)

	internal fun LoadExcludedBlocksFromConfig(config: FileConfiguration)
	{
		for (entry in config.getStringList("excluded_blocks"))
		{
			val type = Material.matchMaterial(entry) ?: continue
			ExcludedBlock.add(type)
		}
	}

}