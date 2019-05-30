/*
 * Copyright (C) 2019 Ganom https://github.com/Ganom
 */
package net.runelite.client.plugins.flexobot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentOrientation;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class FlexoBotOverlay extends Overlay
{
	private final Client client;
	private final FlexoBotPlugin plugin;
	private final FlexoBotConfig config;
	private final PanelComponent panelComponent = new PanelComponent();

	@Inject
	private FlexoBotOverlay(Client client, FlexoBotPlugin plugin, FlexoBotConfig config)
	{
		this.plugin = plugin;
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.BOTTOM_RIGHT);
		panelComponent.setOrientation(ComponentOrientation.VERTICAL);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		panelComponent.getChildren().clear();
		if (plugin.isActive())
		{
			panelComponent.getChildren().clear();
			String overlayTitle = "ToB Cheats by Ganom";

			panelComponent.getChildren().add(TitleComponent.builder()
				.text(overlayTitle)
				.color(Color.GREEN)
				.build());

			panelComponent.setPreferredSize(new Dimension(
				graphics.getFontMetrics().stringWidth(overlayTitle) + 25, 5));

			if (plugin.isTickeat())
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Tick Eat")
					.right("Active")
					.rightColor(Color.GREEN)
					.build());
			}
			else
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Tick Eat")
					.right("Disabled")
					.rightColor(Color.RED)
					.build());
			}

			return panelComponent.render(graphics);
		}
		else
		{
			return null;
		}
	}

}
