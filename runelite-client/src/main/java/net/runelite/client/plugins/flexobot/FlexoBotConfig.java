package net.runelite.client.plugins.flexobot;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup("flexobot")

public interface FlexoBotConfig extends Config
{
	@ConfigItem(
		position = 0,
		keyName = "startOrStop",
		name = "Start/Stop Hotkey",
		description = "Hotkey to start/stop actions"
	)
	default Keybind hotkeyUtil()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		position = 1,
		keyName = "target",
		name = "Target Bot",
		description = "The bot you wish to run"
	)
	default TargetBot target()
	{
		return TargetBot.CLICK_ITEMS;
	}

	@ConfigItem(
		position = 2,
		keyName = "items",
		name = "Items To Click",
		description = "Items that will be clicked"
	)
	default String itemsToClick()
	{
		return "";
	}

	@ConfigItem(
		position = 3,
		keyName = "kill",
		name = "Kill Switch",
		description = "Overrides item clicker and will kill the current task"
	)
	default boolean kill()
	{
		return false;
	}

	@ConfigItem(
		keyName = "randLow",
		name = "Minimum MS Delay",
		description = "Minimum MS Delay",
		position = 40
	)
	default int randLow()
	{
		return 65;
	}

	@ConfigItem(
		keyName = "randHigh",
		name = "Maximum MS Delay",
		description = "Maximum MS Delay",
		position = 41
	)
	default int randHigh()
	{
		return 70;
	}
}
