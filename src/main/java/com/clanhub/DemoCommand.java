package com.clanhub;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.CommandExecuted;

/**
 * The developer-mode command {@code ::hoverinfo demo}: it posts {@link DemoLines}' sample lines into the player's own
 * chatbox as clan and friends chat lines, so the cards can be hovered at once. It does nothing in a Hub build, where
 * developer mode is off. The lines go through {@code Client.addChatMessage} and so reach the plugin's chat reader the
 * way a real line does; nothing is sent to the game's servers.
 */
final class DemoCommand
{
	/** The command's name, typed after {@code ::}. */
	private static final String COMMAND = "hoverinfo";
	/** The command's first argument. */
	private static final String DEMO = "demo";
	/** What the client shows as the channel's name on a clan or friends chat line. */
	private static final String SENDER = "2h Chat Hover Info";

	private DemoCommand()
	{
	}

	/**
	 * Posts the sample lines when {@code developerMode} is on and {@code event} is {@code ::hoverinfo demo} (both words in
	 * any case), and returns how many it posted; otherwise it does nothing and returns 0. Each line is posted with the
	 * five-argument {@code addChatMessage}, its last argument true, so the client posts the {@code ChatMessage} event for
	 * it. The sender is "2h Chat Hover Info" for the clan and friends chat types and empty for every other type.
	 *
	 * <p>Choice: the clan and friends chat types are the eight of those channels, the messages and the chat lines both
	 * (clan, guest clan, group ironman, friends).
	 */
	static int run(Client client, boolean developerMode, CommandExecuted event)
	{
		if (!developerMode || !COMMAND.equalsIgnoreCase(event.getCommand()))
		{
			return 0;
		}
		final String[] arguments = event.getArguments();
		if (arguments == null || arguments.length == 0 || !DEMO.equalsIgnoreCase(arguments[0]))
		{
			return 0;
		}
		int posted = 0;
		for (final DemoLines.Line line : DemoLines.lines())
		{
			client.addChatMessage(line.type, line.name, line.text, senderFor(line.type), true);
			posted++;
		}
		return posted;
	}

	/** "2h Chat Hover Info" for a clan or friends chat type, empty for any other. */
	private static String senderFor(ChatMessageType type)
	{
		switch (type)
		{
			case CLAN_CHAT:
			case CLAN_MESSAGE:
			case CLAN_GUEST_CHAT:
			case CLAN_GUEST_MESSAGE:
			case CLAN_GIM_CHAT:
			case CLAN_GIM_MESSAGE:
			case FRIENDSCHAT:
			case FRIENDSCHATNOTIFICATION:
				return SENDER;
			default:
				return "";
		}
	}
}
