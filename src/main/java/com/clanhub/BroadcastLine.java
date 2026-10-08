package com.clanhub;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.ChatMessageType;

/**
 * Reads the broadcast lines of the clan, guest clan, group ironman and friends chat channels for the one item each
 * names: a drop, a stacked drop, a clue item or a new collection log item. Pure: a type and a message in, a mention
 * out, no RuneLite state. The sentences are the game's own; the patterns follow the ones Chat Item Pricer has used
 * without trouble.
 */
final class BroadcastLine
{
	/** "received a drop: 10 x Black knife (460 coins)": the quantity, the name, the value. */
	private static final Pattern STACKED_DROP = Pattern.compile(
		"received a drop: (\\d+) x (.+?) \\((.+?) coins\\)", Pattern.CASE_INSENSITIVE);
	/** "received a drop: Dragon claws (121,453,000 coins)": the name, the value. */
	private static final Pattern DROP = Pattern.compile(
		"received a drop: (.+?) \\((.+?) coins\\)", Pattern.CASE_INSENSITIVE);
	/** "received a clue item: Ranger boots (25,296,193 coins)": the name, the value. */
	private static final Pattern CLUE = Pattern.compile(
		"received a clue item: (.+?) \\((.+?) coins\\)", Pattern.CASE_INSENSITIVE);
	/** "received a new collection log item: Abyssal whip (1/484)": the name. */
	private static final Pattern COLLECTION_LOG = Pattern.compile(
		"received a new collection log item: (.+?) \\(\\d+/\\d+\\)", Pattern.CASE_INSENSITIVE);

	private BroadcastLine()
	{
	}

	/**
	 * One item named in a broadcast line: the item's name, how many of it, where the name starts and ends in the plain
	 * text (every tag already stripped), and that plain text. Plain data, read by the plugin when it builds the card.
	 */
	static final class ItemMention
	{
		/** The item's name as the line spells it. */
		final String name;
		/** How many of the item: 1 unless the line says "10 x". */
		final int quantity;
		/** Where the name starts in {@link #text}. */
		final int start;
		/** Just past the name's last character in {@link #text}. */
		final int end;
		/** The line without its tags. */
		final String text;

		ItemMention(String name, int quantity, int start, int end, String text)
		{
			this.name = name;
			this.quantity = quantity;
			this.start = start;
			this.end = end;
			this.text = text;
		}
	}

	/**
	 * The item a broadcast line names, or nothing. Only the four channels' broadcast types are read; the tags (colours
	 * and rank icons) are stripped first ({@link ChatText#plain}), and the patterns are tried in the order stacked drop, drop, clue item,
	 * collection log item, so a stacked drop is never read as an item called "10 x Black knife".
	 *
	 * <p>Choice: a quantity too large for an int gives no mention, so no card.
	 */
	static Optional<ItemMention> parse(ChatMessageType type, String message)
	{
		if (message == null || !isBroadcast(type))
		{
			return Optional.empty();
		}
		final String text = ChatText.plain(message);

		Matcher found = STACKED_DROP.matcher(text);
		if (found.find())
		{
			try
			{
				return Optional.of(mention(found, 2, Integer.parseInt(found.group(1)), text));
			}
			catch (NumberFormatException e)
			{
				return Optional.empty();
			}
		}
		found = DROP.matcher(text);
		if (found.find())
		{
			return Optional.of(mention(found, 1, 1, text));
		}
		found = CLUE.matcher(text);
		if (found.find())
		{
			return Optional.of(mention(found, 1, 1, text));
		}
		found = COLLECTION_LOG.matcher(text);
		if (found.find())
		{
			return Optional.of(mention(found, 1, 1, text));
		}
		return Optional.empty();
	}

	/** The mention for the match's name group. */
	private static ItemMention mention(Matcher found, int nameGroup, int quantity, String text)
	{
		return new ItemMention(found.group(nameGroup), quantity, found.start(nameGroup), found.end(nameGroup), text);
	}

	/** The four channels whose broadcast lines carry a card. */
	private static boolean isBroadcast(ChatMessageType type)
	{
		return type == ChatMessageType.CLAN_MESSAGE
			|| type == ChatMessageType.CLAN_GUEST_MESSAGE
			|| type == ChatMessageType.CLAN_GIM_MESSAGE
			|| type == ChatMessageType.FRIENDSCHATNOTIFICATION;
	}
}
