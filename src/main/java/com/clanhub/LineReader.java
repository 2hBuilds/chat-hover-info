package com.clanhub;

import java.util.List;
import net.runelite.api.ChatMessageType;

/**
 * Reads one chat line for the cards it holds. The plugin keeps a list of readers and asks each of them about every
 * chat message; a later lot adds a reader of its own and one line to that list.
 */
interface LineReader
{
	/**
	 * The cards the line holds, in the order their words stand in it; an empty list when it holds nothing. {@code message}
	 * is the chat text as the game sends it, tags and all.
	 */
	List<HoverCard> read(ChatMessageType type, String message);
}
