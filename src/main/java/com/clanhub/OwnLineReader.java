package com.clanhub;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.ChatMessageType;

/**
 * The reader of the player's own game lines about an item: a valuable drop, an untradeable drop and a new collection log
 * item. The sentences are the game's own and are read case-sensitively from the start of the line, once the tags are
 * stripped; {@link ItemCards} makes the card, so a line gives one card, or none when it is another line or names no item
 * the item manager knows.
 */
final class OwnLineReader implements LineReader
{
	/** "Valuable drop: 10 x Black knife (4,600 coins)": the quantity if any, the name, the value. */
	private static final Pattern VALUABLE = Pattern.compile("^Valuable drop: (?:(\\d+) x )?(.+?) \\((.+?) coins?\\)");
	/** "Untradeable drop: Pet snakeling": the quantity if any, the name, the value if any. */
	private static final Pattern UNTRADEABLE = Pattern.compile(
		"^Untradeable drop: (?:(\\d+) x )?(.+?)(?: \\((.+?) coins?\\))?$");
	/** "New item added to your collection log: Abyssal whip": the name. */
	private static final Pattern COLLECTION_LOG = Pattern.compile("^New item added to your collection log: (.+)$");

	private final ItemCards itemCards;

	OwnLineReader(ItemCards itemCards)
	{
		this.itemCards = itemCards;
	}

	/**
	 * The card for the item an own game line names, or none; only {@code GAMEMESSAGE} lines are read. The quantity is 1
	 * unless the line says "10 x".
	 *
	 * <p>Choice: a quantity too large for an int gives no card, as in {@link BroadcastLine}.
	 */
	@Override
	public List<HoverCard> read(ChatMessageType type, String message)
	{
		if (message == null || type != ChatMessageType.GAMEMESSAGE)
		{
			return Collections.emptyList();
		}
		final String plain = ChatText.plain(message);
		Matcher found = VALUABLE.matcher(plain);
		if (!found.find())
		{
			found = UNTRADEABLE.matcher(plain);
			if (!found.find())
			{
				found = COLLECTION_LOG.matcher(plain);
				return found.find() ? one(found, 1, 1, plain) : Collections.<HoverCard>emptyList();
			}
		}
		final String quantity = found.group(1);
		if (quantity == null)
		{
			return one(found, 2, 1, plain);
		}
		try
		{
			return one(found, 2, Integer.parseInt(quantity), plain);
		}
		catch (NumberFormatException e)
		{
			return Collections.emptyList();
		}
	}

	/** The one card for the match's name group, or an empty list when the item manager makes none. */
	private List<HoverCard> one(Matcher found, int nameGroup, int quantity, String plain)
	{
		final HoverCard card = itemCards.forName(found.group(nameGroup), quantity, plain, found.start(nameGroup),
			found.end(nameGroup));
		return card == null ? Collections.<HoverCard>emptyList() : Collections.singletonList(card);
	}
}
