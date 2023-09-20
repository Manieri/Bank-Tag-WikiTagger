package com.wikitagger;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("wikitagger")
public interface WikiTaggerConfig extends Config
{
	@ConfigItem(
			keyName = "dropsChatCommand",
			name = "Drops chat command",
			description = "The chat command to make a tab from the drops of a monster"
	)
	default String dropsChatCommand()
	{
		return "drops";
	}
}
