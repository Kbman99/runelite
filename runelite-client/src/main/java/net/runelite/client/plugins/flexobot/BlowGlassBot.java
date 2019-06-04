package net.runelite.client.plugins.flexobot;

import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.game.ItemManager;
import static net.runelite.api.ObjectID.CAMELOT_TELE_BANK_CHEST;
import static net.runelite.api.ObjectID.PORTAL_4525;

import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import java.util.Random;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BlowGlassBot extends FlexoBotBot
{
	private static final int minDelay = 50;
	private static final int maxDelay = 500;
	private static final int tick = 600;
	private static final int MAX_VIEW_RANGE = 15;

	private final Random random = new Random();

	private static Logger logger = LoggerFactory.getLogger(FlexoBotPlugin.class);

	private final Client client;

	@Inject
	private ItemManager itemManager;

	private ExecutorService worker = Executors.newFixedThreadPool(1);

	@Inject
	private BlowGlassBot(FlexoBotConfig config, FlexoBotPlugin plugin, Client client, ItemManager itemManager)
	{
		super(config, plugin);
		this.client = client;
		this.itemManager = itemManager;
	}

	public void run()
	{
		Future <?> future;
		while (config.blowGlass())
		{
			future = worker.submit(() -> plugin.openBank(25808));
			while (!future.isDone() && config.blowGlass())
			{
				plugin.pause(50, 300);
			}
			future = worker.submit(() -> plugin.depositToBank(567));
			while (!future.isDone() && config.blowGlass())
			{
				plugin.pause(50, 300);
			}
			future = worker.submit(() -> plugin.withdrawFromBank(1775));
			while (!future.isDone() && config.blowGlass())
			{
				plugin.pause(100, 400);
			}
			future = worker.submit(() -> blowGlass());
			while (!future.isDone() && config.blowGlass())
			{
				plugin.pause(200, 400);
			}
			future = worker.submit(() -> plugin.getFlexer().keyPressAndRelease(client.getCanvas(), KeyEvent.VK_6));
			while (!future.isDone() && config.blowGlass())
			{
				plugin.pause(200, tick);
			}
			while (!plugin.getItemsFromInventory(Arrays.asList(1775), true).isEmpty() && config.blowGlass())
			{
				plugin.pause(tick, tick * 2);
			}
		}
	}

	public void teleportToLocation(WidgetInfo teleportToCast)
	{
		plugin.castSpell(teleportToCast);
		plugin.pause(minDelay, maxDelay);
		if (teleportToCast == WidgetInfo.SPELL_TELEPORT_TO_HOUSE)
		{

		}
		else if (teleportToCast == WidgetInfo.SPELL_CAMELOT_TELEPORT)
		{

		}
	}

	private boolean getblowGlass()
	{
		ItemContainer blowGlass = client.getItemContainer(InventoryID.BANK);
		return blowGlass != null;
	}

	private boolean isInRegion(int targetRegion)
	{
		return client.getLocalPlayer().getWorldLocation().getRegionID() == targetRegion;
	}

	private boolean atHouseEntrance()
	{
		return plugin.getUniqueLocalGameObjectIds(2).contains(PORTAL_4525);
	}

	private boolean atCamelotTeleblowGlassChest()
	{
		return plugin.getUniqueLocalGameObjectIds(4).contains(CAMELOT_TELE_BANK_CHEST);
	}

	private void blowGlass()
	{
		plugin.clickInventoryItemMaybePressKey(KeyEvent.VK_UNDEFINED, true, false, 1785, 1775);
		while (!plugin.interactableChatMenuVisible())
		{
			plugin.pause(50, 150);
		}
		plugin.pause(200, 400);
	}
}
