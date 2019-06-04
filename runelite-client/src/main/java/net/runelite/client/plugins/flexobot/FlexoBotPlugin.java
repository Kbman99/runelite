/*
 * Copyright (C) 2019 Kbman99 https://github.com/Kbman99
 */

package net.runelite.client.plugins.flexobot;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.Actor;
import net.runelite.api.GameObject;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemID;
import net.runelite.api.Tile;
import net.runelite.api.VarClientInt;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.InventoryID;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ConfigChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.flexo.Flexo;
import net.runelite.client.flexo.FlexoMouse;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

import com.google.inject.Provides;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
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

	@Getter(AccessLevel.PACKAGE)
	private boolean tickeat;

	@Getter(AccessLevel.PACKAGE)
	private boolean Active;

	@Inject
	private Client client;

	@Inject
	private FlexoBotConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private FlexoBotOverlay overlay;

	@Inject
	private KeyManager keyManager;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private BlowGlassBot blowGlassBot;

	@Inject
	private KarambwanBot karambwanBot;

	@Getter(AccessLevel.PROTECTED)
	private FlexoBotBot[] bots;

	private final Random random = new Random();

	LinkedHashSet<ActionRecorder> actionsSet = new LinkedHashSet<>();

	private HashMap<String, BiConsumer<Integer, Integer[]>> clickBotMap = new HashMap<>();

	private HashMap<String, Runnable> recordBotMap = new HashMap<>();

	private NPC man = null;

	private boolean firstRun = true;

	private boolean running = false;

	private boolean recording = false;

	private long lastActionTime = 0;

	@Getter
	private Flexo flexer;

	@Inject
	private EventBus eventBus;

	private ExecutorService driver = Executors.newFixedThreadPool(1);

	@Provides
	FlexoBotConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FlexoBotConfig.class);
	}

	@Override
	protected void startUp()
	{
		Flexo.client = client;
		overlayManager.add(overlay);
		keyManager.registerKeyListener(clickbotHotkeyListener);
		keyManager.registerKeyListener(recordbotHotkeyListener);
		mouseManager.registerMouseListener(mouseAdapter);

		this.bots = new FlexoBotBot[]{blowGlassBot, karambwanBot};

		for (FlexoBotBot bot : bots)
		{
			eventBus.register(bot);
		}

		clickBotMap.put(TargetBot.TargetClickBot.CLICK_ITEMS.name(), (key, items) -> clickInventoryItemMaybePressKey(key, false, true, items));
		clickBotMap.put(TargetBot.TargetClickBot.DROP_ITEMS.name(), (key, items) -> clickInventoryItemMaybePressKey(key, false, true, items));
		recordBotMap.put(TargetBot.TargetRecordBot.RECORD.name(), () -> executeRecordedActions());

		driver.submit(() ->
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

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		keyManager.unregisterKeyListener(clickbotHotkeyListener);
		keyManager.unregisterKeyListener(recordbotHotkeyListener);
		mouseManager.unregisterMouseListener(mouseAdapter);

		for (FlexoBotBot bot : bots)
		{
			eventBus.unregister(bot);
		}

		man = null;
	}

	private final HotkeyListener clickbotHotkeyListener = new HotkeyListener(() -> config.hotkeyUtilClickBot())
	{
		@Override
		public void hotkeyPressed()
		{
			logger.debug("Hotkey Pressed! running: {}", running);
			if (!driver.isTerminated() && !firstRun && running)
			{
				driver.shutdownNow();
				running = false;
			}
			else
			{
				if (!firstRun)
				{
					driver = Executors.newFixedThreadPool(1);
				}
				firstRun = false;
				int[] items = Arrays.stream(config.itemsToClick().substring(1, config.itemsToClick().length() - 1)
					.split(",")).map(String::trim).mapToInt(Integer::parseInt).toArray();
				int keyTemp = KeyEvent.VK_UNDEFINED;
				if (config.targetClickBot() == TargetBot.TargetClickBot.DROP_ITEMS)
				{
					keyTemp = KeyEvent.VK_SHIFT;
				}
				running = true;
				driver.submit(() -> clickBotMap.get(config.targetClickBot().name()).accept(KeyEvent.VK_UNDEFINED, ArrayUtils.toObject(items)));
			}
		}
	};

	private final HotkeyListener recordbotHotkeyListener = new HotkeyListener(() -> config.hotkeyUtilRecordBot())
	{
		@Override
		public void keyPressed(KeyEvent e)
		{
//			logger.debug("Recordbot hotkey {} pressed", config.hotkeyUtilRecordBot());
			logger.debug("KeyEvent key pressed is {}", e.getKeyCode());

			if (recording && config.hotkeyUtilRecordBot().matches(e))
			{
				actionsSet.add(new ActionRecorder(-1, -1, MouseEvent.NOBUTTON, e.getKeyCode(), lastActionTime == 0 ? 0 : System.currentTimeMillis() - lastActionTime));
				lastActionTime = System.currentTimeMillis();
			}

			if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
			{
				getAllChildren();
				actionsSet.clear();
				logger.debug("Recording cleared");
				lastActionTime = 0;
			}

			if (config.hotkeyUtilRecordBot().matches(e))
			{
				recording = !recording;
				if (recording)
				{
					logger.debug("Starting recording");
				}
				else
				{
					logger.debug("Stopping recording");
				}
			}
		}
	};

	private final MouseAdapter mouseAdapter = new MouseAdapter()
	{
		@Override
		public MouseEvent mousePressed(MouseEvent e)
		{
			if (recording)
			{
				actionsSet.add(new ActionRecorder(e.getX(), e.getY(), e.getButton(), -1, lastActionTime == 0 ? 0 : System.currentTimeMillis() - lastActionTime));
				lastActionTime = System.currentTimeMillis();
			}
			return e;
		}
	};

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		logger.debug("Config changed key: {} -> {}", event.getKey(), config.replayRecording());
		if (event.getKey().equals("replay"))
		{
			if (config.replayRecording() && config.targetRecordBot() != TargetBot.TargetRecordBot.NONE)
			{
				logger.debug("Starting execution...");
				recordBotMap.get(config.targetRecordBot().name()).run();
			}
		}
		else if (event.getKey().equals("blowGlass"))
		{
			if (config.blowGlass())
			{
				logger.debug("Attempting to blow glass...");
				driver.submit(() -> blowGlassBot.run());
			}
		}
		else if (event.getKey().equals("cookKarambwan"))
		{
			if (config.cookKarambwan())
			{
				logger.debug("Attempting to bank...");
				driver.submit(() -> karambwanBot.run());
			}
		}
		else if (event.getKey().equals("alch"))
		{
			if (config.alch())
			{
				logger.debug("Beginning to alch");
				driver.submit(() -> alch());
			}
		}
	}

	@Subscribe
	public void onBeforeRender(BeforeRender r)
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

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		final String msg = event.getMessage();
	}

	public List<WidgetItem> getInventoryItemsFromIDs(boolean unique, int... itemIds)
	{
		Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
		ArrayList<Integer> itemIDs = new ArrayList<>();
		for (int i : itemIds)
		{
			itemIDs.add(i);
		}

		List<WidgetItem> widgetItemList = new ArrayList<>();
		for (WidgetItem item : inventoryWidget.getWidgetItems())
		{
			int id = item.getId();
			if (itemIDs.contains(id))
			{
				widgetItemList.add(item);
				if (unique)
				{
					itemIDs.remove(item.getId());
					logger.debug("made it here");
					if (itemIDs.isEmpty())
					{
						break;
					}
				}
			}
		}

		return widgetItemList;
	}

	public List<Item> getEquippedItems()
	{
		Item[] equippedItems = client.getItemContainer(InventoryID.EQUIPMENT).getItems();

		return Arrays.asList(equippedItems);
	}

	public List<Item> getInventoryItems()
	{
		Item[] inventoryItems = client.getItemContainer(InventoryID.INVENTORY).getItems();
		return Arrays.asList(inventoryItems);
	}

