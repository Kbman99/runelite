package net.runelite.client.plugins.flexobot;

import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;

public abstract class FlexoBotBot
{
	@Getter(AccessLevel.PROTECTED)
	protected final FlexoBotConfig config;

	@Getter(AccessLevel.PROTECTED)
	protected final FlexoBotPlugin plugin;

	@Inject
	protected FlexoBotBot(FlexoBotConfig config, FlexoBotPlugin plugin)
	{
		this.config = config;
		this.plugin = plugin;
	}
}
