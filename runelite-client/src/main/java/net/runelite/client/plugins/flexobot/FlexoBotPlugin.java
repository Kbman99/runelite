/*
 * Copyright (C) 2019 Kbman99 https://github.com/Kbman99
 */

package net.runelite.client.plugins.flexobot;

import com.google.inject.Provides;
import java.util.HashMap;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.Prayer;
import net.runelite.api.Projectile;
import net.runelite.api.Skill;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.flexo.Flexo;
import net.runelite.client.flexo.FlexoMouse;
import net.runelite.client.game.NPCManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

import javax.inject.Inject;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "Flexo Bot",
	description = "Custom bot plugins",
	tags = {
		"clicker", "flexo", "bot"
	},
	enabledByDefault = false,
	type = PluginType.EXTERNAL
)

public class FlexoBotPlugin extends Plugin
{
	private static Logger logger = LoggerFactory.getLogger(FlexoBotPlugin.class);

	@Getter(AccessLevel.PACKAGE) private boolean tickeat;

	@Getter(AccessLevel.PACKAGE) private boolean Active;

	@Getter private Widget widget;

	@Inject private Client client;

	@Inject private ClientThread clientThread;

	@Inject private FlexoBotConfig config;

	@Inject private OverlayManager overlayManager;

	@Inject private FlexoBotOverlay overlay;

	@Inject private KeyManager keyManager;

	private HashMap<String, Consumer<int[]>> botMap = new HashMap<>();

	private NPC man = null;

	private boolean firstRun = true;

	private boolean running = false;

	private Flexo flexer;
	private ExecutorService executorService = Executors.newFixedThreadPool(1);