//	public boolean getUniqueInventoryItems(List<Integer> items)
//	{
//		List<Item> inventoryItems = getInventoryItems();
//
//	}

	private void updateInterfaces()
	{
		driver.submit(() ->
		{
			flexer.keyPressAndRelease(client.getCanvas(), KeyEvent.VK_F1);

			flexer.keyPressAndRelease(client.getCanvas(), KeyEvent.VK_F2);

			flexer.keyPressAndRelease(client.getCanvas(), KeyEvent.VK_F3);

			flexer.keyPressAndRelease(client.getCanvas(), KeyEvent.VK_F4);

			flexer.keyPressAndRelease(client.getCanvas(), KeyEvent.VK_F5);
		});
	}

	public void centerMouse()
	{
		Rectangle playerBounds = FlexoMouse.getClickArea(client.getLocalPlayer().getConvexHull().getBounds());

		Point clickPoint = getClickPoint(playerBounds);
		driver.submit(() ->
		{
			if (playerBounds.getX() >= 1)
			{
				flexer.mouseMove(clickPoint.x, clickPoint.y);
			}
		});
	}

	public void clickObject(GameObject targetObject)
	{
		Rectangle bounds = targetObject.getConvexHull().getBounds();
		Point clickPoint = getObjectClickPoint(bounds);

		// Generate more click points until know the point lies within the target object
		while (!bounds.contains(clickPoint))
		{
			clickPoint = getObjectClickPoint(bounds);
		}

		flexer.mouseMove(clickPoint.x, clickPoint.y);
		flexer.mouseClickAndRelease(client.getCanvas(), 1);
		pause(50, 150);
	}

	public void clickPlayer(Actor actor)
	{
		driver.submit(() ->
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

	public void autoClick()
	{

	}

	public void clickInventoryItemMaybePressKey(int keyToPress, boolean unique, boolean loop, Integer... items)
	{
		//flexer.virtualKeyPress(client.getCanvas(), keyToPress);
//		flexer.keyPressAndRelease(client.getCanvas(), KeyEvent.VK_6);
		List<Integer> values = new ArrayList<>();
		for (int val : items)
		{
			values.add(val);
		}
		do
		{
			for (WidgetItem item : getItemsFromInventory(values, unique))
			{
				if (getCurrentTab() != 3)
				{
					flexer.keyPressAndRelease(client.getCanvas(), KeyEvent.VK_F1);
				}
				clickWidgetItem(item);
			}
			if (!hasRequiredItems(Arrays.asList(items)) || Thread.currentThread().isInterrupted() || config.kill() || !running)
			{
				break;
			}
		} while (loop);
//		pause(600, 1200);
//		//pressAndReleaseKey(KeyEvent.VK_6);
//		flexer.keyPress(KeyEvent.VK_6);
//		flexer.keyRelease(KeyEvent.VK_6);
		running = false;
	}

	public void clickLastInvetoryItem(int item)
	{
		List<Integer> values = Arrays.asList(item);
		List<WidgetItem> items = getItemsFromInventory(values, false);

		if (getCurrentTab() != 3)
		{
			flexer.keyPressAndRelease(client.getCanvas(), KeyEvent.VK_F1);
		}
		clickWidgetItem(items.get(items.size() - 1));
	}

	public void clickWidget(Widget itemToClick)
	{
		Rectangle bounds = FlexoMouse.getClickArea(itemToClick.getBounds());
		Point cp = getClickPoint(bounds);
		if (bounds.getX() >= 1)
		{
			flexer.mouseMove(cp.x, cp.y);
			flexer.mouseClickAndRelease(client.getCanvas(), 1);
		}
	}

	public void clickWidgetItem(WidgetItem itemToClick)
	{
		Rectangle bounds = FlexoMouse.getClickArea(itemToClick.getCanvasBounds());
		Point cp = getClickPoint(bounds);
		if (bounds.getX() >= 1)
		{
			flexer.mouseMove(cp.x, cp.y);
			flexer.mouseClickAndRelease(client.getCanvas(), 1);
		}
	}

	public void executeRecordedActions()
	{
		driver.submit(() ->
		{
			logger.debug("Executing replay!");
			for (ActionRecorder action : actionsSet)
			{
				logger.debug("executed...");
				flexer.delay(action.getDelayUntilNextAction());

				if (action.getXCoord() > -1 && action.getYCoord() > -1
					&& action.getButtonID() > 0 && action.getKeyPressCode() == -1)
				{
					flexer.mouseMove(action.getXCoord(), action.getYCoord());
					flexer.mouseClickAndRelease(client.getCanvas(), action.getButtonID());
				}
				else if (action.getKeyPressCode() > -1)
				{
					flexer.keyPressAndRelease(client.getCanvas(), action.getKeyPressCode());
				}
				if (!config.replayRecording())
				{
					break;
				}
			}
		});

	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		int id = event.getGroupId();
		logger.debug("ID {}", id);
	}


	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
		for (WidgetItem item : inventoryWidget.getWidgetItems())
		{
			if (item.getId() == event.getIdentifier())
			{
				logger.debug(item.getIndex() + " " + item.getCanvasBounds());
			}
		}
	}

	public void dropItems(int key, int... items)
	{

	}

	public void useConsumable(int... food)
	{
		driver.submit(() ->
		{
			Rectangle bounds = new Rectangle(0, 0, 0, 0);
			Point cp = new Point(0, 0);

			flexer.keyPressAndRelease(client.getCanvas(), KeyEvent.VK_F1);

			for (WidgetItem item : getInventoryItemsFromIDs(false, food))
			{
				if (getInventoryItemsFromIDs(false, food).isEmpty())
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

	public void useSpec()
	{
		driver.submit(() ->
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

	public void castSpell(WidgetInfo spellToCast)
	{
		driver.submit(() ->
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

	public void alch()
	{
		int timeToCast = 3000;
		int itemToAlch = Integer.parseInt(config.alchItems());
		flexer.keyPressAndRelease(client.getCanvas(), KeyEvent.VK_F3);

		Widget spell = client.getWidget(WidgetInfo.SPELL_HIGH_LEVEL_ALCHEMY);

		Rectangle spellBounds = FlexoMouse.getClickArea(spell.getBounds());
		Point clickPoint = getClickPoint(spellBounds);

		if (spellBounds.getX() >= 1)
		{
			flexer.mouseMove(clickPoint.x, clickPoint.y);
			boolean hasSufficientItems = true;
			while (config.alch() && hasSufficientItems)
			{
				flexer.mouseClickAndRelease(client.getCanvas(), 1);
				pause(50, 60);
				hasSufficientItems = hasRequiredItems(Arrays.asList(ItemID.NATURE_RUNE, itemToAlch));
				long start = System.currentTimeMillis();
				flexer.mouseClickAndRelease(client.getCanvas(), 1);
				long spellCasted = System.currentTimeMillis();
				while (System.currentTimeMillis() - spellCasted < timeToCast + (spellCasted - start) + 15)
				{
					pause(10, 20);
				}
			}
		}
		logger.debug("Alching finished...");
	}

	public void pressAndReleaseKey(int keyToPress)
	{
		flexer.keyPressAndRelease(client.getCanvas(), keyToPress);
	}

	public Point getClickPoint(Rectangle rect)
	{
		double rand = Math.random();
		//TODO these x and y coords will always be random in the positive direction since the rand is being added.
		// Need to modify to prevent misclicks
		int x = (int) (rect.getX() + (rand * rect.getWidth()) * 0.75);
		int y = (int) (rect.getY() + (rand * rect.getHeight()) * 0.75);

		if (client.isStretchedEnabled())
		{
			double scale = 1 + ((double) 75 / 100);
			return new Point((int) (x * scale), (int) (y * scale));
		}
		else
		{
			return new Point(x, y);
		}
	}

	public Point getCentralClickPoint(Rectangle rect)
	{
		int x = (int) (rect.getX() + (0.5 * rect.getWidth()));
		int y = (int) (rect.getY() + (0.5 * rect.getHeight()));

		if (client.isStretchedEnabled())
		{
			double scale = 1 + ((double) 75 / 100);
			return new Point((int) (x * scale), (int) (y * scale));
		}
		else
		{
			return new Point(x, y);
		}
	}

	public Point getObjectClickPoint(Rectangle rect)
	{
		double rand = Math.random();
		//TODO these x and y coords will always be random in the positive direction since the rand is being added.
		// Need to modify to prevent misclicks
		int x = (int) (rect.getX() + ((rand * ((0.8 - 0.2) + 0.01) + 0.2) * rect.getWidth()));
		int y = (int) (rect.getY() + ((rand * ((0.8 - 0.2) + 0.01) + 0.2) * rect.getHeight()));

		if (client.isStretchedEnabled())
		{
			double scale = 1 + ((double) 75 / 100);
			return new Point((int) (x * scale), (int) (y * scale));
		}
		else
		{
			return new Point(x, y);
		}
	}

	public int getCurrentTab()
	{
		return client.getVar(VarClientInt.PLAYER_INVENTORY_OPENED);
	}

	public void pause(int min, int max)
	{
		try
		{
			Thread.sleep(random.nextInt(max - min) + max);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void depositToBank(int... targetItems)
	{
		for (int i : targetItems)
		{
			clickInventoryItemMaybePressKey(KeyEvent.VK_UNDEFINED, true, false, i);
		}
	}

	public void withdrawFromBank(int targetItemId)
	{
		Widget bank = client.getWidget(WidgetInfo.BANK_CONTAINER);

		if (bank == null)
		{
			logger.debug("Bank not open!");
			return;
		}

		List<Widget> items = getItemsFromBank(Arrays.asList(targetItemId), true);

		if (items.isEmpty())
		{
			logger.debug("Could not find items in bank");
			return;
		}

		for (Widget item : items)
		{
			clickWidget(item);
			pause(50, 300);
			logger.debug("Clicked item: {}", item.getId());
		}

		flexer.keyPressAndRelease(client.getCanvas(), KeyEvent.VK_ESCAPE);
	}

	public void openBank(int bankId)
	{
		while (true)
		{
			clickGameObject(bankId);
			pause(100, 200);
			if (bankIsOpen())
			{
				return;
			}
		}
	}

	public void clickGameObject(int id)
	{
		GameObject obj = findLocalGameObject(id);
		if (obj == null)
		{
			return;
		}
		clickObject(obj);
	}

	public List<Widget> getItemsFromBank(List<Integer> targetItemIds, boolean unique)
	{
		Widget container = client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER);
		List<Widget> listOfItems = new ArrayList<>();
		Widget[] items = container.getChildren();

		for (Widget item : items)
		{
			if (targetItemIds.contains(item.getItemId()))
			{
				listOfItems.add(item);
			}
			if (unique && targetItemIds.size() == listOfItems.size())
			{
				break;
			}
		}
		return listOfItems;
	}

	public List<WidgetItem> getItemsFromInventory(List<Integer> targetItemIds, boolean unique)
	{
		Widget container = client.getWidget(WidgetInfo.INVENTORY);
		List<WidgetItem> listOfItems = new ArrayList<>();
		Collection<WidgetItem> items = container.getWidgetItems();

		for (WidgetItem item : items)
		{
			if (targetItemIds.contains(item.getId()))
			{
				listOfItems.add(item);
			}
			if (unique && targetItemIds.size() == listOfItems.size())
			{
				break;
			}
		}
		return listOfItems;
	}

	public boolean hasRequiredItems(List<Integer> items)
	{
		List<Integer> itemsWeWant = new ArrayList<>();
		List<WidgetItem> itemsWeHave = getItemsFromInventory(items, true);

		for (WidgetItem i : itemsWeHave)
		{
			if (items.contains(i.getId()))
			{
				itemsWeWant.add(i.getId());
			}
		}

		return items.size() == itemsWeWant.size();
	}

	public GameObject findLocalGameObject(int targetObjectId)
	{
		LocalPoint localLocation = getLocalLocation();
		Tile[][][] tiles = client.getScene().getTiles();
		for (int i = -2; i < 2; i++)
		{
			for (int j = -2; j < 2; j++)
			{
				Tile currentTile = tiles[client.getPlane()][localLocation.getSceneX() + i][localLocation.getSceneY() + j];
				for (GameObject obj : currentTile.getGameObjects())
				{
					if (obj == null)
					{
						continue;
					}
					if (obj.getId() == targetObjectId)
					{
						return obj;
					}
				}
			}
		}
		return null;
	}

	public LocalPoint getLocalLocation()
	{
		WorldPoint playerLocalLocation = client.getLocalPlayer().getWorldLocation();
		return LocalPoint.fromWorld(client, playerLocalLocation);
	}

	public List<GameObject> getLocalGameObjects(int radius)
	{
		List<GameObject> objects = new ArrayList<>();

		LocalPoint localLocation = getLocalLocation();
		Tile[][][] tiles = client.getScene().getTiles();
		for (int i = -radius; i < radius; i++)
		{
			for (int j = -radius; j < radius; j++)
			{
				Tile currentTile = tiles[client.getPlane()][localLocation.getSceneX() + i][localLocation.getSceneY() + j];
				for (GameObject obj : currentTile.getGameObjects())
				{
					if (obj == null)
					{
						continue;
					}
					objects.add(obj);
				}
			}
		}
		return objects;
	}

	public List<Integer> getLocalGameObjectIds(int radius)
	{
		List<Integer> gameObjectIds = new ArrayList<>();
		List<GameObject> gameObjects = getLocalGameObjects(radius);

		for (GameObject obj : gameObjects)
		{
			gameObjectIds.add(obj.getId());
		}
		return gameObjectIds;
	}

	public Set<GameObject> getUniqueLocalGameObjects(int radius)
	{
		return new HashSet<>(getLocalGameObjects(radius));
	}

	public Set<Integer> getUniqueLocalGameObjectIds(int radius)
	{
		Set<Integer> gameObjectIds = new HashSet<>();
		Set<GameObject> gameObjects = getUniqueLocalGameObjects(radius);

		for (GameObject obj : gameObjects)
		{
			gameObjectIds.add(obj.getId());
		}
		return gameObjectIds;
	}

	public void getAllChildren()
	{
		Widget chat = client.getWidget(WidgetInfo.CHATBOX_MESSAGES);
		for (Widget w : chat.getStaticChildren())
		{
			logger.debug("Item ID is {} and Id is {}", w.getItemId(), w.getId());
		}
	}

	public boolean interactableChatMenuVisible()
	{
		Widget chatbox = client.getWidget(WidgetInfo.CHATBOX_MESSAGES);
		if (chatbox == null)
		{
			return false;
		}
		for (Widget child : chatbox.getStaticChildren())
		{
			if (child != null)
			{
				if (child.getId() == 10617393)
				{
					return true;
				}
			}
		}
		return false;
	}

	public boolean bankIsOpen()
	{
		Widget bank = client.getWidget(WidgetInfo.BANK_CONTAINER);
		logger.debug("bank hidden: {}", bank == null);
		return !(bank == null);
	}

	public static <T> Set<T> convertListToSet(List<T> list)
	{
		// create an empty set
		Set<T> set = new HashSet<>(list);

		return set;
	}
}
