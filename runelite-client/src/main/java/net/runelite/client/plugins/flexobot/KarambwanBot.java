package net.runelite.client.plugins.flexobot;

import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;

import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import java.util.Random;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class KarambwanBot extends FlexoBotBot
{
	private static final int minDelay = 50;
	private static final int maxDelay = 500;
	private static final int tick = 600;
	private static final int MAX_VIEW_RANGE = 15;

	private volatile boolean terminate;

	private final Random random = new Random();

	private static Logger logger = LoggerFactory.getLogger(FlexoBotPlugin.class);

	private final Client client;

	@Inject
	private ItemManager itemManager;

	private ExecutorService worker = Executors.newFixedThreadPool(2);

	@Inject
	private KarambwanBot(FlexoBotConfig config, FlexoBotPlugin plugin, Client client, ItemManager itemManager)
	{
		super(config, plugin);
		this.client = client;
		this.itemManager = itemManager;
	}

	public void run()
	{
		Future<?> future;
		Future<?> typeJob = null;
		if (worker.isShutdown())
		{
			worker = Executors.newFixedThreadPool(2);
		}
		while (config.cookKarambwan())
		{
			terminate = false;
			future = worker.submit(() -> plugin.openBank(30087));
			do
			{
				plugin.pause(600, 800);
			} while (config.cookKarambwan() && !future.isDone());
			future = worker.submit(() -> plugin.depositToBank(3148, 3144));
			while (!future.isDone() && config.cookKarambwan())
			{
				plugin.pause(50, 300);
			}
			future = worker.submit(() -> plugin.withdrawFromBank(3142));
			while (!future.isDone() && config.cookKarambwan())
			{
				plugin.pause(100, 400);
			}
			plugin.getFlexer().virtualKeyPress(client.getCanvas(), KeyEvent.VK_2);
			plugin.pause(400, 700);
			typeJob = worker.submit(() -> typeTwo());
			while (!plugin.getItemsFromInventory(Arrays.asList(3142), true).isEmpty() && config.cookKarambwan())
			{
				future = worker.submit(() -> plugin.clickLastInvetoryItem(3142));
				while (!future.isDone() && config.cookKarambwan())
				{
					plugin.pause(5, 15);
				}
				worker.submit(() -> plugin.clickGameObject(31631));
				while (!plugin.interactableChatMenuVisible() && config.cookKarambwan())
				{
					plugin.pause(5, 15);
				}
				plugin.pause(15, 50);
			}
			typeJob.cancel(true);
			terminate = true;
			while (!typeJob.isCancelled() && config.cookKarambwan())
			{
				plugin.pause(10, 50);
			}
			plugin.getFlexer().virtualKeyRelease(client.getCanvas(), KeyEvent.VK_2);
			plugin.pause(200, 300);
		}
		if (typeJob != null)
		{
			typeJob.cancel(true);
		}
		plugin.getFlexer().virtualKeyRelease(client.getCanvas(), KeyEvent.VK_2);
		worker.shutdownNow();
	}

	private void typeTwo()
	{
		while (!Thread.currentThread().isInterrupted() && !terminate && config.cookKarambwan())
		{
			plugin.getFlexer().keyTyped(client.getCanvas(), KeyEvent.VK_2);
			try
			{
				Thread.sleep(10);
			}
			catch (InterruptedException ex)
			{
				System.out.println("Exiting gracefully!");
				Thread.currentThread().interrupt();
				plugin.getFlexer().virtualKeyRelease(client.getCanvas(), KeyEvent.VK_2);
				return;
			}
		}
	}
}