	@Provides FlexoBotConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FlexoBotConfig.class);
	}

	private final HotkeyListener hotkeyListener = new HotkeyListener(() -> config.hotkeyUtil())
	{
		@Override public void hotkeyPressed()
		{
			if (!executorService.isTerminated() && !firstRun)
			{
				executorService.shutdownNow();
				running = false;
			}
			else
			{
				if (!firstRun)
				{
					executorService = Executors.newFixedThreadPool(1);
				}
				firstRun = false;
				int[] items = Arrays.stream(config.itemsToClick().substring(1, config.itemsToClick().length() - 1)
					.split(",")).map(String::trim).mapToInt(Integer::parseInt).toArray();
				botMap.get(config.target().name()).accept(items);
				running = true;
			}
		}
	};

	@Override protected void startUp()
	{
		overlayManager.add(overlay);
		keyManager.registerKeyListener(hotkeyListener);
		Flexo.client = client;
		botMap.put(TargetBot.CLICK_ITEMS.name(), items -> clickItem(items));
		executorService.submit(() ->
		{
			flexer = null;
			try
			{
				flexer = new Flexo();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		});

		man = null;
	}

	@Override protected void shutDown()
	{
		overlayManager.remove(overlay);
		keyManager.unregisterKeyListener(hotkeyListener);

		man = null;
	}

	@Subscribe public void onBeforeRender(BeforeRender r)
	{
		for (NPC npc : client.getNpcs())
		{
			if (npc.getId() == NpcID.MAN_3080)
			{
				man = npc;
				break;
			}
		}
	}

	@Subscribe public void onChatMessage(ChatMessage event)
	{
		final String msg = event.getMessage();
//		if (event.getType() == ChatMessageType.PUBLICCHAT)
//		{
//			if (msg.toLowerCase().contains("lol"))
//			{
//
//			}
//		}
	}

	private List<WidgetItem> getItems(int... itemIds)
	{
		Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
		ArrayList<Integer> itemIDs = new ArrayList<>();
		for (int i : itemIds)
		{
			itemIDs.add(i);
		}

		List<WidgetItem> listToReturn = new ArrayList<>();

		for (WidgetItem item : inventoryWidget.getWidgetItems())
		{
			if (itemIDs.contains(item.getId()))
			{
				listToReturn.add(item);
			}
		}

		return listToReturn;
	}

	private void updateInterfaces()
	{
		executorService.submit(() ->
		{
			flexer.keyPressAndRelease(client.getCanvas(), KeyEvent.VK_F1);

			flexer.keyPressAndRelease(client.getCanvas(), KeyEvent.VK_F2);

			flexer.keyPressAndRelease(client.getCanvas(), KeyEvent.VK_F3);

			flexer.keyPressAndRelease(client.getCanvas(), KeyEvent.VK_F4);

			flexer.keyPressAndRelease(client.getCanvas(), KeyEvent.VK_F5);
		});
	}

	private void centerMouse()
	{
		Rectangle playerBounds = FlexoMouse.getClickArea(client.getLocalPlayer().getConvexHull().getBounds());

		Point clickPoint = getClickPoint(playerBounds);
		executorService.submit(() ->
		{
			if (playerBounds.getX() >= 1)
			{
				flexer.mouseMove(clickPoint.x, clickPoint.y);
			}
		});
	}

	private void clickPlayer(Actor actor)
	{
		executorService.submit(() ->
		{

			Rectangle playerBounds = new Rectangle(0, 0, 0, 0);

			if (actor == null)
			{
				return;
			}
			//Do this again so that we can get the updated location of the actor right before we move the mouse
			//TODO change to actor and client.getPlayers() when ready to move on with testing
			for (NPC npc : client.getNpcs())
			{
				if (npc == actor)
				{
					playerBounds = FlexoMouse.getClickArea(npc.getConvexHull().getBounds());
					break;
				}
			}

			Point clickPoint = getClickPoint(playerBounds);

			if (playerBounds.getX() >= 1)
			{
				flexer.mouseMove(clickPoint.x, clickPoint.y);
				flexer.mouseClickAndRelease(client.getCanvas(), 1);
			}
		});
	}

	private void clickItem(int... items)
	{
		executorService.submit(new Runnable()
		{
			@Override public void run()
			{
				while (true)
				{
					for (WidgetItem item : getItems(items))
					{
						Rectangle bounds = FlexoMouse.getClickArea(item.getCanvasBounds());
						Point cp = getClickPoint(bounds);
						if (bounds.getX() >= 1)
						{
							flexer.mouseMove(cp.x, cp.y);
							flexer.mouseClickAndRelease(client.getCanvas(), 1);
						}
					}
					if (getItems(items).size() != items.length || Thread.currentThread().isInterrupted() || config.kill() || !running)
					{
						break;
					}
				}
			}
		});
	}

	private void useConsumable(int... food)
	{
		executorService.submit(() ->
		{
			Rectangle bounds = new Rectangle(0, 0, 0, 0);
			Point cp = new Point(0, 0);

			flexer.keyPressAndRelease(client.getCanvas(), KeyEvent.VK_F1);

			for (WidgetItem item : getItems(food))
			{
				if (getItems(food).isEmpty())
				{
					break;
				}
				bounds = FlexoMouse.getClickArea(item.getCanvasBounds());
				cp = getClickPoint(bounds);
			}

			if (bounds.getX() >= 1)
			{
				flexer.mouseMove(cp.x, cp.y);
				flexer.mouseClickAndRelease(client.getCanvas(), 1);
			}
		});
	}

	private void useSpec()
	{
		executorService.submit(() ->
		{
			flexer.keyPressAndRelease(client.getCanvas(), KeyEvent.VK_F5);

			Widget spec = client.getWidget(WidgetInfo.COMBAT_SPECIAL_ATTACK);

			Rectangle specBounds = FlexoMouse.getClickArea(spec.getBounds());
			Point clickPoint = getClickPoint(specBounds);
			if (specBounds.getX() >= 1)
			{
				flexer.mouseMove(clickPoint.x, clickPoint.y);
				flexer.mouseClickAndRelease(client.getCanvas(), 1);
			}
		});
	}

	private void castSpell(WidgetInfo spellToCast)
	{
		executorService.submit(() ->
		{
			flexer.keyPressAndRelease(client.getCanvas(), KeyEvent.VK_F4);

			Widget spell = client.getWidget(spellToCast);

			Rectangle spellBounds = FlexoMouse.getClickArea(spell.getBounds());
			Point clickPoint = getClickPoint(spellBounds);
			if (spellBounds.getX() >= 1)
			{
				flexer.mouseMove(clickPoint.x, clickPoint.y);
				flexer.mouseClickAndRelease(client.getCanvas(), 1);
			}
		});
	}

	private Point getClickPoint(Rectangle rect)
	{
		if (client.isStretchedEnabled())
		{
			double constant = (Math.random() <= 0.5) ? -2 : 2;
			double rand = Math.random() * constant;
			//TODO these x and y coords will always be random in the positive direction since the rand is being added.
			// Need to modify to prevent misclicks
			int x = (int) (rect.getX() + (rand * 2) + rect.getWidth() / 2);
			int y = (int) (rect.getY() + (rand * 2) + rect.getHeight() / 2);
			double scale = 1 + ((double) 75 / 100);
			return new Point((int) (x * scale), (int) (y * scale));
		}
		else
		{
			double constant = (Math.random() <= 0.5) ? -2 : 2;
			double rand = Math.random() * constant;
			//TODO these x and y coords will always be random in the positive direction since the rand is being added.
			// Need to modify to prevent misclicks
			int x = (int) (rect.getX() + (rand * 2) + rect.getWidth() / 2);
			int y = (int) (rect.getY() + (rand * 2) + rect.getHeight() / 2);
			return new Point(x, y);
		}
	}
}
