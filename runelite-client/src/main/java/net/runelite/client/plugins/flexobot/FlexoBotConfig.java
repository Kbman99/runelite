package net.runelite.client.plugins.flexobot;

import java.awt.event.KeyEvent;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.ModifierlessKeybind;

@ConfigGroup("flexobot")

public interface FlexoBotConfig extends Config
{
	@ConfigItem(
		position = 0,
		keyName = "startOrStopClickBot",
		name = "Click Bot Hotkey",
		description = "Hotkey to start/stop click bot"
	)
	default Keybind hotkeyUtilClickBot()
	{
		return new ModifierlessKeybind(KeyEvent.VK_UNDEFINED, 0);
	}

	@ConfigItem(
		position = 1,
		keyName = "targetClickBot",
		name = "Target Click Bot",
		description = "The click bot you wish to run"
	)
	default TargetBot.TargetClickBot targetClickBot()
	{
		return TargetBot.TargetClickBot.NONE;
	}

	@ConfigItem(
		position = 2,
		keyName = "startOrStopRecordBot",
		name = "Record Bot Hotkey",
		description = "Hotkey to start/stop record bot"
	)
	default Keybind hotkeyUtilRecordBot()
	{
		return new ModifierlessKeybind(KeyEvent.VK_UNDEFINED, 0);
	}

	@ConfigItem(
		position = 3,
		keyName = "targetRecordBot",
		name = "Target Record Bot",
		description = "The record bot you wish to run"
	)
	default TargetBot.TargetRecordBot targetRecordBot()
	{
		return TargetBot.TargetRecordBot.NONE;
	}

	@ConfigItem(
		position = 4,
		keyName = "items",
		name = "Items To Click",
		description = "Items that will be clicked"
	)
	default String itemsToClick()
	{
		return "";
	}

	@ConfigItem(
		position = 5,
		keyName = "replay",
		name = "Replay Recording",
		description = "Replay previously recorded actions"
	)
	default boolean replayRecording()
	{
		return false;
	}

	@ConfigItem(
		position = 6,
		keyName = "kill",
		name = "Kill Switch",
		description = "Overrides item clicker and will kill the current task"
	)
	default boolean kill()
	{
		return false;
	}

	@ConfigItem(
		position = 7,
		keyName = "blowGlass",
		name = "Blow Glass",
		description = "Bot that will blow glass for you"
	)
	default boolean blowGlass()
	{
		return false;
	}

	@ConfigItem(
		position = 8,
		keyName = "cookKarambwan",
		name = "Cook Karambwan",
		description = "Begin cooking karambwan"
	)
	default boolean cookKarambwan()
	{
		return false;
	}

	@ConfigItem(
		position = 9,
		keyName = "alch",
		name = "Alch",
		description = "Begin alching"
	)
	default boolean alch()
	{
		return false;
	}

	@ConfigItem(
		position = 10,
		keyName = "alchItems",
		name = "Alch Items",
		description = "Items to alch"
	)
	default String alchItems()
	{
		return "";
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
