package net.runelite.client.plugins.flexobot;

import lombok.Builder;
import lombok.Getter;

@Builder
public class ActionRecorder
{
	@Getter
	private final int xCoord;

	@Getter
	private final int yCoord;

	@Getter
	private final int buttonID;

	@Getter
	private final int keyPressCode;

	@Getter
	private final long delayUntilNextAction;
}
