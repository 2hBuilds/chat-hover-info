package com.clanhub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.ChatMessageType;

/**
 * The reader of the player's own game lines about an item: a valuable drop, an untradeable drop and a new collection log
 * item. The sentences are the game's own and are read case-sensitively from the start of the line, once the tags are
 * stripped; {@link ItemCards} makes the card, so a line gives one card, or none when it is another line or names no item
 * the item manager knows. A valuable drop line that says where the drop came from ("from Duke Sucellus") gives a second
 * card, the boss's, while the "Boss cards" setting is on, the same quiet card the broadcast lines give, with the wiki's
 * own picture of the boss and its combat level, hitpoints, max hit and quest from the {@link CardTables} when the table
 * knows the boss.
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
	/** ") from Duke Sucellus": the boss, monster, raid or chest a valuable drop line names after the value. */
	private static final Pattern SOURCE = Pattern.compile("\\) from (.+?)\\.?$", Pattern.CASE_INSENSITIVE);

	private final ItemCards itemCards;
	private final BooleanSupplier bossCards;
	private final CardTables tables;

	/**
	 * @param itemCards makes the item card
	 * @param bossCards whether the "Boss cards" setting is on, asked for each line
	 * @param tables the bosses' facts, which may not have loaded yet
	 */
	OwnLineReader(ItemCards itemCards, BooleanSupplier bossCards, CardTables tables)
	{
		this.itemCards = itemCards;
		this.bossCards = bossCards;
		this.tables = tables;
	}

	/**
	 * The card for the item an own game line names, or none; only {@code GAMEMESSAGE} lines are read. The quantity is 1
	 * unless the line says "10 x".
	 *
	 * <p>Choice: a quantity too large for an int gives no card, as in {@link BroadcastLine}.
	 *
	 * <p>Choice: only a valuable drop line is read for a source, from its value's closing bracket to the line's end, as in
	 * {@link BroadcastLine}; the boss card follows the item's, and is given even when the item gives none.
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
		final boolean valuable = found.find();
		if (!valuable)
		{
			found = UNTRADEABLE.matcher(plain);
			if (!found.find())
			{
				found = COLLECTION_LOG.matcher(plain);
				return found.find() ? one(found, 1, 1, plain, false) : Collections.<HoverCard>emptyList();
			}
		}
		final String quantity = found.group(1);
		if (quantity == null)
		{
			return one(found, 2, 1, plain, valuable);
		}
		try
		{
			return one(found, 2, Integer.parseInt(quantity), plain, valuable);
		}
		catch (NumberFormatException e)
		{
			return Collections.emptyList();
		}
	}

	/**
	 * The item's card for the match's name group (none when the item manager makes none), then, when {@code withSource} is
	 * true and the line names a source after the match's closing bracket and "Boss cards" is on, the boss's quiet card.
	 */
	private List<HoverCard> one(Matcher found, int nameGroup, int quantity, String plain, boolean withSource)
	{
		final List<HoverCard> cards = new ArrayList<>(2);
		final HoverCard card = itemCards.forName(found.group(nameGroup), quantity, plain, found.start(nameGroup),
			found.end(nameGroup));
		if (card != null)
		{
			cards.add(card);
		}
		if (withSource && bossCards.getAsBoolean())
		{
			final Matcher from = SOURCE.matcher(plain);
			from.region(found.end() - 1, plain.length());
			if (from.find())
			{
				final String raw = from.group(1);
				final String source = raw.trim();
				if (!source.isEmpty())
				{
					final int start = from.start(1) + raw.indexOf(source);
					cards.add(BroadcastItemReader.bossCard(source, plain, start, start + source.length(), tables));
				}
			}
		}
		return cards;
	}
}
