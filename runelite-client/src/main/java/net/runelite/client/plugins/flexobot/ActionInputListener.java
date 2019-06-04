//package net.runelite.client.plugins.flexobot;
//
//import java.awt.event.KeyEvent;
//import java.awt.event.MouseEvent;
//import javax.inject.Inject;
//import net.runelite.api.Client;
//import net.runelite.client.input.KeyListener;
//
//public class ActionInputListener implements KeyListener
//{
//	private final Client client;
//	private final FlexoBotPlugin plugin;
//	private boolean recording = false;
//	private long lastActionTime = 0;
//
//	@Inject
//	private ActionInputListener(Client client, FlexoBotPlugin plugin)
//	{
//		this.client = client;
//		this.plugin = plugin;
//	}
//
//	public MouseEvent addAction(MouseEvent e)
//	{
//		//TODO instead of just the x and y we need to be able to map the clicks to in game objects and tiles
//		if (recording)
//		{
//			plugin.actionsSet.add(new ActionRecorder(e.getX(), e.getY(), e.getButton(), -1, lastActionTime == 0 ? 0 : System.currentTimeMillis() - lastActionTime));
//			lastActionTime = System.currentTimeMillis();
//		}
//		//System.out.println(e.getPoint());
//
//		return e;
//	}
//
//	@Override
//	public void keyTyped(KeyEvent e)
//	{
//
//	}
//
//	@Override
//	public void keyPressed(KeyEvent e)
//	{
//
//	}
//
//	@Override
//	public void keyReleased(KeyEvent e)
//	{
//		System.out.println("Entered keyReleased");
//		if (recording && e.getKeyCode() != KeyEvent.VK_CONTROL)
//		{
//			plugin.actionsSet.add(new ActionRecorder(-1, -1, MouseEvent.NOBUTTON, e.getKeyCode(), lastActionTime == 0 ? 0 : System.currentTimeMillis() - lastActionTime));
//			lastActionTime = System.currentTimeMillis();
//		}
//		//System.out.println(e.getKeyCode());
//		if (e.getKeyCode() == KeyEvent.VK_RIGHT)
//		{
//			plugin.actionsSet.clear();
//			System.out.println("Recording cleared");
//			lastActionTime = 0;
//		}
//
//		if (e.getKeyCode() == KeyEvent.VK_CONTROL)
//		{
//			recording = !recording;
//			if (recording)
//			{
//				System.out.println("Starting recording");
//			}
//			else
//			{
//				System.out.println("Stopping recording");
//			}
//		}
//	}
//}
